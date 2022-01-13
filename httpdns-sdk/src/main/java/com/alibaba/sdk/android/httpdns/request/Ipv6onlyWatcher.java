package com.alibaba.sdk.android.httpdns.request;

import com.alibaba.sdk.android.httpdns.HttpDnsSettings;
import com.alibaba.sdk.android.httpdns.log.HttpDnsLog;

public class Ipv6onlyWatcher implements HttpRequestWatcher.Watcher {
    private static int currentIndex = 0;
    private String[] ipv6ServerIps;
    private String originIp;
    private String changedIp;
    private boolean changed = false;

    public Ipv6onlyWatcher(String[] ips) {
        this.ipv6ServerIps = ips;
    }

    @Override
    public void onStart(HttpRequestConfig config) {
        HttpDnsSettings.NetworkChecker checker = HttpDnsSettings.getChecker();
        if (checker != null && checker.isIpv6Only() && ipv6ServerIps != null && ipv6ServerIps.length > 0) {
            originIp = config.getIp();
            changed = true;
            String ipv6 = ipv6ServerIps[currentIndex % ipv6ServerIps.length];
            if (HttpDnsLog.isPrint()) {
                HttpDnsLog.d("origin ip is " + originIp + " change to " + ipv6);
            }
            config.setIp("[" + ipv6 + "]");
            changedIp = ipv6;
        }
    }

    @Override
    public void onSuccess(HttpRequestConfig config, Object data) {
        changeIpBack(config, true);
    }

    @Override
    public void onFail(HttpRequestConfig config, Throwable throwable) {
        changeIpBack(config, false);
    }

    private void changeIpBack(HttpRequestConfig config, boolean success) {
        if (changed) {
            config.setIp(originIp);
            changed = false;
            if (!success) {
                if (changedIp.equals(ipv6ServerIps[currentIndex % ipv6ServerIps.length])) {
                    // 使用这个ip请求不成功，修改index，下次使用其它ip
                    currentIndex = (currentIndex + 1) % ipv6ServerIps.length;
                }
            }
        }
    }
}
