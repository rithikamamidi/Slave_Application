package com.example.slaveapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsClient;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.Strategy;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.ArrayList;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;


public class MainActivity extends AppCompatActivity {
    private static final Strategy STRATEGY = Strategy.P2P_CLUSTER;
    private ConnectionsClient connectionsClient;
    private final String codeName = "slave";
    private TextView statusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkAndRequestPermissions();
        connectionsClient = Nearby.getConnectionsClient(this);
        statusText = findViewById(R.id.status_text);
    }

    private void startAdvertising() {
        AdvertisingOptions advertisingOptions =
                new AdvertisingOptions.Builder().setStrategy(STRATEGY).build();
        Nearby.getConnectionsClient(this)
                .startAdvertising(
                        codeName, "com.example.slaveapplication", connectionLifecycleCallback, advertisingOptions)
                .addOnSuccessListener(
                        new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void unused) {
                                statusText.append("\n"+"Started advertising");
                                System.out.println("Started advertising");
                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                statusText.append("\n"+"Cannot start advertising"+e);
                                System.out.println("Cannot start advertising"+e);
                            }
                        });
    }

    private final ConnectionLifecycleCallback connectionLifecycleCallback =
            new ConnectionLifecycleCallback() {
                @Override
                public void onConnectionInitiated(String endpointId, ConnectionInfo connectionInfo) {
                    statusText.append("\n"+"Connection Initiated"+endpointId);
                    statusText.append("\n"+"Accepting connection"+endpointId+connectionInfo);
                    connectionsClient.acceptConnection(endpointId, payloadCallback);
//                    opponentName = connectionInfo.getEndpointName();
                }

                @Override
                public void onConnectionResult(String endpointId, ConnectionResolution result) {
                    if (result.getStatus().isSuccess()) {
//                        Log.i(TAG, "onConnectionResult: connection successful");
                        statusText.append("\n"+"Connection successful!!");
                        System.out.println("Connection successful");

//                        connectionsClient.stopDiscovery();

//                        opponentEndpointId = endpointId;
//                        setOpponentName(opponentName);
//                        setStatusText(getString(R.string.status_connected));
//                        setButtonState(true);
                    } else {
                        statusText.append("\n"+"Connection failed :(");
                        System.out.println("Connection failed");
//                        Log.i(TAG, "onConnectionResult: connection failed");
                    }
                }

                @Override
                public void onDisconnected(String endpointId) {
                    statusText.append("\n"+"Connection Disconnected");
                    System.out.println("Connection disconnected");
//                    Log.i(TAG, "onDisconnected: disconnected from the opponent");
                }
            };

    private final PayloadCallback payloadCallback =
            new PayloadCallback() {
                @Override
                public void onPayloadReceived(String endpointId, Payload payload) {
                    String textMessage = new String(payload.asBytes(), UTF_8);
                    statusText.append("\n"+"Payload received"+ textMessage);
                    System.out.println("Payload received");
//                    opponentChoice = GameChoice.valueOf(new String(payload.asBytes(), UTF_8));
                }

                @Override
                public void onPayloadTransferUpdate(String endpointId, PayloadTransferUpdate update) {
                    statusText.append("\n"+"Payload Transfer");
//                    if (update.getStatus() == Status.SUCCESS && myChoice != null && opponentChoice != null) {
//                        finishRound();
//                    }
                }
            };

    public void advertiseDevice(View view) {
        startAdvertising();
    }

    public boolean checkAndRequestPermissions() {
        int internet = ContextCompat.checkSelfPermission(this,
                Manifest.permission.INTERNET);
        int loc = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION);
        int loc2 = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        List<String> listPermissionsNeeded = new ArrayList<>();

        if (internet != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.INTERNET);
        }
        if (loc != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }
        if (loc2 != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions((Activity) this, listPermissionsNeeded.toArray
                    (new String[listPermissionsNeeded.size()]), 1);
            return false;
        }
        return true;
    }
}
