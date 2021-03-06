package mil.health.sdd.nearbyclient1.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
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
import java.security.cert.CertificateException;

import mil.health.sdd.nearbyclient1.GeneralPreferencesHelper;
import mil.health.sdd.nearbyclient1.PKIPreferences;
import mil.health.sdd.nearbyclient1.R;
import mil.health.sdd.nearbyclient1.fragments.X509CertFragment;
import mil.health.sdd.nearbyclient1.helper.CSRHelper;
import mil.health.sdd.nearbyclient1.helper.PKIHelper;

public class PKIActivity extends AppCompatActivity implements X509CertFragment.CertificateListener {
    private static final String TAG = "PKIActivity";

    private static final String CA_CN ="dha-android-client.local";
    private static final String CA_CN_PATTERN ="CN=%s, O=DHA, OU=SDD, L=Tacoma, ST=WA, C=US";
    private static PKIPreferences pkPrefs;
    private static SharedPreferences generalPrefs;
    X509CertFragment localCertFragment;
    X509CertFragment caCertFragment;

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
            if(pkPrefs.getSignedCert() != null){
                FragmentManager fm = getSupportFragmentManager();
                FragmentTransaction ft1 = fm.beginTransaction();
                localCertFragment = new X509CertFragment();


                try {
                    localCertFragment.setCert(pkPrefs.getCertInfo(pkPrefs.getSignedCert()));
                    localCertFragment.setTitle("Device Certificate");
                } catch (CertificateEncodingException e) {
                    e.printStackTrace();
                }

                Log.v(TAG,"adding Fragment R.id.fragmentLocalCertContainer");
                ft1.add(R.id.fragmentLocalCertContainer,localCertFragment);

                caCertFragment = new X509CertFragment();

                try {
                    caCertFragment.setCert(pkPrefs.getCertInfo(pkPrefs.getCaCert()));
                    caCertFragment.setTitle("Certificate Authority");
                    Log.v(TAG,"adding Fragment R.id.fragmentCaCertContainer");
                    ft1.add(R.id.fragmentCaCertContainer,caCertFragment);
                } catch (CertificateEncodingException e) {
                    e.printStackTrace();
                } catch (CertificateException e) {
                    e.printStackTrace();
                } catch (NoSuchProviderException e) {
                    e.printStackTrace();
                }

                ft1.commit();
            } else {
                notifyUser("Please proceed to CSR request");
            }



        }
    }

    @Override
    protected void onStart() {

        super.onStart();

    }

    @Override
    protected void onStop() {
//        FragmentManager fm = getSupportFragmentManager();
//        FragmentTransaction ft2 = fm.beginTransaction();
//        if(caCertFragment != null){
//            ft2.remove(caCertFragment);
//        }
//        if(localCertFragment != null){
//            ft2.remove(localCertFragment);
//        }
//        ft2.commit();
        super.onStop();

    }

    public void onClickDelete(){

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

        GeneralPreferencesHelper gph = new GeneralPreferencesHelper(generalPrefs);

        String app_uuid = gph.getUUID(getString(R.string.app_uuid_name));

        String cnString = String.format(CA_CN_PATTERN, app_uuid + "." + CA_CN);
        PKCS10CertificationRequest csr = CSRHelper.generateCSR(kp,cnString);

        PKIPreferences pkPrefs = new PKIPreferences(this,getString(R.string.pki_preferences_filename));
        pkPrefs.store(kp,csr);
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
