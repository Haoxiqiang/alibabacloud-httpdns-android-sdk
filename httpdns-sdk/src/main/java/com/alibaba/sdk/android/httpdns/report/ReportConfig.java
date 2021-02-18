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

    // 异常事件
    // sc请求异常
    public static final String EVENT_ERR_SC = "err_sc";

    // httpdns解析异常
    public static final String EVENT_ERR_SRV = "err_srv";

    // 未捕获异常埋点
    public static final String EVENT_ERR_UNCAUGHT_EXCEPTION = "err_uncaught_exception";

    public static final String KEY_SC_ADDR = "scAddr";

    public static final String KEY_SERVER_ADDR = "srvAddr";

    public static final String KEY_ERR_CODE = "errCode";

    public static final String KEY_ERR_MSG = "errMsg";

    public static final String KEY_IPV6 = "ipv6";

    public static final String KEY_IPV6_SRV = "ipv6_srv";

    public static final String KEY_EXCEPTION = "exception";


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
}
