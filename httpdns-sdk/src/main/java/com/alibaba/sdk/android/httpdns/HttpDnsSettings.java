package com.alibaba.sdk.android.httpdns;

import com.alibaba.sdk.android.httpdns.net.HttpDnsNetworkDetector;

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

    private static NetworkChecker checker = new NetworkChecker() {
        @Override
        public boolean isIpv6Only() {
            return getNetworkDetector().getNetType() == NetType.v6;
        }
    };

    public static void setNetworkChecker(NetworkChecker checker) {
        HttpDnsSettings.checker = checker;
    }

    public static NetworkChecker getChecker() {
        return checker;
    }

    public static NetworkDetector getNetworkDetector() {
        return HttpDnsNetworkDetector.getInstance();
    }

    /**
     * 需要外部注入的一些网络环境判断
     */
    public interface NetworkChecker {
        boolean isIpv6Only();
    }

    /**
     * 获取网络类型的接口
     */
    public interface NetworkDetector {
        NetType getNetType();
    }
}
