package com.alibaba.sdk.android.httpdns.net64;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.alibaba.sdk.android.httpdns.net.SpConstants;
import com.alibaba.sdk.android.httpdns.net.SpStatusManager;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by tomchen on 2018/8/27
 *
 * @author lianke
 */
public class Net64Mgr implements Net64Service {

    private static final String TAG = "Net64Mgr";

    private final static int IP_STACK_UNKNOWN = 0;

    private final static int IPV4_ONLY = 1;

    private final static int IPV6_ONLY = 2;

    private final static int IP_DUAL_STACK = 3;

    private final static String INTERFACE_WIFI = "wlan";

    private final static String INTERFACE_MOBILE = "rmnet";

    private final static long SERVICE_BLOCKED_MAX_TIME_MILLIS = 12 * 60 * 60 * 1000L; // 12h

    private int mIpStack;

    private boolean mIpv6Enable;

    private volatile boolean mServiceEnable;

    private volatile boolean mServiceBlocked;

    private volatile boolean mServiceWorking;

    private Net64Store mStore = new Net64Store();

    private ConcurrentHashMap<String, List<String>> mHost2ipv6List = new ConcurrentHashMap<>();

    private Net64Mgr() {
    }

    public static Net64Mgr getInstance() {
        return Net64Mgr.InstanceHolder.INSTANCE;
    }

    public boolean isEnable() {
        return mIpv6Enable;
    }

    public boolean isServiceWorking() {
        return mServiceWorking;
    }

    // block 12h
    private void updateServiceBlockStatus(Context context) {
        mServiceBlocked = mStore.loadBlockedStatus(context);
        Log.d(TAG, "[updateServiceBlockStatus] block_status loaded: " + mServiceBlocked);
        if (mServiceBlocked) {
            final long lastBlockedTimeMillis = mStore.loadLastedBlockedTimeMillis(context);
            final long currentTimeMillis = System.currentTimeMillis();
            if (lastBlockedTimeMillis > 0
                    && currentTimeMillis - lastBlockedTimeMillis > SERVICE_BLOCKED_MAX_TIME_MILLIS) {
                mServiceBlocked = false;
            }
        }
        Log.d(TAG, "[updateServiceBlockStatus] block_status updated: " + mServiceBlocked);
    }

    public void setServiceBlocked(Context context, boolean blocked) {
        mServiceBlocked = blocked;
        mStore.saveBlockedStatus(context, blocked);
        if (blocked) {
            mStore.saveLastedBlockedTimeMillis(context, System.currentTimeMillis());
        }
    }

    public void put(String host, List<String> ipv6List) {
        mHost2ipv6List.put(host, ipv6List);
    }

    public List<String> get(String host) {
        return mHost2ipv6List.get(host);
    }

    private final static class InstanceHolder {
        private final static Net64Mgr INSTANCE = new Net64Mgr();
    }

    private void updateIpStack() throws SocketException {
        boolean hasV6 = false;
        boolean hasV4 = false;

        Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
        for (NetworkInterface anInterface : Collections.list(networkInterfaces)) {
            if (isInvalid(SpStatusManager.getInstance().getNetworkType(), anInterface.getDisplayName())) {
                for (InterfaceAddress addr : anInterface.getInterfaceAddresses()) {
                    InetAddress a = addr.getAddress();
                    if (a instanceof Inet6Address) {
                        if (isInvalid(a)) {
                            continue;
                        }
                        hasV6 = true;
                    } else if (a instanceof Inet4Address) {
                        if (isInvalid(a) && !a.getHostAddress().startsWith("192.168.43.")) { //过滤掉分享wifi热点这种case
                            continue;
                        }
                        hasV4 = true;
                    }
                }
            }
        }

        mIpStack = hasV6 ? (hasV4 ? IP_DUAL_STACK : IPV6_ONLY) : (hasV4 ? IPV4_ONLY : IP_STACK_UNKNOWN);

        Log.d(TAG, "[stack]: " + mIpStack);
    }

    private boolean isInvalid(int networkType, String displayName) {
        if (TextUtils.isEmpty(displayName)) {
            return false;
        }
        switch (networkType) {
            case SpConstants.NETWORK_TYPE_WIFI:
                if (displayName.startsWith(INTERFACE_WIFI)) {
                    return true;
                }
                break;
            case SpConstants.NETWORK_TYPE_2G:
            case SpConstants.NETWORK_TYPE_3G:
            case SpConstants.NETWORK_TYPE_4G:
                if (displayName.startsWith(INTERFACE_MOBILE)) {
                    return true;
                }
                break;
        }
        return false;
    }

    private boolean isInvalid(InetAddress address) {
        return address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isAnyLocalAddress();
    }

    @Override
    public void enableIPv6(boolean enable) {
        mIpv6Enable = enable;
    }

    @Override
    public String getIPv6ByHostAsync(String host) {
        if (!mIpv6Enable) {
            return null;
        }

        List<String> ipv6List = mHost2ipv6List.get(host);
        if (ipv6List != null && ipv6List.size() > 0) {
            return ipv6List.get(0);
        } else {
            return null;
        }
    }
}
