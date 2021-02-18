package com.alibaba.sdk.android.httpdns;

import com.alibaba.sdk.android.httpdns.probe.ProbeTask;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

/**
 * @author zonglin.nzl
 * @date 2020/10/15
 */
public interface ApiForTest {

    void setInitServer(String[] ips, int[] ports);

    void setThread(ScheduledExecutorService scheduledExecutorService);

    void setSocketFactory(ProbeTask.SpeedTestSocketFactory speedTestSocketFactory);

    void setUpdateServerTimeInterval(int timeInterval);

    void setSniffTimeInterval(int timeInterval);

    void setUpdateServerTaskSchema(String schema);

    ExecutorService getWorker();
}
