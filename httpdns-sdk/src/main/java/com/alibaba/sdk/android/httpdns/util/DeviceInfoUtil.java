package com.alibaba.sdk.android.httpdns.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.alibaba.sdk.android.httpdns.HttpDnsLog;

/**
 * Created by liyazhou on 2017/12/26.
 */

public class DeviceInfoUtil {
    public static final String CONNECTIVITY_NONE_NETWORK = "none_network";
    public static final String CONNECTIVITY_UNKOWN_NETWORK = "unknown_network";
    public static final String CONNECTIVITY_NETWORK_AVAILABLE = "network_available";

    public static String detectNetwork(Context context) {
        if (context != null) {
            try {
                ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                if (cm != null) {
                    NetworkInfo[] infos = cm.getAllNetworkInfo();
                    if (infos != null) {
                        for (NetworkInfo ni : infos) {
                            if (ni.isConnected()) {
                                return ni.getTypeName();
                            }
                        }
                        return CONNECTIVITY_NONE_NETWORK;
                    }
                }
            } catch (Throwable throwable) {
                HttpDnsLog.Loge("get connectivity failed." + throwable.toString());
            }

            return CONNECTIVITY_UNKOWN_NETWORK;
        } else {
            return CONNECTIVITY_UNKOWN_NETWORK;
        }
    }
}
