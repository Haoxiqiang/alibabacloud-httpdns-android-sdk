package com.alibaba.sdk.android.httpdns.net;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.support.v4.content.ContextCompat;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import com.alibaba.sdk.android.httpdns.log.HttpDnsLog;

import java.util.ArrayList;


public class NetworkStateManager {

    // 4g/3g/2g/wifi
    public final static String TYPE_4G = "4g";
    public final static String TYPE_3G = "3g";
    public final static String TYPE_2G = "2g";
    public final static String TYPE_WIFI = "wifi";
    public final static String TYPE_UNKNOWN = "unknown";

    public static final String NONE_NETWORK = "None_Network";
    private Context context;
    private String netType;
    private String sp;
    private String lastConnectedNetwork = NONE_NETWORK;
    private ArrayList<OnNetworkChange> listeners = new ArrayList<>();

    private static class Holder {
        private static final NetworkStateManager instance = new NetworkStateManager();
    }

    public static NetworkStateManager getInstance() {
        return Holder.instance;
    }

    private NetworkStateManager() {
    }


    public void init(Context ctx) {
        if (ctx == null) {
            throw new IllegalStateException("Context can't be null");
        }
        if (this.context != null) {
            // inited
            return;
        }
        this.context = ctx.getApplicationContext();
        BroadcastReceiver bcReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // onReceive()在主线程执行，SDK内部线程CrashHandler无法捕获，使用try/catch捕获
                try {
                    if (isInitialStickyBroadcast()) { // no need to handle initial sticky broadcast
                        return;
                    }
                    String action = intent.getAction();
                    if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action)) {
                        updateNetworkStatus(context);
                        String currentNetwork = detectCurrentNetwork();
                        if (!currentNetwork.equals(NONE_NETWORK) && !currentNetwork.equalsIgnoreCase(lastConnectedNetwork)) {
                            for (OnNetworkChange onNetworkChange : listeners) {
                                onNetworkChange.onNetworkChange(currentNetwork);
                            }
                        }
                        if (!currentNetwork.equals(NONE_NETWORK)) {
                            lastConnectedNetwork = currentNetwork;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        try {
            IntentFilter mFilter = new IntentFilter();
            mFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
            context.registerReceiver(bcReceiver, mFilter);
        } catch (Exception e) {
            e.printStackTrace();
        }

        updateNetworkStatus(this.context);
        HttpDnsLog.i("NetworkStateManager init " + getSp());
    }

    private String detectCurrentNetwork() {
        try {
            ConnectivityManager connectivityManager = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo info = connectivityManager.getActiveNetworkInfo();
            if (info != null && info.isAvailable()) {
                String name = info.getTypeName();
                HttpDnsLog.d("[detectCurrentNetwork] - Network name:" + name + " subType name: " + info.getSubtypeName());
                return name == null ? NONE_NETWORK : name;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return NONE_NETWORK;
    }

    public void addListener(OnNetworkChange change) {
        this.listeners.add(change);
    }


    public interface OnNetworkChange {
        void onNetworkChange(String networkType);
    }

    public void reset() {
        context = null;
        lastConnectedNetwork = NONE_NETWORK;
        listeners.clear();
        sp = TYPE_UNKNOWN;
        netType = TYPE_UNKNOWN;
    }

    private void updateNetworkStatus(Context context) {
        sp = TYPE_UNKNOWN;
        netType = TYPE_UNKNOWN;
        try {
            ConnectivityManager connectivity = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivity == null) {
                return;
            }

            NetworkInfo activeNetInfo = connectivity.getActiveNetworkInfo();
            if (activeNetInfo == null) {
                return;
            }

            if (!activeNetInfo.isAvailable() || !activeNetInfo.isConnected()) {
                return;
            }

            if (activeNetInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                sp = getSsid(context);
                netType = TYPE_WIFI;
                return;

            } else if (activeNetInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
                sp = getCellSP(context);
                switch (activeNetInfo.getSubtype()) {
                    case TelephonyManager.NETWORK_TYPE_CDMA:
                    case TelephonyManager.NETWORK_TYPE_IDEN:
                    case TelephonyManager.NETWORK_TYPE_1xRTT:
                    case TelephonyManager.NETWORK_TYPE_EDGE:
                    case TelephonyManager.NETWORK_TYPE_GPRS:
                        netType = TYPE_2G;
                        return;
                    case TelephonyManager.NETWORK_TYPE_EVDO_0:
                    case TelephonyManager.NETWORK_TYPE_UMTS:
                    case TelephonyManager.NETWORK_TYPE_EVDO_A:
                    case TelephonyManager.NETWORK_TYPE_HSPA:
                    case TelephonyManager.NETWORK_TYPE_HSUPA:
                    case TelephonyManager.NETWORK_TYPE_HSDPA:
                    case TelephonyManager.NETWORK_TYPE_HSPAP:
                        netType = TYPE_3G;
                        return;
                    case TelephonyManager.NETWORK_TYPE_LTE:
                        netType = TYPE_4G;
                        return;
                    case TelephonyManager.NETWORK_TYPE_UNKNOWN:
                    default:
                        return;
                }
            } else {
                return;
            }
        } catch (Throwable e) {
            HttpDnsLog.w("getNetType fail", e);
        } finally {
            if (sp == null) {
                sp = TYPE_UNKNOWN;
            }
            if (netType == null) {
                netType = TYPE_UNKNOWN;
            }
        }
        return;
    }

    private static String getSsid(Context context) {
        try {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                WifiManager wifiMgr = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                WifiInfo info = wifiMgr.getConnectionInfo();
                return info != null ? info.getSSID() : null;
            } else {
                return null;
            }
        } catch (Throwable e) {
            HttpDnsLog.w("get ssid fail", e);
            return null;
        }
    }

    private static String getCellSP(Context context) {
        try {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                TelephonyManager telManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                String operator = telManager.getSimOperator();
                if (!TextUtils.isEmpty(operator)) {
                    return operator;
                }
            }
        } catch (Throwable e) {
            HttpDnsLog.w("getCellSP fail", e);
        }
        return "UNKNOW";
    }

    public String getNetType() {
        return netType;
    }

    public String getSp() {
        return sp;
    }
}
