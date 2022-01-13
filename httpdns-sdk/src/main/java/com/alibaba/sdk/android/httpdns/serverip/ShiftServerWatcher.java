package com.alibaba.sdk.android.httpdns.serverip;

import com.alibaba.sdk.android.httpdns.request.HttpRequestConfig;
import com.alibaba.sdk.android.httpdns.request.HttpRequestWatcher;

/**
 * 请求失败时，切换服务IP，服务IP都切换过，使用初始IP
 *
 * @author zonglin.nzl
 * @date 2020/12/3
 */
public class ShiftServerWatcher implements HttpRequestWatcher.Watcher {

    private Server[] servers;
    private int currentIndex = 0;

    public ShiftServerWatcher(Server[] servers) {
        this.servers = servers;
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
        currentIndex++;

        if (currentIndex < servers.length) {
            // 更新请求用的IP
            requestConfig.setIp(servers[currentIndex].getServerIp());
            requestConfig.setPort(servers[currentIndex].getPort(requestConfig.getSchema()));
        }
    }
}
