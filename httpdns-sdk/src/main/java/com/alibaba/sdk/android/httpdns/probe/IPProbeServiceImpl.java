package com.alibaba.sdk.android.httpdns.probe;


import com.alibaba.sdk.android.httpdns.GlobalDispatcher;
import com.alibaba.sdk.android.httpdns.HttpDnsLog;
import com.alibaba.sdk.android.httpdns.report.ReportManager;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by liyazhou on 2017/12/14.
 */

public class IPProbeServiceImpl implements IPProbeService {
    private AtomicLong taskInteger = new AtomicLong(0);

    private ConcurrentHashMap<String, Long> probeMap = new ConcurrentHashMap<>();

    private IPListUpdateCallback outerCallback = null;

    private IPProbeTaskCallback innerCallback = new IPProbeTaskCallback() {
        @Override
        public void onIPProbeTaskFinished(long taskNumber, IPProbeOptimizeItem item) {
            try {
                if (item != null) {
                    if (!probeMap.containsKey(item.getHostName()) || (probeMap.get(item.getHostName()) != taskNumber)) {
                        // drop the result
                        HttpDnsLog.Logd("corresponding tasknumber not exists, drop the result");
                    } else {
                        if (item != null && item.getIps() != null && item.getDefaultIp() != null &&
                                item.getSelectedIp() != null && item.getHostName() != null) {
                            HttpDnsLog.Logi("defultId:" + item.getDefaultIp() + ", selectedIp:" + item.getSelectedIp() +
                                    ", promote:" + (item.getDefaultTimeCost() - item.getSelectedTimeCost()));
                            reportIpSelection(item.getHostName(), item.getDefaultIp(), item.getSelectedIp(), item.getDefaultTimeCost(), item.getSelectedTimeCost(), item.getIps().length);
                            outerCallback.onIPListUpdate(item.getHostName(), item.getIps());
                            probeMap.remove(item.getHostName());
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    @Override
    public void setIPListUpdateCallback(IPListUpdateCallback callback) {
        this.outerCallback = callback;
    }

    /**
     * 启动一个IP埋点任务
     *
     * @param hostname
     * @param port
     * @param ips
     */
    @Override
    public void launchIPProbeTask(String hostname, int port, String[] ips) {

        // 新建一个新的hostobject作为探测用
        if (getProbeStatus(hostname) == IPProbeStatus.NO_PROBING) {
            // 如果有相同任务在跑就不再重启任务
            long taskNumber = this.taskInteger.addAndGet(1L);
            this.probeMap.put(hostname, taskNumber);
            GlobalDispatcher.getDispatcher().execute(new IPListProbeRunnable(taskNumber, hostname, ips, port, this.innerCallback));
        } else {
            HttpDnsLog.Loge("already launch the same task, drop the task");
        }
    }

    /**
     * 获取埋点任务状态
     *
     * @param hostname
     * @return
     */
    @Override
    public IPProbeStatus getProbeStatus(String hostname) {
        if (probeMap.containsKey(hostname)) {
            return IPProbeStatus.PROBING;
        } else {
            return IPProbeStatus.NO_PROBING;
        }
    }

    /**
     * 停止IP埋点任务
     *
     * @param hostname
     * @return
     */
    @Override
    public boolean stopIPProbeTask(String hostname) {
        //将对应的hostname 从probemap中删除
        if (this.probeMap.containsKey(hostname)) {
            HttpDnsLog.Logd("stop ip probe task for host:" + hostname);
            this.probeMap.remove(hostname);
            return true;
        } else {
            return false;
        }
    }

    /**
     * 上报IP探测埋点
     *
     * @param host
     * @param defaultIp
     * @param selectedIp
     * @param defaultIpCost
     * @param selectedIpCost
     * @param ipCount
     */
    private void reportIpSelection(String host, String defaultIp, String selectedIp, long defaultIpCost, long selectedIpCost, int ipCount) {
        ReportManager reportManager = ReportManager.getInstance();
        if (reportManager != null) {
            reportManager.reportIpSelection(host, defaultIp, selectedIp, defaultIpCost, selectedIpCost, ipCount);
        }
    }

}
