package com.alibaba.sdk.android.httpdns.cache;

import android.text.TextUtils;
import android.util.Log;

import java.text.SimpleDateFormat;

/**
 * Created by tomchen on 2017/5/4.
 */

public class DBCacheUtils {

    private static final SimpleDateFormat DATA_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    static String formatData(String time) {
        try {
            return DATA_FORMAT.format(getLongSafely(time) * 1000L);
        } catch (Exception e) {
            if (DBConfig.DEBUG) {
                Log.d(DBConfig.TAG, e.getMessage(), e);
            }
            return DATA_FORMAT.format(System.currentTimeMillis());
        }
    }

    static String parseData(String time) {
        try {
            return String.valueOf(DATA_FORMAT.parse(time).getTime() / 1000L);
        } catch (Exception e) {
            if (DBConfig.DEBUG) {
                Log.d(DBConfig.TAG, e.getMessage(), e);
            }
            return String.valueOf(System.currentTimeMillis() / 1000L);
        }
    }

    public static long getLongSafely(String longStr) {
        if (TextUtils.isEmpty(longStr)) {
            return 0L;
        }
        try {
            return Long.valueOf(longStr);
        } catch (Exception e) {
            return 0L;
        }
    }
}
