package com.alibaba.sdk.android.httpdns.net;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import com.alibaba.sdk.android.httpdns.BuildConfig;

/**
 * Created by tomchen on 2017/4/26.
 */

public class SpStatusManager {

    private static final String TAG = "SpStatusManager";

    private static final boolean DEBUG = BuildConfig.DEBUG;

    private static final String SP_INIT = "UNKNOWN";

    private static final String BSSID_INIT = "UNKNOWN";

    // network type
    private int mNetworkType = SpConstants.NETWORK_TYPE_UNKNOWN;

    // ssid
    private String mSP = SP_INIT;

    // bssid
    private String mBssid = BSSID_INIT;

    private SpStatusManager() {
    }

    public static SpStatusManager getInstance() {
        return InstanceHolder.INSTANCE;
    }

    private final static class InstanceHolder {
        private final static SpStatusManager INSTANCE = new SpStatusManager();
    }

    public boolean isWifi() {
        return mNetworkType == SpConstants.NETWORK_TYPE_WIFI;
    }

    public String getSP() {
        return mSP;
    }

    public String getBssid() {
        return mBssid;
    }

    public int getNetworkType() {
        return mNetworkType;
    }

    public void update(final Context context) {
        handleUpdate(context);
    }

    private void handleUpdate(Context context) {
        int networkType = getNetworkType(context);
        mNetworkType = networkType;

        switch (networkType) {
            case SpConstants.NETWORK_TYPE_UNCONNECTED:
            case SpConstants.NETWORK_TYPE_UNKNOWN:
                break;
            case SpConstants.NETWORK_TYPE_WIFI:
                mSP = getSSID(context);
                mBssid = getBSID(context);
                break;
            case SpConstants.NETWORK_TYPE_2G:
            case SpConstants.NETWORK_TYPE_3G:
            case SpConstants.NETWORK_TYPE_4G:
                mSP = getCellSP(context);
                break;
        }

        if (TextUtils.isEmpty(mSP)) {
            mSP = SP_INIT;
        }

        if (DEBUG) {
            Log.d(TAG, "[SpStatusManager] update mSP: " + mSP);
        }
    }

    private String getCellSP(Context context) {
        int code = SpConstants.SP_UNKNOWN;

        try {
            TelephonyManager telManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            String operator = telManager.getSimOperator();
            if (!TextUtils.isEmpty(operator)) {
                return operator;
            }
        } catch (Throwable e) {
            if (DEBUG) {
                Log.d(TAG, e.getMessage(), e);
            }
        }

        return String.valueOf(code);
    }

    // getWifiSp;
    private String getSSID(Context context) {
        try {
            WifiManager wifiMgr = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            WifiInfo info = wifiMgr.getConnectionInfo();
            return info != null ? info.getSSID() : null;
        } catch (Throwable e) {
            if (DEBUG) {
                Log.e(TAG, e.getMessage(), e);
            }
            return null;
        }
    }

    private String getBSID(Context context) {
        try {
            WifiManager wifiMgr = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            WifiInfo info = wifiMgr.getConnectionInfo();
            return info != null ? info.getBSSID() : null;
        } catch (Throwable e) {
            if (DEBUG) {
                Log.e(TAG, e.getMessage(), e);
            }
            return null;
        }
    }

    private int getNetworkType(Context context) {
        try {

            ConnectivityManager connectivity = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivity == null) {
                return SpConstants.NETWORK_TYPE_UNKNOWN;
            }

            NetworkInfo activeNetInfo = connectivity.getActiveNetworkInfo();
            if (activeNetInfo == null) {
                return SpConstants.NETWORK_TYPE_UNCONNECTED;
            }

            if (!activeNetInfo.isAvailable() || !activeNetInfo.isConnected()) {
                return SpConstants.NETWORK_TYPE_UNCONNECTED;
            }

            if (activeNetInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                return SpConstants.NETWORK_TYPE_WIFI;

            } else if (activeNetInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
                switch (activeNetInfo.getSubtype()) {
                    case TelephonyManager.NETWORK_TYPE_CDMA:
                    case TelephonyManager.NETWORK_TYPE_IDEN:
                    case TelephonyManager.NETWORK_TYPE_1xRTT:
                    case TelephonyManager.NETWORK_TYPE_EDGE:
                    case TelephonyManager.NETWORK_TYPE_GPRS:
                        return SpConstants.NETWORK_TYPE_2G;
                    case TelephonyManager.NETWORK_TYPE_EVDO_0:
                    case TelephonyManager.NETWORK_TYPE_UMTS:
                    case TelephonyManager.NETWORK_TYPE_EVDO_A:
                    case TelephonyManager.NETWORK_TYPE_HSPA:
                    case TelephonyManager.NETWORK_TYPE_HSUPA:
                    case TelephonyManager.NETWORK_TYPE_HSDPA:
                    case TelephonyManager.NETWORK_TYPE_HSPAP:
                        return SpConstants.NETWORK_TYPE_3G;
                    case TelephonyManager.NETWORK_TYPE_LTE:
                        return SpConstants.NETWORK_TYPE_4G;
                    case TelephonyManager.NETWORK_TYPE_UNKNOWN:
                    default:
                        return SpConstants.NETWORK_TYPE_UNKNOWN;
                }
            } else {
                return SpConstants.NETWORK_TYPE_UNKNOWN;
            }
        } catch (Exception e) {
            if (DEBUG) {
                Log.d(TAG, e.getMessage(), e);
            }
        }

        return SpConstants.NETWORK_TYPE_UNCONNECTED;
    }
}
