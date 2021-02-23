package com.alibaba.sdk.android.httpdns.report;


import android.content.Context;

import java.util.HashMap;

/**
 * utils data upload manager
 * Created by liyazhou on 2017/9/25.
 */

public class ReportManager {
    private static final String TAG = "HttpDns:ReportManager";
    private static ReportDispatcher mDispatcher = new ReportDispatcher();
    private static StringBuilder accountIds = new StringBuilder();
    private static HashMap<String, Boolean> reportEnableStatue = new HashMap<>();
    // 上报开关，默认是打开的
    private boolean reportSwitch = true;
    private String accountId = null;

    public static void init(Context context) {
    }

    private ReportManager(String accountIds) {
        this.accountId = accountIds;
    }

    public static void enableReport(String accountId, boolean enable) {

    }

    private static boolean isEnable(String accountId) {
        return false;
    }

    public static ReportManager getDefaultReportManager() {
        return new ReportManager(accountIds.toString());
    }

    public static ReportManager getReportManagerByAccount(String accountId) {
        return new ReportManager(accountId);
    }

    /**
     * 设置accountId，将accountId写入到通用埋点参数中
     *
     * @param accountId
     */
    public void setAccountId(String accountId) {
        this.accountId = accountId;
        accountIds.append("&&").append(accountId);
    }

    private HashMap<String, String> wrapAccountId(HashMap<String, String> params) {
        if (params == null) {
            params = new HashMap<>();
        }
        params.put(ReportConfig.ACCOUNT_ID, this.accountId);
        return params;
    }

    /**
     * SC请求异常埋点
     *
     * @param scAddr  SC服务器ip/host
     * @param errCode 错误码
     * @param errMsg  错误信息
     * @param isIpv6  是否ipv6，0为否，1位是
     */
    public void reportErrorSCRequest(String scAddr, String errCode, String errMsg, int isIpv6) {
    }

    /**
     * httpdns请求异常埋点
     *
     * @param srvAddr httpdns服务器ip/host
     * @param errCode 错误码
     * @param errMsg  错误信息
     * @param isIpv6  是否ipv6，0为否，1位是
     */
    public void reportErrorHttpDnsRequest(String srvAddr, String errCode, String errMsg, int isIpv6, int isIpv6_srv) {

    }


    /**
     * 非捕获异常埋点
     *
     * @param exception 异常信息
     */
    public void reportErrorUncaughtException(String exception) {

    }
}
