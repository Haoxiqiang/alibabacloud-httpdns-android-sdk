package com.alibaba.sdk.android.httpdns;

import com.alibaba.sdk.android.httpdns.probe.IPProbeItem;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class HttpDnsConfig {
    static String accountID;
    static final boolean DEBUG = false;
    // 此处需要配置自己的初始服务IP
    static String[] SERVER_IPS = new String[]{"初始服务IP"};
    // 此处需要配置自己的初始服务IP
    static final String[] SCHEDULE_CENTER_URLS = {"初始服务IP"};
    static final String LOG_TAG = "HttpDnsSDK";
    static final String HTTP_PROTOCOL = "http://";
    static final String HTTPS_PROTOCOL = "https://";
    static final String[] NO_IPS = new String[0];
    static final String HTTPS_CERTIFICATE_HOSTNAME = "203.107.1.1";
    static String SERVER_PORT = "80";
    static String PROTOCOL = HTTP_PROTOCOL; //默认使用http协议

    static final int RESOLVE_RETRY_TIMES = 1;
    static final int MAX_HOST_OBJECT = 100;
    static final int MAX_THREAD_NUM = 3;
    static int CUSTOMIZED_RESOLVE_TIMEOUT_IN_MESC = 15 * 1000;//用户自定义超时时间,默认为15S
    static Map<String, String> extra = new HashMap<String, String>();

    // IP探测列表
    static List<IPProbeItem> ipProbeItemList = null;

    synchronized static void setAccountID(String accountID) {
        HttpDnsConfig.accountID = accountID;
    }

    synchronized static void setTimeoutInterval(int timeoutInterval) {
        if (timeoutInterval > 0) {
            HttpDnsConfig.CUSTOMIZED_RESOLVE_TIMEOUT_IN_MESC = timeoutInterval;
        }
    }

    synchronized static void setHTTPSRequestEnabled(boolean enabled) {
        if (enabled) {
            HttpDnsConfig.PROTOCOL = HTTPS_PROTOCOL;
            HttpDnsConfig.SERVER_PORT = "443";//https用443端口
        } else {
            HttpDnsConfig.PROTOCOL = HTTP_PROTOCOL;
            HttpDnsConfig.SERVER_PORT = "80";
        }
    }

    synchronized static boolean setServerIps(String[] serverIps) {
        if (serverIps != null && serverIps.length != 0) {
            SERVER_IPS = serverIps;
            HttpDnsLog.Logd("serverIps:" + Arrays.toString(SERVER_IPS));
            return true;
        } else {
            return false;
        }
    }

    synchronized static void setIpProbeItemList(List<IPProbeItem> ipProbeItemList) {
        HttpDnsConfig.ipProbeItemList = ipProbeItemList;
    }

    synchronized static void setSdnsGlobalParams(Map<String, String> params) {
        HttpDnsConfig.extra.putAll(params);
    }

    synchronized static void clearSdnsGlobalParams() {
        extra.clear();
    }

}
