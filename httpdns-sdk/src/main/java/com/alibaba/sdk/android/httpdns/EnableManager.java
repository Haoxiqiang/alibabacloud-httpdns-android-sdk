package com.alibaba.sdk.android.httpdns;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by tomchen on 2018/12/20
 *
 * @author lianke
 */
public class EnableManager {

    private static boolean isEnable = true;

    private static boolean isSafe = true;

    private static SharedPreferences sp;

    private static final String SPF_NAME = "httpdns_config_enable";

    private static final String SPF_KEY_ENABLE = "key_enable";

    static void init(Context context) {
        if (context != null) {
            sp = context.getSharedPreferences(SPF_NAME, Context.MODE_PRIVATE);
            if (sp != null) {
                isEnable = sp.getBoolean(SPF_KEY_ENABLE, true);
            }
        }
    }

    public static boolean isEnable() {
        return isEnable && isSafe;
    }

    public static void enable(boolean enable) {
        isEnable = enable;
        if (sp != null) {
            SharedPreferences.Editor editor = sp.edit();
            editor.putBoolean(SPF_KEY_ENABLE, enable);
            editor.apply();
        }
    }

    public static void safe(boolean safe) {
        isSafe = safe;
    }
}
