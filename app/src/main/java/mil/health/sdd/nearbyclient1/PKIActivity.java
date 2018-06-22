package mil.health.sdd.nearbyclient1;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.Security;
import java.security.cert.CertificateEncodingException;

public class PKIActivity extends Activity {
    private static final String TAG = "PKIActivity";

    private static final String CA_KEY_ALIAS = "andoidIotCA";
    private static final String CA_CN ="android-dha-client1.local";
    private static final String CA_CN_PATTERN ="CN=%s, O=DHA, OU=SDD";
    private static PKIPreferences pkPrefs;
    private static SharedPreferences generalPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pki);

        generalPrefs = getSharedPreferences(getString(R.string.general_preferences_filename), Context.MODE_PRIVATE);
        pkPrefs = new PKIPreferences(this,getString(R.string.pki_preferences_filename));
        if(!pkPrefs.isSetup()){
            try {
                createKeyPair();
            } catch (InvalidAlgorithmParameterException e) {
                e.printStackTrace();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (NoSuchProviderException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (OperatorCreationException e) {
                e.printStackTrace();
            } catch (CertificateEncodingException e) {
                e.printStackTrace();
            }
        } else {
            notifyUser("Certs already setup");
        }
    }

    @Override
    protected void onStart() {

        super.onStart();

    }
    private void notifyUser(String msg){
//        Snackbar.make(findViewById(R.id.pkiCoordinatorLayout), msg,
//                Snackbar.LENGTH_SHORT).show(); //Relies on AppCompat so doesn't work

        Context context = getApplicationContext();
        CharSequence text = msg;
        int duration = Toast.LENGTH_SHORT;

        Toast toast = Toast.makeText(context, text, duration);
        toast.show();
    }

    public void pkiComplete(View view){
        Intent mPKIIntent = new Intent(this,MainActivity.class);
        startActivity(mPKIIntent);
    }

    private void createKeyPair() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchProviderException, IOException, OperatorCreationException, CertificateEncodingException {
        Provider bcProvider = new BouncyCastleProvider();
        Security.addProvider(bcProvider);
        KeyPair kp = PKIHelper.createKeyPair();
        String cnString = String.format(CA_CN_PATTERN, CA_CN);
        PKCS10CertificationRequest csr = CSRHelper.generateCSR(kp,cnString);

        PKIPreferences pkPrefs = new PKIPreferences(this,getString(R.string.pki_preferences_filename));
        pkPrefs.store(kp);
        SharedPreferences.Editor genPrefEditor = generalPrefs.edit();
        if(pkPrefs.isSetup()){
            genPrefEditor.putBoolean(getString(R.string.pki_setup_isready_name),true);
            notifyUser("Certs setup");

        } else {
            genPrefEditor.putBoolean(getString(R.string.pki_setup_isready_name),false);
            notifyUser("Certs setup failed");
        }
        genPrefEditor.commit();

    }
}
