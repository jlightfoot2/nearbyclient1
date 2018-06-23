package mil.health.sdd.nearbyclient1;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pqc.jcajce.provider.BouncyCastlePQCProvider;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
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

public class PKIPreferences {
    private Context context;
    private SharedPreferences sharedPreferences;
    private String pkiPrivateKeyPref;
    private String pkiPublicKeyPref;
    private String pkiCSRPref;
    private String pkiX509CertPref;
    public byte[] pkiPrivateKeyBytes;
    public byte[] pkiPublicKeyBytes;
    public byte[] pkiCSRBytes;
    public byte[] pkiX509CertBytes;
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
        if(isSetup()){
            this.decodeCerts();
        }
    }

    public boolean isSetup(){
        return !(pkiPrivateKeyPref.isEmpty() || pkiPublicKeyPref.isEmpty() || pkiCSRPref.isEmpty());
    }

    public void store(KeyPair caKeyPair,PKCS10CertificationRequest csr) throws CertificateEncodingException, IOException {
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

    public void deleteAll(){
        SharedPreferences.Editor pkiEditor = sharedPreferences.edit();
        pkiEditor.putString(context.getString(R.string.pki_public_key_name), "");
        pkiEditor.putString(context.getString(R.string.pki_private_key_name), "");
        pkiEditor.putString(context.getString(R.string.pki_csr_key_name), "");
        pkiEditor.putString(context.getString(R.string.pki_x509_signed_cert_name),"");

        pkiEditor.commit();
        this.retrieveRawCerts();
    }

    private void decodeCerts(){
        pkiPrivateKeyBytes = Base64.decode(pkiPrivateKeyPref, BASE64_CONF);
        pkiPublicKeyBytes = Base64.decode(pkiPublicKeyPref, BASE64_CONF);
        pkiCSRBytes = Base64.decode(pkiCSRPref, BASE64_CONF);
        pkiX509CertBytes = Base64.decode(pkiX509CertPref, BASE64_CONF);
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

    public X509Certificate getSignedCert() throws CertificateException, NoSuchProviderException {
        CertificateFactory certFactory = CertificateFactory.getInstance("X.509", BouncyCastlePQCProvider.PROVIDER_NAME);
        InputStream in = new ByteArrayInputStream(pkiX509CertBytes);
        return (X509Certificate) certFactory.generateCertificate(in);
    }

}
