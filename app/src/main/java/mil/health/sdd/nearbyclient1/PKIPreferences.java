package mil.health.sdd.nearbyclient1;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;

import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.Security;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
//TODO encrypt private key
public class PKIPreferences {
    private Context context;
    private SharedPreferences sharedPreferences;
    private String pkiPrivateKeyPref;
    private String pkiPublicKeyPref;
    private String pkiCSRPref;
    private String pkiX509CertPref;
    private String pkiCaCertPref;
    public byte[] pkiPrivateKeyBytes;
    public byte[] pkiPublicKeyBytes;
    public byte[] pkiCSRBytes;
    public byte[] pkiX509CertBytes;
    public byte[] pkiCaCertBytes;
    public static String TAG = "PKIPreferences";
    private static final int BASE64_CONF = Base64.NO_WRAP;
    public PKIPreferences(Context context,String share_prefs_filename){
        this.context = context;
        sharedPreferences = this.context.getSharedPreferences(share_prefs_filename, Context.MODE_PRIVATE);

        Provider bcProvider = new BouncyCastleProvider();
        Security.addProvider(bcProvider);

        this.retrieveRawCerts();
    }

    private void retrieveRawCerts(){
        pkiPrivateKeyPref = sharedPreferences.getString(context.getString(R.string.pki_private_key_name),"");
        pkiPublicKeyPref = sharedPreferences.getString(context.getString(R.string.pki_public_key_name),"");
        pkiCSRPref = sharedPreferences.getString(context.getString(R.string.pki_csr_key_name),"");
        pkiX509CertPref = sharedPreferences.getString(context.getString(R.string.pki_x509_signed_cert_name),"");
        pkiCaCertPref = sharedPreferences.getString(context.getString(R.string.pki_ca_cert_name),"");
        if(isSetup()){
            this.decodeCerts();
        }
    }

    public boolean isSetup(){
        return !(pkiPrivateKeyPref.isEmpty() || pkiPublicKeyPref.isEmpty() || pkiCSRPref.isEmpty());
    }

    public void store(KeyPair caKeyPair,PKCS10CertificationRequest csr) throws  IOException {
        SharedPreferences.Editor pkiEditor = sharedPreferences.edit();
        pkiEditor.putString(context.getString(R.string.pki_public_key_name), Base64.encodeToString(caKeyPair.getPublic().getEncoded(), BASE64_CONF));
        pkiEditor.putString(context.getString(R.string.pki_private_key_name), Base64.encodeToString(caKeyPair.getPrivate().getEncoded(), BASE64_CONF));
        pkiEditor.putString(context.getString(R.string.pki_csr_key_name), Base64.encodeToString(csr.getEncoded(), BASE64_CONF));
        pkiEditor.commit();
        this.retrieveRawCerts();
    }

    public void store(X509Certificate signedCert) throws CertificateEncodingException {
        SharedPreferences.Editor pkiEditor = sharedPreferences.edit();
        pkiEditor.putString(context.getString(R.string.pki_x509_signed_cert_name), Base64.encodeToString(signedCert.getEncoded(), BASE64_CONF));
        pkiEditor.commit();
        this.retrieveRawCerts();
    }

    public void storeCa(X509Certificate signedCert) throws CertificateEncodingException {
        SharedPreferences.Editor pkiEditor = sharedPreferences.edit();
        pkiEditor.putString(context.getString(R.string.pki_ca_cert_name), Base64.encodeToString(signedCert.getEncoded(), BASE64_CONF));
        pkiEditor.commit();
        this.retrieveRawCerts();
    }

    public void deleteAll(){
        SharedPreferences.Editor pkiEditor = sharedPreferences.edit();
        pkiEditor.putString(context.getString(R.string.pki_public_key_name), "");
        pkiEditor.putString(context.getString(R.string.pki_private_key_name), "");
        pkiEditor.putString(context.getString(R.string.pki_csr_key_name), "");
        pkiEditor.putString(context.getString(R.string.pki_x509_signed_cert_name),"");
        pkiEditor.putString(context.getString(R.string.pki_ca_cert_name),"");

        pkiEditor.commit();
        this.retrieveRawCerts();
    }

    private void decodeCerts(){
        pkiPrivateKeyBytes = Base64.decode(pkiPrivateKeyPref, BASE64_CONF);
        pkiPublicKeyBytes = Base64.decode(pkiPublicKeyPref, BASE64_CONF);
        pkiCSRBytes = Base64.decode(pkiCSRPref, BASE64_CONF);
        pkiX509CertBytes = Base64.decode(pkiX509CertPref, BASE64_CONF);
        pkiCaCertBytes = Base64.decode(pkiCaCertPref, BASE64_CONF);
    }

    public KeyPair getKeyPair() throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException {
        KeyFactory kf = KeyFactory.getInstance("RSA",BouncyCastleProvider.PROVIDER_NAME);
        PrivateKey privateKey = kf.generatePrivate(new PKCS8EncodedKeySpec(pkiPrivateKeyBytes));
        PublicKey publicKey = kf.generatePublic(new X509EncodedKeySpec(pkiPublicKeyBytes));
        return new KeyPair(publicKey,privateKey);
    }

    public PKCS10CertificationRequest getCSR() throws IOException {
      return new PKCS10CertificationRequest(pkiCSRBytes);
    }

    public X509Certificate getSignedCert()  {
        CertificateFactory certFactory = null;
        try {
            certFactory = CertificateFactory.getInstance("X.509", BouncyCastleProvider.PROVIDER_NAME);
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
        }
        if(certFactory == null){
            return null;
        }
        InputStream in = new ByteArrayInputStream(pkiX509CertBytes);
        try {
            return (X509Certificate) certFactory.generateCertificate(in);
        } catch (CertificateException e) {
            e.printStackTrace();
        }
        return null;
    }

    public X509Certificate getCaCert() throws CertificateException, NoSuchProviderException {
        CertificateFactory certFactory = CertificateFactory.getInstance("X.509", BouncyCastleProvider.PROVIDER_NAME);
        InputStream in = new ByteArrayInputStream(pkiCaCertBytes);
        return (X509Certificate) certFactory.generateCertificate(in);
    }

    public static CertInfo getCertInfo(X509Certificate cert) throws CertificateEncodingException {
        CertInfo certInfo = new CertInfo();
        X500Name x500Name = new JcaX509CertificateHolder(cert).getSubject();
        Principal p = cert.getSubjectDN();
        Log.v(TAG,p.getName());

//        RDN email = x500Name.getRDNs(BCStyle.EmailAddress)[0];
        RDN cn = x500Name.getRDNs(BCStyle.CN)[0];
        RDN organization = x500Name.getRDNs(BCStyle.O)[0];
        RDN organizationUnit = x500Name.getRDNs(BCStyle.OU)[0];
        RDN country = x500Name.getRDNs(BCStyle.C).length > 0 ? x500Name.getRDNs(BCStyle.C)[0] : null;
        RDN locality = x500Name.getRDNs(BCStyle.L).length > 0 ? x500Name.getRDNs(BCStyle.L)[0] : null;
        RDN state = x500Name.getRDNs(BCStyle.ST).length > 0 ?x500Name.getRDNs(BCStyle.ST)[0] : null;
        String cnStr = cn.getFirst().getValue().toString();
        String organizationStr = organization.getFirst().getValue().toString();
        String organizationUnitStr = organizationUnit.getFirst().getValue().toString();
        String countryStr = country != null ? country.getFirst().getValue().toString() : "";
        String localityStr = locality != null ? locality.getFirst().getValue().toString() : "";
        String stateStr = state != null ? state.getFirst().getValue().toString() : "";
        certInfo.setCountry(countryStr);
        certInfo.setCn(cnStr);
        certInfo.setOrganization(organizationStr);
        certInfo.setLocality(localityStr);
        certInfo.setState(stateStr);
        return certInfo;
    }

}
