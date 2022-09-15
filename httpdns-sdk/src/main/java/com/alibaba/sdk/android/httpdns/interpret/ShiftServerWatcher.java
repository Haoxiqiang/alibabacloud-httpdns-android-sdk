package com.alibaba.sdk.android.httpdns.interpret;

import com.alibaba.sdk.android.httpdns.RequestIpType;
import com.alibaba.sdk.android.httpdns.impl.HttpDnsConfig;
import com.alibaba.sdk.android.httpdns.request.HttpException;
import com.alibaba.sdk.android.httpdns.request.HttpRequestConfig;
import com.alibaba.sdk.android.httpdns.request.HttpRequestWatcher;
import com.alibaba.sdk.android.httpdns.serverip.ScheduleService;

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
    private long beginRequestTime;

    public ShiftServerWatcher(HttpDnsConfig config, ScheduleService scheduleService, StatusControl statusControl) {
        this.config = config;
        this.scheduleService = scheduleService;
        this.statusControl = statusControl;
    }

    @Override
    public void onStart(HttpRequestConfig config) {
        beginRequestTime = System.currentTimeMillis();
    }

    @Override
    public void onSuccess(HttpRequestConfig requestConfig, Object data) {
        if(requestConfig.getIpType() == RequestIpType.v6) {
            if (this.config.getCurrentServer().markOkServerV6(requestConfig.getIp(), requestConfig.getPort())) {
                if (statusControl != null) {
                    statusControl.turnUp();
                }
            }
        } else {
            if (this.config.getCurrentServer().markOkServer(requestConfig.getIp(), requestConfig.getPort())) {
                if (statusControl != null) {
                    statusControl.turnUp();
                }
            }
        }

    }

    @Override
    public void onFail(HttpRequestConfig requestConfig, Throwable throwable) {
        long cost = System.currentTimeMillis() - beginRequestTime;
        // 是否切换服务IP, 超过超时时间，我们也切换ip，花费时间太长，说明这个ip可能也有问题
        if (shouldShiftServer(throwable) || cost > requestConfig.getTimeout()) {
            // 切换和更新请求的服务IP
            boolean isBackToFirstServer = false;
            if(requestConfig.getIpType() == RequestIpType.v6) {
                isBackToFirstServer = this.config.getCurrentServer().shiftServerV6(requestConfig.getIp(), requestConfig.getPort());
                requestConfig.setIp(this.config.getCurrentServer().getServerIpForV6());
                requestConfig.setPort(this.config.getCurrentServer().getPortForV6());
            } else {
                isBackToFirstServer = this.config.getCurrentServer().shiftServer(requestConfig.getIp(), requestConfig.getPort());
                requestConfig.setIp(this.config.getCurrentServer().getServerIp());
                requestConfig.setPort(this.config.getCurrentServer().getPort());
            }

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
        if (throwable instanceof HttpException) {
            return ((HttpException) throwable).shouldShiftServer();
        }
        // 除了特定的一些错误（sdk问题或者客户配置问题，不是服务网络问题），都切换服务IP，这边避免个别服务节点真的访问不上
        // 一方面尽可能提高可用性，另一方面当客户发生异常时，也方便根据服务IP来判断是否是真的无网络，因为多个服务IP都访问不上的可能性较低
        // 还有一方面是 sniff模式是在切换服务IP的前提下触发的，这样也提高的sniff模式触发几率，在真的网络异常时，降低网络请求频次
        return true;
    }
}
