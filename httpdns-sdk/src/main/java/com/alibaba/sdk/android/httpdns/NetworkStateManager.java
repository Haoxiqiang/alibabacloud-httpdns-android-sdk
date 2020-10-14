package com.alibaba.sdk.android.httpdns;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.alibaba.sdk.android.httpdns.cache.DBCacheManager;

import java.util.ArrayList;

public class NetworkStateManager {

    static boolean isPreResolveAfterNetworkChangedEnabled = false;
    private static Context context;
    private static String networkMode;
    private static final String NONE_NETWORK = "None_Network";

    public static void setContext(Context ctx) {
        if (ctx == null) {
            throw new IllegalStateException("Context can't be null");
        }
        NetworkStateManager.context = ctx;
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
                        DBCacheManager.updateSp(context);
                        String currentNetwork = detectCurrentNetwork();
                        if (currentNetwork != NONE_NETWORK && !currentNetwork.equalsIgnoreCase(networkMode)) {
                            HttpDnsLog.Logd("[BroadcastReceiver.onReceive] - Network state changed");
                            ArrayList<String> hostList = HostManager.getInstance().getAllHost();
                            HostManager.getInstance().clear();
                            HostManager.getInstance().updateFromDBCache();
                            if (isPreResolveAfterNetworkChangedEnabled && HttpDns.instance != null) {
                                HttpDnsLog.Logd("[BroadcastReceiver.onReceive] - refresh host");
                                HttpDns.instance.setPreResolveHosts(hostList);
                            }
                        }
                        networkMode = currentNetwork;
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

    }

    private static String detectCurrentNetwork() {
        try {
            ConnectivityManager connectivityManager = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo info = connectivityManager.getActiveNetworkInfo();
            if (info != null && info.isAvailable()) {
                String name = info.getTypeName();
                HttpDnsLog.Logd("[detectCurrentNetwork] - Network name:" + name + " subType name: " + info.getSubtypeName());
                return name == null ? NONE_NETWORK : name;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return NONE_NETWORK;
    }


}
