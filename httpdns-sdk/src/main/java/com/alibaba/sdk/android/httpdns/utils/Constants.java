package com.alibaba.sdk.android.httpdns.utils;

import com.alibaba.sdk.android.httpdns.HTTPDNSResult;
import com.alibaba.sdk.android.httpdns.interpret.SniffException;

import java.util.HashMap;

/**
 * @author zonglin.nzl
 * @date 11/4/21
 */
public class Constants {

    // region 定义
    // 国内
    public static final String REGION_DEFAULT = "";
    // 香港
    public static final String REGION_HK = "hk";
    // 新加坡
    public static final String REGION_SG = "sg";


    // 一些默认值 或者 单一场景定义
    public static final int NO_PORT = -1;
    public static final int[] NO_PORTS = null;

    public static final String[] NO_IPS = new String[0];
    public static final HashMap<String, String> NO_EXTRA = new HashMap<String, String>();

    public static final HTTPDNSResult EMPTY = new HTTPDNSResult("", NO_IPS, NO_IPS, NO_EXTRA, false, false);

    public static final SniffException sniff_too_often = new SniffException("sniff too often");

    public static final Exception region_not_match = new Exception("region not match");

    // 配置缓存使用的变量
    public static final String CONFIG_CACHE_PREFIX = "httpdns_config_";
    public static final String CONFIG_REGION = "region";
    public static final String CONFIG_ENABLE = "enable";

    public static final String CONFIG_KEY_SERVERS = "serverIps";
    public static final String CONFIG_KEY_PORTS = "ports";
    public static final String CONFIG_CURRENT_INDEX = "current";
    public static final String CONFIG_LAST_INDEX = "last";
    public static final String CONFIG_SERVERS_LAST_UPDATED_TIME = "servers_last_updated_time";
    public static final String CONFIG_CURRENT_SERVER_REGION = "server_region";
}
