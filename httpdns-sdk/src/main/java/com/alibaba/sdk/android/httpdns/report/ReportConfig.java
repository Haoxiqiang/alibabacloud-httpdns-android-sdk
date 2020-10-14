package com.alibaba.sdk.android.httpdns.report;

/**
 * Created by liyazhou on 2017/9/25.
 */

class ReportConfig {
    public static final String ACCOUNT_ID = "accountId";

    public static final String SDK_ID = "httpdns";

    // 事件名

    // 业务事件
    // SDK启动
    public static final String EVENT_BIZ_ACTIVE = "biz_active";

    // sniffer事件
    public static final String EVENT_BIZ_SNIFFER = "biz_sniffer";

    // 本地disable
    public static final String EVENT_BIZ_LOCAL_DISABLE = "biz_local_disable";

    // 启用持久化缓存埋点
    public static final String EVENT_BIZ_CACHE = "biz_cache";

    // 允许过期事件
    public static final String EVENT_BIZ_EXPIRED_IP = "biz_expired_ip";

    // 允许解析返回ipv6地址
    public static final String EVENT_BIZ_IPV6_ENABLE = "biz_ipv6_enable";

    // 允许ipv6解析服务
    public static final String EVENT_BIZ_IPV6_SRV_ENABLE = "biz_ipv6_srv_enable";

    // 异常事件
    // sc请求异常
    public static final String EVENT_ERR_SC = "err_sc";

    // httpdns解析异常
    public static final String EVENT_ERR_SRV = "err_srv";

    // 未捕获异常埋点
    public static final String EVENT_ERR_UNCAUGHT_EXCEPTION = "err_uncaught_exception";

    // 性能事件
    // SC请求耗时
    public static final String EVENT_PERF_SC = "perf_sc";

    // httpdns访问耗时
    public static final String EVENT_PERF_SRV = "perf_srv";

    // getip成功率
    public static final String EVENT_PERF_GET_IP_SUCCESS = "perf_getip";

    // 用户调用getip成功比例
    public static final String EVENT_PERF_USER_GET_IP_SUCCESS = "perf_user_getip";

    // ip探测事件埋点
    public static final String EVENT_PERF_IP_SELECTION = "perf_ipselection";


    // 相关参数key
    public static final String KEY_HOST = "host";

    public static final String KEY_SC_ADDR = "scAddr";

    public static final String KEY_SERVER_ADDR = "srvAddr";

    public static final String KEY_ENABLE = "enable";

    public static final String KEY_ERR_CODE = "errCode";

    public static final String KEY_ERR_MSG = "errMsg";

    public static final String KEY_IPV6 = "ipv6";

    public static final String KEY_IPV6_SRV = "ipv6_srv";

    public static final String KEY_EXCEPTION = "exception";

    public static final String KEY_SUCCESS = "success";

    public static final String KEY_COST = "cost";

    public static final String KEY_CACHE_OPEN = "cacheOpen";

    public static final String KEY_DEFAULT_IP = "defaultIp";

    public static final String KEY_SELECTED_IP = "selectedIp";

    public static final String KEY_DEFAULT_IP_COST = "defaultIpCost";

    public static final String KEY_SELECTED_IP_COST = "selectedIpCost";

    public static final String KEY_IP_COUNT = "ipCount";


    // errorcode
    // 默认错误码
    public static final int ERROR_CODE_DEFAULT = 10000;

    // 超时错误码
    public static final int ERROR_CODE_TIMEOUT = 10001;


    public static final String ERROR_MSG_TIMEOUT = "time out exception";
    public static final String ERROR_MSG_DEFAULT = "default error";

    // 安全防护时间为7s
    public static final int CRASH_DEFEND_TIME = 7;

    // 安全防护超出次数为2
    public static final int CRASH_DEFEND_LIMIT = 2;

    // 最大请求时间
    public static final long MAX_HTTPDNS_REQUEST_TIME = (long) (30 * 1000);

    // 最大SC请求时间
    public static final long MAX_SC_REQUEST_TIME = (long) (30 * 1000);

    // 最大IP探测时间
    public static final long MAX_IP_PROBE_TIME = (long) (5 * 1000);

}
