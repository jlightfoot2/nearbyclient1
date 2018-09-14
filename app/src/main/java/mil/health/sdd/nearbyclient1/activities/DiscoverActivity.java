package mil.health.sdd.nearbyclient1.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsClient;
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.Strategy;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import mil.health.sdd.nearbyclient1.PKIPreferences;
import mil.health.sdd.nearbyclient1.R;

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
public class DiscoverActivity extends Activity {
    public static final String SERVICE_ID = "mil.health.sdd.nearbyclient2.CA_SYSTEM";
    private static final Strategy STRATEGY = Strategy.P2P_STAR;
    public static final String TAG = "DiscoverActivity";
    private String connectionAuthenticationToken = "";
    public String mEndPointId = null;
    private static PKIPreferences pkPrefs;
    private static SharedPreferences generalPrefs;
    private DiscoverActivity activityInstance;
    private byte[] x509SignedCertBytes = null;
    private byte[] caCertificateBytes = null;
    private long csrPayloadId = 0;
    private ConnectionsClient mConnectionsClient;

    private boolean certReceived = false;
    private boolean caReceived = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_discover);
        Log.v(TAG,"onCreate called");

        generalPrefs = getSharedPreferences(getString(R.string.general_preferences_filename), Context.MODE_PRIVATE);
        activityInstance = this;
        mConnectionsClient = Nearby.getConnectionsClient(this);
        startDiscovery();
    }

    private void notifyUser(String msg){

        Context context = getApplicationContext();
        CharSequence text = msg;
        int duration = Toast.LENGTH_SHORT;

        Toast toast = Toast.makeText(context, text, duration);
        toast.show();
    }
    private final PayloadCallback mPayloadCallback =
            new PayloadCallback() {
                @Override
                public void onPayloadReceived(String remoteEndpointId, Payload payload) {
//                    opponentChoice = GameChoice.valueOf(new String(payload.asBytes(), UTF_8));
                    Log.v(TAG,"onPayloadReceived");
                    if(!certReceived){
                        x509SignedCertBytes = payload.asBytes();
                        try {
                            CertificateFactory certFactory = CertificateFactory.getInstance("X.509", BouncyCastleProvider.PROVIDER_NAME);
                            InputStream in = new ByteArrayInputStream(x509SignedCertBytes);
                            X509Certificate cert = (X509Certificate) certFactory.generateCertificate(in);
                            pkPrefs.store(cert);
                            notifyUser("x509 returned and stored");
                            Log.v(TAG,"x509 returned and stored");
                            pkPrefs.getSignedCert();
                            Log.v(TAG,"x509 retrieved");
                            notifyUser("x509 retrieved");
                        } catch (CertificateException e) {

                            Log.e(TAG,"Payload CertificateException",e);
                        } catch (NoSuchProviderException e) {
                            Log.e(TAG,"Payload NoSuchProviderException",e);
                        }
                    } else {
                        caCertificateBytes = payload.asBytes();
                        //TODO store ca in prefrences to be used later
                        try {
                            CertificateFactory certFactory = CertificateFactory.getInstance("X.509", BouncyCastleProvider.PROVIDER_NAME);
                            InputStream in = new ByteArrayInputStream(caCertificateBytes);
                            X509Certificate cert = (X509Certificate) certFactory.generateCertificate(in);
                            pkPrefs.storeCa(cert);
                            notifyUser("CA Cert returned and stored");
                            Log.v(TAG,"CA Cert returned and stored");
                            pkPrefs.getCaCert();
                            Log.v(TAG,"CA Cert retrieved");
                            notifyUser("CA Cert retrieved");
                        } catch (CertificateException e) {

                            Log.e(TAG,"Payload CertificateException",e);
                        } catch (NoSuchProviderException e) {
                            Log.e(TAG,"Payload NoSuchProviderException",e);
                        }
                    }

                }

                @Override
                public void onPayloadTransferUpdate(String sendingEndpointId, PayloadTransferUpdate update) {

                    if (update.getStatus() == PayloadTransferUpdate.Status.SUCCESS && update.getPayloadId() == csrPayloadId) {
                        notifyUser("CSR Sent " + update.getTotalBytes());
                    } else if(!certReceived && update.getStatus() == PayloadTransferUpdate.Status.SUCCESS) {
                        notifyUser("Signed Cert returned " + update.getTotalBytes());
                        certReceived = true;
                    } else if(update.getStatus() == PayloadTransferUpdate.Status.SUCCESS) {
                        notifyUser("CA Cert Returned " + update.getTotalBytes());
                    }
                }
            };
    private final EndpointDiscoveryCallback mEndpointDiscoveryCallback =
            new EndpointDiscoveryCallback() {
                @Override
                public void onEndpointFound(
                        String endpointId, DiscoveredEndpointInfo discoveredEndpointInfo) {
                    // An endpoint was found!
                    EditText headerText = findViewById(R.id.editTextConnectionHeader);
                    headerText.setText("Endpoint Found!");

                    mEndPointId = endpointId;
                    EditText tokenText = findViewById(R.id.editTextConnectionStatus);
                    tokenText.setText("Endpoint: " + mEndPointId);
                    //onEndpointFound(endpointId, discoveredEndpointInfo); causes infinite loop
                    Log.v(TAG,"An endpoint was found!");
                }

                @Override
                public void onEndpointLost(String endpointId) {
                    // A previously discovered endpoint has gone away.
                    mEndPointId = null;
                    Log.v(TAG,"A previously discovered endpoint has gone away.");
                }
            };

    private void startDiscovery() {
        mConnectionsClient.startDiscovery(
                SERVICE_ID,
                mEndpointDiscoveryCallback,
                new DiscoveryOptions(STRATEGY))
                .addOnSuccessListener(
                        new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void unusedResult) {
                                // We're discovering!
                                Log.v(TAG,"We're discovering!");
                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                // We were unable to start discovering.

                                Log.e(TAG,"We were unable to start discovering",e);
                            }
                        });
    }

    @Override
    protected void onStop() {
        stopNearbyDiscover();
        Log.v(TAG,"onStop called in Activity");
        super.onStop();
    }

    public void stopDiscovery(View view){
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    public void stopNearbyDiscover(){
        mConnectionsClient.stopDiscovery();
        mConnectionsClient.stopAllEndpoints();
    }

    public void acceptConnection(View view){
        Log.v(TAG,"User clicks to accept connection ");
        mConnectionsClient.stopDiscovery();
        if(!mEndPointId.isEmpty()){
            Log.v(TAG,"User accepts advertising endpoint " + mEndPointId);
            mConnectionsClient.acceptConnection(mEndPointId, mPayloadCallback);
        }
    }

    public void requestConnection(View view){
        sendConnectionRequest(mEndPointId);
    }

    public void sendConnectionRequest(
            String endpointId) {
        mConnectionsClient.requestConnection(
                SERVICE_ID,
                endpointId,
                mConnectionLifecycleCallback)
                .addOnSuccessListener(
                        new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void unusedResult) {
                                // We successfully requested a connection. Now both sides
                                // must accept before the connection is established.
                                Log.v(TAG, "addOnSuccessListener > onSuccess");
                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                // Nearby Connections failed to request the connection.
                                Log.e(TAG,"Nearby Connections failed to request the connection",e);
                            }
                        });
    }

    private final ConnectionLifecycleCallback mConnectionLifecycleCallback =
            new ConnectionLifecycleCallback() {

                @Override
                public void onConnectionInitiated(
                        String endpointId, ConnectionInfo connectionInfo) {
                    // Automatically accept the connection on both sides.
                    connectionAuthenticationToken = connectionInfo.getAuthenticationToken();
                    EditText tokenText = findViewById(R.id.editTextConnectionStatus);
                    tokenText.setText("Token: " + connectionAuthenticationToken);


                    EditText headerText = findViewById(R.id.editTextConnectionHeader);
                    headerText.setText("Please verify token");

                    Log.v(TAG,"authentication token received");
                    //mConnectionsClient.acceptConnection(endpointId, mPayloadCallback);
                }

                @Override
                public void onConnectionResult(String endpointId, ConnectionResolution result) {
                    switch (result.getStatus().getStatusCode()) {
                        case ConnectionsStatusCodes.STATUS_OK:
                            // We're connected! Can now start sending and receiving data.
                            Log.v(TAG,"We're connected! Can now start sending and receiving data");
                            pkPrefs = new PKIPreferences(activityInstance,getString(R.string.pki_preferences_filename));

                            if(pkPrefs.isSetup()){
                                Payload csrPayload = null;
                                try {
                                    pkPrefs = new PKIPreferences(activityInstance,getString(R.string.pki_preferences_filename));
                                    Log.v(TAG,"client: Sending CSR bytes to CA");
                                    csrPayload= Payload.fromBytes(pkPrefs.getCSR().getEncoded());
                                    csrPayloadId = csrPayload.getId();
                                    certReceived = false;
                                    mConnectionsClient.sendPayload(mEndPointId, csrPayload);
                                    Log.v(TAG,"CSR bytes send to CA");
                                } catch (IOException e) {
                                    Log.e(TAG,"could not retrieve CSR from preferences",e);
                                    mConnectionsClient.stopAllEndpoints();
                                }

                            }
                            break;
                        case ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED:
                            // The connection was rejected by one or both sides.
                            Log.v(TAG,"The connection was rejected by one or both sides.");
                            break;
                        case ConnectionsStatusCodes.STATUS_ERROR:
                            // The connection broke before it was able to be accepted.
                            Log.v(TAG,"The connection broke before it was able to be accepted.");
                            break;
                    }
                }

                @Override
                public void onDisconnected(String endpointId) {
                    // We've been disconnected from this endpoint. No more data can be
                    Log.v(TAG,"We've been disconnected from this endpoint. No more data can be sent or received.");
                }
            };


}
