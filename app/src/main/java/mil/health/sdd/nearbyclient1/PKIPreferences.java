package mil.health.sdd.nearbyclient1;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.Security;
import java.security.cert.CertificateEncodingException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

public class PKIPreferences {
    private Context context;
    private SharedPreferences sharedPreferences;
    private String caPrivateKeyPref;
    private String caPublicKeyPref;
    public byte[] caPrivateKeyBytes;
    public byte[] caPublicKeyBytes;
    private static final int BASE64_CONF = Base64.NO_WRAP;
    public PKIPreferences(Context context,String share_prefs_filename){
        this.context = context;
        sharedPreferences = this.context.getSharedPreferences(share_prefs_filename, Context.MODE_PRIVATE);

        Provider bcProvider = new BouncyCastleProvider();
        Security.addProvider(bcProvider);

        this.retrieveRawCerts();
    }

    private void retrieveRawCerts(){
        caPrivateKeyPref = sharedPreferences.getString(context.getString(R.string.pki_private_key_name),"");
        caPublicKeyPref = sharedPreferences.getString(context.getString(R.string.pki_public_key_name),"");
        if(isSetup()){
            this.decodeCerts();
        }
    }

    public boolean isSetup(){
        return !(caPrivateKeyPref.isEmpty() || caPublicKeyPref.isEmpty());
    }

    public void store(KeyPair caKeyPair) throws CertificateEncodingException {
        SharedPreferences.Editor pkiEditor = sharedPreferences.edit();
        pkiEditor.putString(context.getString(R.string.pki_public_key_name), Base64.encodeToString(caKeyPair.getPublic().getEncoded(), BASE64_CONF));
        pkiEditor.putString(context.getString(R.string.pki_private_key_name), Base64.encodeToString(caKeyPair.getPrivate().getEncoded(), BASE64_CONF));
        pkiEditor.commit();
        this.retrieveRawCerts();
    }

    private void decodeCerts(){
        caPrivateKeyBytes  = Base64.decode(caPrivateKeyPref, BASE64_CONF);
        caPublicKeyBytes  = Base64.decode(caPublicKeyPref, BASE64_CONF);
    }

    public KeyPair getKeyPair() throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException {
        KeyFactory kf = KeyFactory.getInstance("RSA",BouncyCastleProvider.PROVIDER_NAME);
        PrivateKey privateKey = kf.generatePrivate(new PKCS8EncodedKeySpec(caPrivateKeyBytes));
        PublicKey publicKey = kf.generatePublic(new X509EncodedKeySpec(caPublicKeyBytes));
        return new KeyPair(publicKey,privateKey);
    }

}
