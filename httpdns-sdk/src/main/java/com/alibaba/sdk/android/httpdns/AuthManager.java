package com.alibaba.sdk.android.httpdns;

import android.text.TextUtils;
import android.util.Log;

/**
 * Created by tomchen on 2017/6/30.
 */

class AuthManager {

    private static final String TAG = "AuthManager";

    private static final boolean DEBUG = false;

    // 过期时间默认10分钟
    private static final long EXPIRATION_TIME = 10 * 60L;

    private static String sSecretKey;

    private static long sOffset;

    static void setSecretKey(String secretKey) {
        sSecretKey = secretKey;
    }

    static void setAuthCurrentTime(long time) {
        sOffset = time - (System.currentTimeMillis() / 1000);
    }

    static boolean isEnable() {
        return !TextUtils.isEmpty(sSecretKey);
    }

    static String getTimestamp() {
        return String.valueOf(System.currentTimeMillis() / 1000 + sOffset + EXPIRATION_TIME);
    }

    static String getSign(final String host, final String timestamp) {
        String sign = "";

        if (!HttpDnsUtil.isAHost(host)) {
            return sign;
        }

        // sign = md5sum( host-secret-timestamp )
        StringBuilder params = new StringBuilder();
        params.append(host);
        params.append("-");
        params.append(sSecretKey);
        params.append("-");
        params.append(timestamp);

        try {
            sign = HttpDnsUtil.getMD5String(params.toString());

            if (DEBUG) {
                Log.d(TAG, "[AuthManager] getSign params: " + params.toString() + " sign: " + sign);
            }
        } catch (Exception e) {
            if (DEBUG) {
                Log.d(TAG, e.getMessage(), e);
            }
        }

        return sign;
    }
}
