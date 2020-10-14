package com.alibaba.sdk.android.httpdns;

import android.text.TextUtils;

import com.alibaba.sdk.android.httpdns.report.ReportManager;

/**
 * Created by liyazhou on 17/3/24.
 */

public class Sniffer {
    // disable模式下的嗅探频率,30S
    private static final long SNIFF_INTERVAL = (long) (30 * 1000);

    // 嗅探重试次数为0
    private static final int SNIFF_RETRY_TIMES = 0;
    private long lastSnifferTimeStamp = 0;
    private boolean snifferSwitch = true;
    private String hostName = null;
    private static volatile Sniffer instance = null;

    private Sniffer() {
    }

    public static Sniffer getInstance() {
        if (instance == null) {
            synchronized (Sniffer.class) {
                if (instance == null) {
                    instance = new Sniffer();
                }
            }
        }
        return instance;
    }

    //disabled状态下, 嗅探频率为30s
    private boolean isAnotherSniffNeeded() {
        long currentTimeMillis = System.currentTimeMillis();
        if (lastSnifferTimeStamp == 0 || currentTimeMillis - lastSnifferTimeStamp >= SNIFF_INTERVAL) {
            lastSnifferTimeStamp = currentTimeMillis;
            return true;
        } else {
            return false;
        }
    }

    synchronized public void sniff(String host) {
        try {
            if (host != null) {
                hostName = host;
            }

            boolean launchable = true;
            String reason = null;
            if (!this.snifferSwitch) {
                launchable = false;
                reason = "sniffer is turned off";
            } else if (!this.isAnotherSniffNeeded()) {
                launchable = false;
                reason = "sniff too often";
            } else if (TextUtils.isEmpty(this.hostName)) {
                launchable = false;
                reason = "hostname is null";
            }

            if (launchable) {
                HttpDnsLog.Logd("launch a sniff task");
                QueryHostTask task = new QueryHostTask(hostName, QueryType.SNIFF_HOST);
                task.setRetryTimes(SNIFF_RETRY_TIMES);
                GlobalDispatcher.getDispatcher().submit(task);
                this.sniffReport(host, StatusManager.getServerIp(QueryType.SNIFF_HOST));
                this.hostName = null;
            } else {
                HttpDnsLog.Logd("launch sniffer failed due to " + reason);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 停止嗅探,重置lastSnifferTimeStamp
     */
    synchronized public void stopSniff() {
        lastSnifferTimeStamp = 0;
    }

    synchronized public void switchSniff(boolean isOn) {
        snifferSwitch = isOn;
    }

    /**
     * 嗅探上报
     *
     * @param host  嗅探的域名
     * @param srvIp 服务器ip
     */
    private void sniffReport(String host, String srvIp) {
        try {
            // 嗅探上报
            ReportManager reportManager = ReportManager.getInstance();
            if (reportManager != null) {
                reportManager.reportSniffer(host, StatusManager.getServerIp(QueryType.SNIFF_HOST), srvIp);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
