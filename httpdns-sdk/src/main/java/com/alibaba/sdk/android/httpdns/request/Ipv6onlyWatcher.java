package com.alibaba.sdk.android.httpdns.request;

import com.alibaba.sdk.android.httpdns.log.HttpDnsLog;
import com.alibaba.sdk.android.httpdns.net.Inet64Util;

public class Ipv6onlyWatcher implements HttpRequestWatcher.Watcher {
    private String originIp;
    private boolean changed = false;

    @Override
    public void onStart(HttpRequestConfig config) {
        if (Inet64Util.isIPv6OnlyNetwork()) {
            originIp = config.getIp();
            changed = true;
            String ipv6 = Inet64Util.convertToIPv6(originIp);
            HttpDnsLog.d("origin ip is " + originIp + " change to " + ipv6);
            config.setIp("[" + ipv6 + "]");
        }
    }

    @Override
    public void onSuccess(HttpRequestConfig config, Object data) {
        changeIpBack(config);
    }

    @Override
    public void onFail(HttpRequestConfig config, Throwable throwable) {
        changeIpBack(config);
    }

    private void changeIpBack(HttpRequestConfig config) {
        if (changed) {
            config.setIp(originIp);
            changed = false;
        }
    }
}
