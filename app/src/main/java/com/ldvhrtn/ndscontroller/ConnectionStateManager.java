package com.ldvhrtn.ndscontroller;

import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.List;

import android.content.Context;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.text.format.Formatter;
import android.util.Log;

import androidx.core.app.ActivityCompat;

public class ConnectionStateManager {
    private final String TAG = "Con State Manager";
    private final WifiManager mWifiManager;

    public ConnectionStateManager(Context context) throws SecurityException {
        if (context == null) throw new NullPointerException();
        mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
    }

    public String get_ip_address(){
        WifiInfo mWifiInfo = mWifiManager.getConnectionInfo();
        int ip = mWifiInfo.getIpAddress();
        String ip_str;
        if (ip == 0) {
            ip_str = getIpAddressAP();
            if (ip_str.length() < 1) {
                ip_str = "0.0.0.0";
            }
        } else {
            ip_str = String.format("%d.%d.%d.%d", (ip & 0xff), (ip >> 8 & 0xff), (ip >> 16 & 0xff),
                    (ip >> 24 & 0xff));
        }
        return ip_str;
    }
    // checks whether wifi is connected
    public boolean wifi_is_connected() {
        WifiInfo mWifiInfo = mWifiManager.getConnectionInfo();
        int ip = mWifiInfo.getIpAddress();
        String m_ip_str = String.format("%d.%d.%d.%d", (ip & 0xff), (ip >> 8 & 0xff), (ip >> 16 & 0xff),
                (ip >> 24 & 0xff));
        Log.d(TAG, m_ip_str);
        return (mWifiInfo.getIpAddress() != 0);
    }

    public boolean get_tether_state() {
        // can't easily access AP details at higher API levels anymore, just trust this I guess?
        return (get_ip_address().length() > 7) && !wifi_is_connected();
    }

    // returns false if non-trivial security options are active
    public boolean connection_nds_compatible() {
        if (mWifiManager.isWifiEnabled()) {
            WifiInfo mWifiInfo = mWifiManager.getConnectionInfo();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                return mWifiInfo.getCurrentSecurityType() < 2;
            } else {
                // TODO check how to do this at lower api levels?
                return false;
            }
        }
        return false;
    }

    private String getIpAddressAP() {
        String ip = "";
        try {
            Enumeration<NetworkInterface> enumNetworkInterfaces = NetworkInterface
                    .getNetworkInterfaces();
            while (enumNetworkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = enumNetworkInterfaces
                        .nextElement();
                Enumeration<InetAddress> enumInetAddress = networkInterface
                        .getInetAddresses();
                while (enumInetAddress.hasMoreElements()) {
                    InetAddress inetAddress = enumInetAddress.nextElement();

                    if (inetAddress.isSiteLocalAddress()) {
                        ip += inetAddress.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            ip += "Something Wrong! " + e + "\n";
        }
        return ip;
    }
}