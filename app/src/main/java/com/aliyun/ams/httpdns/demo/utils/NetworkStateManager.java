package com.aliyun.ams.httpdns.demo.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.alibaba.sdk.android.httpdns.HttpDnsSettings;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 监听网络变化
 */
public class NetworkStateManager implements HttpDnsSettings.NetworkChecker, INetworkHelper {

    private Context context;

    private static class Holder {
        private static final NetworkStateManager instance = new NetworkStateManager();
    }

    public static NetworkStateManager getInstance() {
        return Holder.instance;
    }

    private NetworkStateManager() {
    }

    private ExecutorService service = Executors.newSingleThreadExecutor();
    private int type = -1;
    private int subType = -1;


    public void init(Context ctx) {
        if (ctx == null) {
            throw new IllegalStateException("Context can't be null");
        }
        if (this.context != null) {
            // inited
            return;
        }
        this.context = ctx.getApplicationContext();
        Inet64Util.init(this);
        BroadcastReceiver bcReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(final Context context, final Intent intent) {
                service.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (isInitialStickyBroadcast()) { // no need to handle initial sticky broadcast
                                return;
                            }
                            String action = intent.getAction();
                            if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action)) {
                                updateNetworkType(context);
                                Inet64Util.startIpStackDetect();
                                Inet64Util.startIpStackDetectByHost();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        };

        try {
            IntentFilter mFilter = new IntentFilter();
            mFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
            context.registerReceiver(bcReceiver, mFilter);
        } catch (Exception e) {
            e.printStackTrace();
        }
        service.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    updateNetworkType(context);
                    Inet64Util.startIpStackDetect();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void updateNetworkType(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = connectivityManager.getActiveNetworkInfo();
        if (info != null && info.isAvailable() && info.isConnected()) {
            type = info.getType();
            subType = info.getSubtype();
        } else {
            type = -1;
            subType = -1;
        }
    }


    @Override
    public boolean isIpv6Only() {
        return Inet64Util.isIPv6OnlyNetworkByHost();
    }


    @Override
    public String generateCurrentNetworkId() {
        return type + "_" + subType;
    }

    @Override
    public boolean isWifi() {
        return type == ConnectivityManager.TYPE_WIFI;
    }

    @Override
    public boolean isMobile() {
        return type == ConnectivityManager.TYPE_MOBILE;
    }
}
