package com.onwardsmg.wopinwifidemo;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiInfo;
import android.support.annotation.NonNull;
import android.util.Log;

/**
 * Created by laikwoktai on 2/9/2018.
 */

public class WifiConnectionReceiver extends BroadcastReceiver {

    static final String TAG = "WifiConnectionReceiver";

    /**
     * Notifies the receiver to turn wifi on
     */
    private static final String ACTION_WIFI_ON = "android.intent.action.WIFI_ON";

    /**
     * Notifies the receiver to turn wifi off
     */
    private static final String ACTION_WIFI_OFF = "android.intent.action.WIFI_OFF";

    /**
     * Notifies the receiver to connect to a specified wifi
     */
    private static final String ACTION_CONNECT_TO_WIFI = "android.intent.action.CONNECT_TO_WIFI";

    private WifiManager wifiManager;

    private Context context;

    private int netId = -9999;

    public WifiConnectionReceiver(Context applicationContext) {
        context = applicationContext;
        wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
    }

    public void onReceive(Context c, Intent intent) {
        Log.d(TAG, "onReceive() called with: intent = [" + intent + "]");

        wifiManager = (WifiManager) c.getSystemService(Context.WIFI_SERVICE);

        final String action = intent.getAction();

        if (!isTextNullOrEmpty(action)) {
            switch (action) {
                case ACTION_WIFI_ON:
                    // Turns wifi on
                    wifiManager.setWifiEnabled(true);
                    break;
                case ACTION_WIFI_OFF:
                    // Turns wifi off
                    wifiManager.setWifiEnabled(false);
                    break;
                case ACTION_CONNECT_TO_WIFI:
                    // Connects to a specific wifi network
                    final String networkSSID = intent.getStringExtra("ssid");
                    final String networkPassword = intent.getStringExtra("password");

                    if (!isTextNullOrEmpty(networkSSID) && !isTextNullOrEmpty(networkPassword)) {
                        connectToWifi(networkSSID, networkPassword);
                    } else {
                        Log.e(TAG, "onReceive: cannot use " + ACTION_CONNECT_TO_WIFI +
                                "without passing in a proper wifi SSID and password.");
                    }
                    break;
            }
        }
    }

    private boolean isTextNullOrEmpty(final String text) {
        return text != null && !text.isEmpty();
    }

    /**
     * Connect to the specified wifi network.
     *
     * @param networkSSID     - The wifi network SSID
     * @param networkPassword - the wifi password
     */
    public void connectToWifi(final String networkSSID, final String networkPassword) {
        if (netId > 0)
            wifiManager.disableNetwork(netId);
        if (!wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(true);
        }

        WifiConfiguration conf = new WifiConfiguration();
        conf.SSID = String.format("\"%s\"", networkSSID);
        conf.preSharedKey = String.format("\"%s\"", networkPassword);

        netId = wifiManager.addNetwork(conf);
        Log.d(TAG, "netId " + netId);
        wifiManager.disconnect();
        wifiManager.enableNetwork(netId, true);
        wifiManager.reconnect();
    }

    public String getConnectionInfo() {
        WifiInfo info  = wifiManager.getConnectionInfo();
        Log.d(TAG,"Current Connected SSID : " + info.getSSID());
        String ssid = info.getSSID();
        return ssid;
    }

    @NonNull
    public static IntentFilter getIntentFilterForWifiConnectionReceiver() {
        final IntentFilter randomIntentFilter = new IntentFilter(ACTION_WIFI_ON);
        randomIntentFilter.addAction(ACTION_WIFI_OFF);
        randomIntentFilter.addAction(ACTION_CONNECT_TO_WIFI);
        return randomIntentFilter;
    }


}
