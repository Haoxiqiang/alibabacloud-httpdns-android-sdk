package com.alibaba.sdk.android.httpdns.track;

import android.util.Log;

import com.alibaba.sdk.android.httpdns.net.SpConstants;
import com.alibaba.sdk.android.httpdns.net.SpStatusManager;

import java.util.Random;

/**
 * Created by tomchen on 2018/8/6
 *
 * @author lianke
 */
public class SessionTrackMgr {

    private static final String TAG = "SessionTrackMgr";

    private String mSid;

    private SessionTrackMgr() {
        try {
            final String sampleAlphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
            final Random random = new Random();
            char[] buf = new char[12];
            for (int i = 0; i < 12; i++)
                buf[i] = sampleAlphabet.charAt(random.nextInt(sampleAlphabet.length()));
            mSid = new String(buf);
        } catch (Exception e) {
            Log.d(TAG, e.getMessage(), e);
        }
    }

    public static SessionTrackMgr getInstance() {
        return InstanceHolder.INSTANCE;
    }

    private final static class InstanceHolder {
        private final static SessionTrackMgr INSTANCE = new SessionTrackMgr();
    }


    public String getNetType() {
        final int networkType = SpStatusManager.getInstance().getNetworkType();

        switch (networkType) {
            case SpConstants.NETWORK_TYPE_UNCONNECTED:
            case SpConstants.NETWORK_TYPE_UNKNOWN:
                return SessionTrackConfig.TYPE_UNKNOWN;
            case SpConstants.NETWORK_TYPE_WIFI:
                return SessionTrackConfig.TYPE_WIFI;
            case SpConstants.NETWORK_TYPE_2G:
                return SessionTrackConfig.TYPE_2G;
            case SpConstants.NETWORK_TYPE_3G:
                return SessionTrackConfig.TYPE_3G;
            case SpConstants.NETWORK_TYPE_4G:
                return SessionTrackConfig.TYPE_4G;
        }

        return SessionTrackConfig.TYPE_UNKNOWN;
    }

    public String getBssid() {
        return SpStatusManager.getInstance().getBssid();
    }

    public String getSessionId() {
        return mSid;
    }
}
