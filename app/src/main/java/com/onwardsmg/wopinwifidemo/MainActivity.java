package com.onwardsmg.wopinwifidemo;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.util.Log;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.squareup.okhttp.Callback;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{
    private static final String TAG = "MainActivity";

    private ArrayList<String> listItems=new ArrayList<String>();
    private ArrayAdapter<String> adapter;
    private ListView list;

    private WifiConnectionReceiver wifiConnectionReceiver;
    private Handler handler = new Handler();
    private OkHttpClient client = new OkHttpClient();
    private EditText editTextSSID;
    private EditText editTextPassword;

    //Mqtt Related
    private static final String URL = "tcp://wifi.h2popo.com:8083";
    private static final String username = "wopin";
    private static final String password = "wopinH2popo";
    private static final String clientId = "clientId";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED){
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)){
                Toast.makeText(this, "The permission to get BLE location data is required", Toast.LENGTH_SHORT).show();
            }else{
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }
        }else{
            Toast.makeText(this, "Location permissions already granted", Toast.LENGTH_SHORT).show();
        }

        wifiConnectionReceiver = new WifiConnectionReceiver(this.getApplicationContext());

        findViewById(R.id.buttonConnectWifi).setOnClickListener(this);
        findViewById(R.id.buttonLink).setOnClickListener(this);
        findViewById(R.id.buttonPublish).setOnClickListener(this);
        findViewById(R.id.buttonSubscribe).setOnClickListener(this);
        list =  findViewById(R.id.list);
        editTextSSID = findViewById(R.id.editTextSSID);
        editTextPassword = findViewById(R.id.editTextPassword);
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_expandable_list_item_1, listItems);
        list.setAdapter(adapter);

        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String ssid = adapterView.getItemAtPosition(i).toString();
                Log.d(TAG, "Select ssid : " + ssid);
                editTextSSID.setText(ssid);
            }
        });
        start();
    }

    @Override
    public void onClick(View v) {
        Log.d(TAG, "Button is clicked " + v.getId());
        switch (v.getId()) {
            case R.id.buttonConnectWifi:
                Log.d(TAG, "Connect Wifi is clicked");
                //ToDo: Get this from QR code
                wifiConnectionReceiver.connectToWifi("H2PoPo", "12345678");
                break;
            case R.id.buttonLink:
                Log.d(TAG, "Link Wifi is clicked");
                String ssid = editTextSSID.getText().toString();
                String password = editTextPassword.getText().toString();
                sendWifiConfigToCup(ssid, password);
                break;
            case R.id.buttonSubscribe:
                Log.d(TAG, "Subscribe button is clicked");
                //ToDo: ID can be get from linking cup
                MqttManager.getInstance().subscribe("WOPIN-ECFABC1A490F", 0);
                break;
            case R.id.buttonPublish:
                Log.d(TAG, "Publish button is clicked");
                MqttManager.getInstance().publish("WOPIN-ECFABC1A490F", 0, "hello".getBytes());
                break;
            default:
                break;
        }
    }

    public void start() {
        boolean b = MqttManager.getInstance().creatConnect(URL, username, password, clientId);
        Log.d(TAG, "Mqtt isConnected: " + b);
        handler.postDelayed(runnable, 10000);
    }

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            String currentSSID = wifiConnectionReceiver.getConnectionInfo();
            //ToDo: Check the current connected ID to be eqaul to the QrCode
            if (currentSSID.trim().equals("\"H2PoPo\"")) {
                Log.d(TAG, "Sending scanning request");
                MediaType mediaType = MediaType.parse("application/octet-stream");
                RequestBody body = RequestBody.create(mediaType, "scan:1\n");
                Request request = new Request.Builder()
                                     .url("http://172.16.0.123/wopin_wifi")
                                     .post(body)
                                     .build();

                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Request request, IOException e) {
                        Log.d(TAG, "Http call failure");
                        Log.e(TAG, e.getMessage());
                        start();
                    }

                    @Override
                    public void onResponse(Response response) throws IOException {
                        Log.d(TAG, response.toString());
                        final String jsonString = "[" + response.body().string() + "]";

                        MainActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                JSONArray jsonResponse;
                                try {
                                    jsonResponse = new JSONArray(jsonString);
                                    for (int i = 0; i < jsonResponse.length(); i++) {
                                        Log.d(TAG, jsonResponse.getString(i));
                                        JSONObject jobj = new JSONObject(jsonResponse.getString(i));
                                        String ssid = jobj.getString("essid");
                                        adapter.add(ssid);
                                    }
                                    adapter.notifyDataSetChanged();
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    }
                });
            } else {
                start();
            }
        }
    };

    private void sendWifiConfigToCup(String ssid, String password) {
        String currentSSID = wifiConnectionReceiver.getConnectionInfo();
        if (currentSSID.trim().equals("\"H2PoPo\"")) {
            Log.d(TAG, "Sending scanning request");
            MediaType mediaType = MediaType.parse("application/octet-stream");
            //Be careful of the space after  ":", this is necessary....
            RequestBody body = RequestBody.create(mediaType, "ssid: " + ssid + "\n" + "password: " + password);
            Request request = new Request.Builder()
                    .url("http://172.16.0.123/wopin_wifi")
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Request request, IOException e) {
                    Log.d(TAG, "Http call failure");
                    Log.e(TAG, e.getMessage());
                    start();
                }

                @Override
                public void onResponse(Response response) throws IOException {
                    Log.d(TAG, response.toString());
                    final String jsonString = response.body().string();
                    Log.d(TAG, jsonString);
                }
            });
        } else {
            Log.d(TAG, "Cup is not connected");
        }
    }

    private void sendHydroOnOffCommand(String deviceId, boolean isOn, String timeString) {
        if (isOn) {
            String cmd = "021".concat(timeString);
            MqttManager.getInstance().publish(deviceId, 0, cmd.getBytes());
        } else {
            MqttManager.getInstance().publish(deviceId, 0, "02000000".getBytes());
        }
    }

    private void sendCleanOnOffCommand(String deviceId, boolean isOn) {
        if (isOn) {
            MqttManager.getInstance().publish(deviceId, 0, "031".getBytes());
        } else {
            MqttManager.getInstance().publish(deviceId, 0, "030".getBytes());
        }
    }

    private void sendLEDCommand(String deviceId, int r, int g, int b) {
        String rgbString = String.format("01%02X%02X%02X", r, g, b);
        MqttManager.getInstance().publish(deviceId, 0, rgbString.getBytes());
    }

}
