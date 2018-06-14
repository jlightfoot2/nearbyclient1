package mil.health.sdd.nearbyclient1;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

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
    private ConnectionsClient mConnectionsClient;
    private String connectionAuthenticationToken = "";
    public String mEndPointId = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_discover);
        Log.v(TAG,"onCreate called");
        startDiscovery();
    }
    private final PayloadCallback mPayloadCallback =
            new PayloadCallback() {
                @Override
                public void onPayloadReceived(String endpointId, Payload payload) {
//                    opponentChoice = GameChoice.valueOf(new String(payload.asBytes(), UTF_8));
                }

                @Override
                public void onPayloadTransferUpdate(String endpointId, PayloadTransferUpdate update) {
//                    if (update.getStatus() == Status.SUCCESS && myChoice != null && opponentChoice != null) {
//                        finishRound();
//                    }
                }
            };
    private final EndpointDiscoveryCallback mEndpointDiscoveryCallback =
            new EndpointDiscoveryCallback() {
                @Override
                public void onEndpointFound(
                        String endpointId, DiscoveredEndpointInfo discoveredEndpointInfo) {
                    // An endpoint was found!
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
        mConnectionsClient = Nearby.getConnectionsClient(this);
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

    public void stopDiscovery(View view){
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    public void acceptConnection(View view){
        if(!mEndPointId.isEmpty()){
            mConnectionsClient.acceptConnection(mEndPointId, mPayloadCallback);
        }
    }

    public void onEndpointFound(
            String endpointId, DiscoveredEndpointInfo discoveredEndpointInfo) {
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
                    EditText tokenText = findViewById(R.id.editTextAcceptConnection);
                    tokenText.setText(connectionAuthenticationToken);
                    mEndPointId = endpointId;
                    Log.v(TAG,"authentication token received");
                    //mConnectionsClient.acceptConnection(endpointId, mPayloadCallback);
                }

                @Override
                public void onConnectionResult(String endpointId, ConnectionResolution result) {
                    switch (result.getStatus().getStatusCode()) {
                        case ConnectionsStatusCodes.STATUS_OK:
                            // We're connected! Can now start sending and receiving data.
                            Log.v(TAG,"We're connected! Can now start sending and receiving data");
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
