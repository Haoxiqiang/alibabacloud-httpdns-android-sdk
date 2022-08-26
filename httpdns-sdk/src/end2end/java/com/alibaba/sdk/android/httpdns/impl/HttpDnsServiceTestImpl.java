package com.alibaba.sdk.android.httpdns.impl;

import android.content.Context;

import com.alibaba.sdk.android.httpdns.ApiForTest;
import com.alibaba.sdk.android.httpdns.BeforeHttpDnsServiceInit;
import com.alibaba.sdk.android.httpdns.InitManager;
import com.alibaba.sdk.android.httpdns.probe.ProbeTask;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

/**
 * 测试时 使用的httpdns 实例
 * 增加了一些用于测试的api和机制
 * @author zonglin.nzl
 * @date 2020/10/16
 */
public class HttpDnsServiceTestImpl extends HttpDnsServiceImpl implements ApiForTest {
    public HttpDnsServiceTestImpl(Context context, String accountId, String secret) {
        super(context, accountId, secret);
    }

    @Override
    protected void beforeInit() {
        super.beforeInit();
        // 通过InitManager 在httpdns初始化之前 进行一些测试需要前置工作
        BeforeHttpDnsServiceInit init = InitManager.getInstance().getAndRemove(config.getAccountId());

        if (init != null) {
            init.beforeInit(this);
        }
    }

    @Override
    protected void initCrashDefend(Context context, HttpDnsConfig config) {
        // do nothing for test
        config.crashDefend(false);
    }

    @Override
    public void setInitServer(String region, String[] ips, int[] ports) {
        this.config.setInitServers(region, ips, ports);
    }

    @Override
    public void setThread(ScheduledExecutorService scheduledExecutorService) {
        this.config.setWorker(scheduledExecutorService);
    }

    @Override
    public void setSocketFactory(ProbeTask.SpeedTestSocketFactory speedTestSocketFactory) {
        this.ipProbeService.setSocketFactory(speedTestSocketFactory);
    }

    @Override
    public void setUpdateServerTimeInterval(int timeInterval) {
        this.scheduleService.setTimeInterval(timeInterval);
    }

    @Override
    public void setSniffTimeInterval(int timeInterval) {
        this.requestHandler.setSniffTimeInterval(timeInterval);
    }

    @Override
    public ExecutorService getWorker() {
        return this.config.worker;
    }

    @Override
    public void setDefaultUpdateServer(String[] defaultServerIps, int[] ports) {
        this.config.setDefaultUpdateServer(defaultServerIps, ports);
    }

    @Override
    public void setInitServerIpv6(String[] ips, int[] ports) {
        this.config.setInitServersIpv6(ips, ports);
    }

    @Override
    public void setDefaultUpdateServerIpv6(String[] defaultServerIps, int[] ports) {
        this.config.setDefaultUpdateServerIpv6(defaultServerIps, ports);
    }
}
