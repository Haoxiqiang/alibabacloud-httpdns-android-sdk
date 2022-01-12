package com.alibaba.sdk.android.httpdns.serverip;

import com.alibaba.sdk.android.httpdns.impl.HttpDnsConfig;
import com.alibaba.sdk.android.httpdns.request.HttpRequestConfig;
import com.alibaba.sdk.android.httpdns.request.HttpRequestWatcher;

/**
 * 请求失败时，切换服务IP，服务IP都切换过，使用初始IP
 *
 * @author zonglin.nzl
 * @date 2020/12/3
 */
public class ShiftServerWatcher implements HttpRequestWatcher.Watcher {

    private HttpDnsConfig config;

    public ShiftServerWatcher(HttpDnsConfig config) {
        this.config = config;
    }

    @Override
    public void onStart(HttpRequestConfig config) {

    }

    @Override
    public void onSuccess(HttpRequestConfig requestConfig, Object data) {
    }

    @Override
    public void onFail(HttpRequestConfig requestConfig, Throwable throwable) {
        // 切换和更新请求的服务IP
        boolean isBackToFirstServer = this.config.getServerConfig().shiftServer(requestConfig.getIp(), requestConfig.getPort());
        // 所有服务IP都尝试过了，重置为初始IP
        if (isBackToFirstServer) {
            this.config.resetServerIpsToInitServer();
        }
        // 更新请求用的IP
        requestConfig.setIp(this.config.getServerConfig().getServerIp());
        requestConfig.setPort(this.config.getServerConfig().getPort());
    }
}
