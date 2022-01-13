package com.alibaba.sdk.android.httpdns.utils;

import com.alibaba.sdk.android.httpdns.HTTPDNSResult;
import com.alibaba.sdk.android.httpdns.interpret.SniffException;

import java.util.HashMap;

/**
 * @author zonglin.nzl
 * @date 11/4/21
 */
public class Constants {

    // 国内
    public static final String REGION_DEFAULT = "";
    // 香港
    public static final String REGION_HK = "hk";
    // 新加坡
    public static final String REGION_SG = "sg";

    public static final int NO_PORT = -1;
    public static final int[] NO_PORTS = null;

    public static final String[] NO_IPS = new String[0];
    public static final HashMap<String, String> NO_EXTRA = new HashMap<String, String>();

    public static final HTTPDNSResult EMPTY = new HTTPDNSResult("", NO_IPS, NO_IPS, NO_EXTRA, false, false);

    public static final SniffException sniff_too_often = new SniffException("sniff too often");

    public static final Exception region_not_match = new Exception("region not match");
}
