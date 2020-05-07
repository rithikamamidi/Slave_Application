package com.example.slaveapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.BatteryManager;
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

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import static java.nio.charset.StandardCharsets.UTF_8;


public class MainActivity extends AppCompatActivity {
    private static final Strategy STRATEGY = Strategy.P2P_CLUSTER;
    private ConnectionsClient connectionsClient;
    private final String codeName = "slave";
    private TextView statusText;
    private TextView executionTime;
    private TextView battery;
    public String batteryLevel_slave;
    public String masterEndpoint;
    private double lati;
    public double jLatitude;
    public double jLongitude;
    private double longi;
    private TextView latitude_GPS;
    private TextView longitude_GPS;
    LocationManager locationManager;
    LocationListener locationListener;
    public long startTime;
    public long endTime;
    public long duration;

    private BroadcastReceiver batterylevelReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
//            battery.setText(String.valueOf(level) + "%");
            batteryLevel_slave = String.valueOf(level);
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        startTime = System.currentTimeMillis();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkAndRequestPermissions();
        connectionsClient = Nearby.getConnectionsClient(this);
        statusText = findViewById(R.id.status_text);
//        battery = (TextView) findViewById(R.id.batteryLevel);
        this.registerReceiver(this.batterylevelReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        latitude_GPS = findViewById(R.id.latitude);
        longitude_GPS = findViewById(R.id.longitude);
        locationManager = (LocationManager) this.getSystemService(LOCATION_SERVICE);
        boolean isGPS_enabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        if (isGPS_enabled) {
            locationListener = new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    double longitude = location.getLongitude();
                    double latitude = location.getLatitude();
                    try {
                        Geocoder geocoder = new Geocoder(MainActivity.this, Locale.getDefault());
                        List<Address> addressList = geocoder.getFromLocation(latitude, longitude, 1);
                        lati = addressList.get(0).getLatitude();
                        jLatitude = addressList.get(0).getLatitude();
                        jLongitude = addressList.get(0).getLongitude();
                        longi = addressList.get(0).getLongitude();
                        latitude_GPS.setText("latitude:" + lati);
                        longitude_GPS.setText("longitude:" + longi);

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {

                }

                @Override
                public void onProviderEnabled(String provider) {

                }

                @Override
                public void onProviderDisabled(String provider) {

                }
            };
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        } else {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
                latitude_GPS.setText("getting Location");
                longitude_GPS.setText("getting Location");
            }
        } else {
            latitude_GPS.setText("getting latitude");
            longitude_GPS.setText("getting longitude");
        }
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
                                statusText.append("\n" + "Started advertising");
                                System.out.println("Started advertising");
                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                statusText.append("\n" + "Cannot start advertising" + e);
                                System.out.println("Cannot start advertising" + e);
                            }
                        });
    }

    private final ConnectionLifecycleCallback connectionLifecycleCallback =
            new ConnectionLifecycleCallback() {
                @Override
                public void onConnectionInitiated(String endpointId, ConnectionInfo connectionInfo) {
//                    statusText.append("\n" + "Connection Initiated" + endpointId);
//                    statusText.append("\n" + "Accepting connection" + endpointId + connectionInfo);
                    connectionsClient.acceptConnection(endpointId, payloadCallback);
                }

                @Override
                public void onConnectionResult(String endpointId, ConnectionResolution result) {
                    if (result.getStatus().isSuccess()) {
                        statusText.append("\n" + "Connection successful! With client having endpoint:"+endpointId);
                        System.out.println("Connection successful");

                        masterEndpoint = endpointId;
                        final String batteryPercentage = batteryLevel_slave;
                        JSONObject obj = new JSONObject();

                        /*try {
                            obj.put("batteryLevel", batteryPercentage);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        connectionsClient.sendPayload(
                                endpointId, Payload.fromBytes(obj.toString().getBytes(StandardCharsets.UTF_8)));*/
                        AlertDialog.Builder mBuilder = new AlertDialog.Builder(MainActivity.this);
                        mBuilder.setTitle("Do you want to let master monitor the battery level?");
                        mBuilder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                               final JSONObject obj_bl = new JSONObject();
                                final JSONObject obj_location = new JSONObject();
                                Timer timer = new Timer();
                                timer.scheduleAtFixedRate(new TimerTask()
                                {
                                    public void run()
                                    {
                                        // Your code
                                        try {
                                            obj_bl.put("batteryLevel", batteryPercentage);
                                            obj_location.put("latitude",jLatitude);
                                            obj_location.put("longitude",jLongitude);
                                            obj_bl.put("location", obj_location);

                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }


                                        connectionsClient.sendPayload(
                                                masterEndpoint, Payload.fromBytes(obj_bl.toString().getBytes(StandardCharsets.UTF_8)));



                                    }
                                }, 1000, 5000);


                            }
                        });
                        mBuilder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                JSONObject obj = new JSONObject();

                                try {
                                    obj.put("batterylevel", "disconnect");
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                connectionsClient.sendPayload(
                                        masterEndpoint, Payload.fromBytes(obj.toString().getBytes(StandardCharsets.UTF_8)));
                            }

                        });
                        AlertDialog alertDialog = mBuilder.create();
                        alertDialog.show();
                    } else {
                        statusText.append("\n" + "Connection failed :(");
                        System.out.println("Connection failed");
                    }
                }

                @Override
                public void onDisconnected(String endpointId) {
                    statusText.append("\n" + "Connection Disconnected");
                    System.out.println("Connection disconnected");
                }
            };

    private final PayloadCallback payloadCallback =
            new PayloadCallback() {
                @Override
                public void onPayloadReceived(String endpointId, Payload payload) {
                    String textMessage = new String(payload.asBytes(), UTF_8);
//                    statusText.append("\n" + "Payload received" + textMessage);
                    JSONObject payload_received = null;
                    try {
                        payload_received = new JSONObject(textMessage);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    Iterator<String> keys = payload_received.keys();
                    String A = null, B = null;
                    int r_a = 0, r_b = 0, c_a = 0, c_b = 0, s_itr = 0, e_itr = 0;
                    while (keys.hasNext()) {
                        String key = keys.next();
                        switch (key) {
                            case "request": {
                                AlertDialog.Builder mBuilder = new AlertDialog.Builder(MainActivity.this);
                                mBuilder.setTitle("Do you want to proceed with the processing?");
                                mBuilder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        JSONObject obj = new JSONObject();

                                        try {
                                            obj.put("request", "yes");
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                        connectionsClient.sendPayload(
                                                masterEndpoint, Payload.fromBytes(obj.toString().getBytes(StandardCharsets.UTF_8)));
                                    }
                                });
                                mBuilder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        JSONObject obj = new JSONObject();

                                        try {
                                            obj.put("request", "no");
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                        connectionsClient.sendPayload(
                                                masterEndpoint, Payload.fromBytes(obj.toString().getBytes(StandardCharsets.UTF_8)));
                                    }

                                });
                                AlertDialog alertDialog = mBuilder.create();
                                alertDialog.show();
                                break;
                            }

                            case "matrix_A": {
                                try {
                                    A = (String) payload_received.get(key);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                break;
                            }
                            case "matrix_B": {
                                try {
                                    B = (String) payload_received.get(key);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                break;
                            }
                            case "rows_a": {
                                try {
                                    System.out.println("PAYLOAD" + payload_received + key);
                                    r_a = (int) payload_received.get(key);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                break;
                            }
                            case "columns_a": {
                                try {
                                    c_a = (int) payload_received.get(key);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                break;
                            }
                            case "rows_b": {
                                try {
                                    r_b = (int) payload_received.get(key);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                break;
                            }
                            case "columns_b": {
                                try {
                                    c_b = (int) payload_received.get(key);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                break;
                            }
                            case "s_itr": {
                                try {
                                    s_itr = (int) payload_received.get(key);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                break;
                            }
                            case "e_itr": {
                                try {
                                    e_itr = (int) payload_received.get(key);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                break;
                            }
                        }
                    }
                    if (A != null && B != null) {
                        int[][] matrix_a = new int[r_a][c_a];
                        int[][] matrix_b = new int[r_b][c_b];
                        int[][] matrix_c = new int[r_a][c_b];
                        String[] array_a = A.split(",");
                        String[] array_b = B.split(",");
                        String mat_c_string = "";

                        int i = 0;
                        for (int j = 0; j < r_a; j++) {
                            for (int k = 0; k < c_a; k++) {
                                matrix_a[j][k] = Integer.parseInt(array_a[i]);
                                System.out.println("HERE" + matrix_a[j][k]);
                                i++;
                            }
                        }
                        i = 0;
                        for (int j = 0; j < r_b; j++) {
                            for (int k = 0; k < c_b; k++) {
                                matrix_b[j][k] = Integer.parseInt(array_b[i]);
                                System.out.println("HERE B" + matrix_b[j][k]);
                                i++;
                            }
                        }
                        for (int[] row : matrix_a) {
                            System.out.println("Matrix A");
                            System.out.println(Arrays.toString(row));
                        }

                        for (int[] row : matrix_b) {
                            System.out.println("Matrix B");
                            System.out.println(Arrays.toString(row));
                        }

//                        statusText.append("\nTest data" + matrix_a.toString() + matrix_b.toString());
                        for (int x = s_itr; x < e_itr; x++) {
                            for (int j = 0; j < c_b; j++) {
                                for (int k = 0; k < r_b; k++) {
                                    matrix_c[x][j] = matrix_c[x][j] + matrix_a[x][k] * matrix_b[k][j];
                                }
                            }
                        }

                        for (int x = s_itr; x < e_itr; x++) {
                            for (int j = 0; j < c_b; j++) {
                                mat_c_string += matrix_c[x][j] + ",";
                            }
                        }
                        mat_c_string = mat_c_string.replaceAll(",$", "");
                        JSONObject matrix_calculation_result = new JSONObject();
                        try {
                            matrix_calculation_result.put("s_itr", s_itr);
                            matrix_calculation_result.put("e_itr", e_itr);
                            matrix_calculation_result.put("r_a", r_a);
                            matrix_calculation_result.put("c_a", c_a);
                            matrix_calculation_result.put("r_b", r_b);
                            matrix_calculation_result.put("c_b", c_b);
                            matrix_calculation_result.put("calculation_result", mat_c_string);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        endTime = System.currentTimeMillis();
                        duration = (endTime - startTime);
                        executionTime = findViewById(R.id.et);
                        statusText.append("\n"+"Sent computation result to master");
                        System.out.println("durataion:"+duration);


                        connectionsClient.sendPayload(masterEndpoint, Payload.fromBytes(matrix_calculation_result.toString().getBytes(UTF_8)));

                    }


                    System.out.println("Payload received");
                }

                @Override
                public void onPayloadTransferUpdate(String endpointId, PayloadTransferUpdate update) {
//                    statusText.append("\n" + "Payload Transfer");
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
