package com.alibaba.sdk.android.httpdns.net;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Process;

import com.alibaba.sdk.android.httpdns.log.HttpDnsLog;
import com.alibaba.sdk.android.httpdns.utils.ThreadUtil;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;


public class NetworkStateManager {
    public static final String NONE_NETWORK = "None_Network";
    private Context context;
    private String lastConnectedNetwork = NONE_NETWORK;
    private ArrayList<OnNetworkChange> listeners = new ArrayList<>();

    private ExecutorService worker = ThreadUtil.createSingleThreadService("network");

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
            public void onReceive(final Context context, final Intent intent) {
                try {
                    worker.execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                if (isInitialStickyBroadcast()) { // no need to handle initial sticky broadcast
                                    return;
                                }
                                String action = intent.getAction();
                                if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action) && hasNetInfoPermission(context)) {
                                    String currentNetwork = detectCurrentNetwork();
                                    HttpDnsNetworkDetector.getInstance().cleanCache(!currentNetwork.equals(NONE_NETWORK));
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
                    });
                } catch (Exception e) {
                }
            }
        };

        try {
            if (hasNetInfoPermission(context)) {
                IntentFilter mFilter = new IntentFilter();
                mFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
                context.registerReceiver(bcReceiver, mFilter);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String detectCurrentNetwork() {
        try {
            ConnectivityManager connectivityManager = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo info = connectivityManager.getActiveNetworkInfo();
            if (info != null && info.isAvailable() && info.isConnected()) {
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
    }

    private static boolean hasNetInfoPermission(Context context) {
        try {
            return checkSelfPermission(context, Manifest.permission.ACCESS_NETWORK_STATE) == PackageManager.PERMISSION_GRANTED;
        } catch (Throwable e) {
            HttpDnsLog.w("check network info permission fail", e);
        }
        return false;
    }


    private static int checkSelfPermission(Context context, String permission) {
        return context.checkPermission(permission, Process.myPid(), Process.myUid());
    }
}
