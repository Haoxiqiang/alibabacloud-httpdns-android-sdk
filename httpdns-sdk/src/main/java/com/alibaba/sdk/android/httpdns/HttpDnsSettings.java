package com.alibaba.sdk.android.httpdns;

/**
 * @author zonglin.nzl
 * @date 11/8/21
 */
public class HttpDnsSettings {

    private static boolean dailyReport = true;

    public static void setDailyReport(boolean dailyReport) {
        HttpDnsSettings.dailyReport = dailyReport;
    }

    public static boolean isDailyReport() {
        return dailyReport;
    }

    private static NetworkChecker checker;

    public static void setNetworkChecker(NetworkChecker checker) {
        HttpDnsSettings.checker = checker;
    }

    public static NetworkChecker getChecker() {
        return checker;
    }

    /**
     * 需要外部注入的一些网络环境判断
     */
    public interface NetworkChecker {
        boolean isIpv6Only();
    }
}
