package mil.health.sdd.nearbyclient1;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

/**
 * Skeleton of an Android Things activity.
 * <p>
 * Android Things peripheral APIs are accessible through the class
 * PeripheralManagerService. For example, the snippet below will open a GPIO pin and
 * set it to HIGH:
 * <p>
 * <pre>{@code
 * PeripheralManagerService service = new PeripheralManagerService();
 * mLedGpio = service.openGpio("BCM6");
 * mLedGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
 * mLedGpio.setValue(true);
 * }</pre>
 * <p>
 * For more complex peripherals, look for an existing user-space driver, or implement one if none
 * is available.
 *
 * @see <a href="https://github.com/androidthings/contrib-drivers#readme">https://github.com/androidthings/contrib-drivers#readme</a>
 */
public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";
    private SharedPreferences generalPrefs;
    private boolean pkiReady = false;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(TAG,"onCreate called");
        setContentView(R.layout.activity_main);
        generalPrefs = getSharedPreferences(getString(R.string.general_preferences_filename), Context.MODE_PRIVATE);
        pkiReady = generalPrefs.getBoolean(getString(R.string.pki_setup_isready_name),false);
        Button discoverButton = findViewById(R.id.buttonDiscover);
        if(pkiReady){
            discoverButton.setVisibility(View.VISIBLE);
        } else {
            discoverButton.setVisibility(View.INVISIBLE);
        }
    }

    public void discoverCA(View view){
        Intent mDiscoverIntent = new Intent(this,DiscoverActivity.class);
        startActivity(mDiscoverIntent);
    }

    public void managePKI(View view){
        Intent mPKIIntent = new Intent(this,PKIActivity.class);
        startActivity(mPKIIntent);
    }

    @Override
    public void onStart() {
        Log.v(TAG,"onStart called");
        super.onStart();
    }

    @Override
    public void onStop() {
        Log.v(TAG,"onStop called");
        super.onStop();
    }
}
