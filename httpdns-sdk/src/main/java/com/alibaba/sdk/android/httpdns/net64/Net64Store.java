package com.alibaba.sdk.android.httpdns.net64;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by tomchen on 2018/8/27
 *
 * @author lianke
 */
final class Net64Store {

    private final static String SPF_NAME = "n_6_s";

    private final static String KEY_SERVICE_BLOCKED_STATUS = "k_s_b_s";

    private final static String KEY_SERVICE_BLOCKED_TIME = "k_s_b_t";

    void saveBlockedStatus(Context context, boolean blocked) {
        if (context != null) {
            final SharedPreferences spf = context.getSharedPreferences(SPF_NAME, Context.MODE_PRIVATE);
            final SharedPreferences.Editor editor = spf.edit();
            editor.putBoolean(KEY_SERVICE_BLOCKED_STATUS, blocked);
            editor.apply();
        }
    }

    boolean loadBlockedStatus(Context context) {
        return context == null
                || context.getSharedPreferences(SPF_NAME, Context.MODE_PRIVATE).getBoolean(KEY_SERVICE_BLOCKED_STATUS, false);
    }

    void saveLastedBlockedTimeMillis(Context context, long time) {
        if (context != null) {
            final SharedPreferences spf = context.getSharedPreferences(SPF_NAME, Context.MODE_PRIVATE);
            final SharedPreferences.Editor editor = spf.edit();
            editor.putLong(KEY_SERVICE_BLOCKED_TIME, time);
            editor.apply();
        }
    }

    long loadLastedBlockedTimeMillis(Context context) {
        if (context != null) {
            return context.getSharedPreferences(SPF_NAME, Context.MODE_PRIVATE).getLong(KEY_SERVICE_BLOCKED_TIME, 0L);
        } else {
            return -1L;
        }
    }
}
