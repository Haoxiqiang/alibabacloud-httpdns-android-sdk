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

    private static boolean checkNetwork = true;

    public static boolean isCheckNetwork() {
        return checkNetwork;
    }

    public static void setCheckNetwork(boolean checkNetwork) {
        HttpDnsSettings.checkNetwork = checkNetwork;
    }
}
