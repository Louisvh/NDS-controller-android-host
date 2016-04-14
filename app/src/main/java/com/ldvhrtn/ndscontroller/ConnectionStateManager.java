package com.ldvhrtn.ndscontroller;

import java.lang.reflect.Method;
import java.util.List;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.text.format.Formatter;
import android.util.Log;

public class ConnectionStateManager {
    private final String TAG = "Con State Manager";
    private final WifiManager mWifiManager;
    private WifiConfiguration mConfig;
    private Method set_tether_enabled;
    private Method get_tether_configuration;
    private Method get_tether_state;

    public ConnectionStateManager(Context context) throws SecurityException, NoSuchMethodException {
        if (context == null) throw new NullPointerException();
        mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        set_tether_enabled = mWifiManager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class,boolean.class);
        get_tether_configuration = mWifiManager.getClass().getMethod("getWifiApConfiguration",null);
        get_tether_state = mWifiManager.getClass().getMethod("isWifiApEnabled");
    }
    // sets the tethering state to boolean state
    public boolean setWifiApState(WifiConfiguration config, boolean state) {
        if (config == null) throw new NullPointerException();
        try {
            if (state) {
                mWifiManager.setWifiEnabled(!state);
            }
            return (Boolean) set_tether_enabled.invoke(mWifiManager, config, state);
        } catch (Exception e) {
            Log.e(TAG, "", e);
            return false;
        }
    }
    // presents the current tethering configuration
    public WifiConfiguration getWifiApConfiguration() {
        try{
            return (WifiConfiguration) get_tether_configuration.invoke(mWifiManager, null);
        }
        catch(Exception e)
        {
            return null;
        }
    }
    // checks whether wifi is connected
    public boolean wifi_is_connected() {
        WifiInfo mWifiInfo = mWifiManager.getConnectionInfo();
        return (mWifiInfo.getSSID() != null);
    }
    public String wifi_ip() {
        WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
        int m_ip = wifiInfo.getIpAddress();
        return Formatter.formatIpAddress(m_ip);
    }
    // checks current tethering state
    public boolean get_tether_state() {
        try {
            boolean state = (Boolean) get_tether_state.invoke(mWifiManager);
            boolean state1 = state;
            return state;
        } catch (Exception e) {
            Log.e(TAG, e.toString(), e);
            Log.e(TAG, e.toString());
            return false;
        }
    }
    // returns false if non-trivial security options are active
    public boolean connection_nds_compatible() {
        boolean trivial_connection = false;
        WifiConfiguration mConfig = getWifiApConfiguration();
        if(mWifiManager.isWifiEnabled()) {
            List<ScanResult> networkList = mWifiManager.getScanResults();

            WifiInfo mWifiInfo = mWifiManager.getConnectionInfo();
            String currentSSID = mWifiInfo.getSSID();

            if (networkList != null) {
                for (ScanResult network : networkList) {
                    if (currentSSID.equals('"'+network.SSID+'"')){
                        String Capabilities =  network.capabilities;
                        Log.d (TAG, network.SSID + " capabilities : " + Capabilities);

                        if (Capabilities.contains("WPA2") || Capabilities.contains("WPA")
                                || Capabilities.contains("WEP")) {
                            trivial_connection = false;
                        } else {
                            trivial_connection = true;
                        }
                    }
                }
            }
        } else {
            boolean WPA_disabled = mConfig.preSharedKey == null;
            String WEP_key = mConfig.wepKeys[mConfig.wepTxKeyIndex];
            boolean WEP_trivial;
            if (WEP_key == null) {
                WEP_trivial = true;
            } else {
                //this is incorrect, length isn't known (TODO, maybe)
                //WEP_trivial = WEP_key.length() == 5 || WEP_key.length() == 13;
                WEP_trivial = false;
            }
            trivial_connection = WPA_disabled && WEP_trivial;
        }
        return trivial_connection;
    }
}