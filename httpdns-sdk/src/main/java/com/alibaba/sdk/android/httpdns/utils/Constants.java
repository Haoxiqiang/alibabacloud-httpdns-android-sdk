package com.alibaba.sdk.android.httpdns.utils;

import com.alibaba.sdk.android.httpdns.HTTPDNSResult;

import java.util.HashMap;

/**
 * @author zonglin.nzl
 * @date 11/4/21
 */
public class Constants {

    public static final String[] NO_IPS = new String[0];
    public static final HashMap<String, String> NO_EXTRA = new HashMap<String, String>();

    public static final HTTPDNSResult EMPTY = new HTTPDNSResult("", NO_IPS, NO_IPS, NO_EXTRA, false, false);

}
