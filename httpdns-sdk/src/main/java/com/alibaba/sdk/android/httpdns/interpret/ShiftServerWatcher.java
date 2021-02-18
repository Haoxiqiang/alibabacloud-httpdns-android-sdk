package com.alibaba.sdk.android.httpdns.interpret;

import com.alibaba.sdk.android.httpdns.impl.HttpDnsConfig;
import com.alibaba.sdk.android.httpdns.serverip.ScheduleService;
import com.alibaba.sdk.android.httpdns.request.HttpException;
import com.alibaba.sdk.android.httpdns.request.HttpRequestConfig;
import com.alibaba.sdk.android.httpdns.request.HttpRequestWatcher;

import java.net.SocketTimeoutException;

/**
 * 请求失败时，切换服务IP，服务IP都切换过，更新服务IP
 *
 * @author zonglin.nzl
 * @date 2020/12/3
 */
public class ShiftServerWatcher implements HttpRequestWatcher.Watcher {

    private HttpDnsConfig config;
    private ScheduleService scheduleService;
    private StatusControl statusControl;

    public ShiftServerWatcher(HttpDnsConfig config, ScheduleService scheduleService, StatusControl statusControl) {
        this.config = config;
        this.scheduleService = scheduleService;
        this.statusControl = statusControl;
    }

    @Override
    public void onSuccess(HttpRequestConfig requestConfig, Object data) {
        if (this.config.markOkServer(requestConfig.getIp(), requestConfig.getPort())) {
            if (statusControl != null) {
                statusControl.turnUp();
            }
        }
    }

    @Override
    public void onFail(HttpRequestConfig requestConfig, Throwable throwable) {
        // 是否切换服务IP
        if (shouldShiftServer(throwable)) {
            // 切换和更新请求的服务IP
            boolean isBackToFirstServer = this.config.shiftServer(requestConfig.getIp(), requestConfig.getPort());
            requestConfig.setIp(this.config.getServerIp());
            requestConfig.setPort(this.config.getPort());
            // 所有服务IP都尝试过了，通知上层进一步处理
            if (isBackToFirstServer && scheduleService != null) {
                scheduleService.updateServerIps();
            }
            if (statusControl != null) {
                statusControl.turnDown();
            }
        }
    }

    private boolean shouldShiftServer(Throwable throwable) {
        if (throwable instanceof SocketTimeoutException) {
            return true;
        }
        if (throwable instanceof HttpException) {
            return ((HttpException) throwable).isServerDegrade();
        }
        return false;
    }
}
