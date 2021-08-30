package com.alibaba.sdk.android.httpdns.impl;

import android.content.Context;

import com.alibaba.sdk.android.httpdns.ApiForTest;
import com.alibaba.sdk.android.httpdns.probe.ProbeTask;
import com.alibaba.sdk.android.httpdns.serverip.UpdateServerTask;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

/**
 * @author zonglin.nzl
 * @date 2020/10/16
 */
public class HttpDnsServiceTestImpl extends HttpDnsServiceImpl implements ApiForTest {
    public HttpDnsServiceTestImpl(Context context, String accountId, String secret) {
        super(context, accountId, secret);
    }

    @Override
    protected void reportSdkStart(Context context, String accountId) {
        // do nothing for test
    }

    @Override
    protected void initCrashDefend(Context context, HttpDnsConfig config) {
        // do nothing for test
        config.crashDefend(false);
    }

    @Override
    protected void initBeacon(Context context, String accountId, HttpDnsConfig config) {
        // do nothing for test
    }

    @Override
    public void setInitServer(String[] ips, int[] ports) {
        this.config.setInitServers(ips, ports);
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
}
