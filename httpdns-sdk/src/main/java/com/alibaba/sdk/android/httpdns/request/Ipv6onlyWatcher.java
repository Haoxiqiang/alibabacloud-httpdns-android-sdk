package com.alibaba.sdk.android.httpdns.request;

import com.alibaba.sdk.android.httpdns.impl.HttpDnsConfig;
import com.alibaba.sdk.android.httpdns.log.HttpDnsLog;
import com.alibaba.sdk.android.httpdns.net.Inet64Util;

public class Ipv6onlyWatcher implements HttpRequestWatcher.Watcher {
    private HttpDnsConfig config;
    private String originIp;
    private boolean changed = false;

    public Ipv6onlyWatcher(HttpDnsConfig config) {
        this.config = config;
    }

    @Override
    public void onStart(HttpRequestConfig config) {
        if (Inet64Util.isIPv6OnlyNetwork()) {
            originIp = config.getIp();
            changed = true;
            String ipv6 = this.config.getIpv6ServerIp();
            HttpDnsLog.d("origin ip is " + originIp + " change to " + ipv6);
            config.setIp("[" + ipv6 + "]");
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
                this.config.shiftIpv6Server();
            }
        }
    }
}
