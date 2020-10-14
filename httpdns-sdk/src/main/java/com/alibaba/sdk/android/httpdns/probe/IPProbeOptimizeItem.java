package com.alibaba.sdk.android.httpdns.probe;

/**
 * Created by liyazhou on 2017/12/15.
 */

class IPProbeOptimizeItem {
    private String hostName;

    // IP优选后的ip列表
    private String[] ips;

    // 原有的默认IP
    private String defaultIp;

    // 优选后的最优IP
    private String selectedIp;

    // 默认IP建连耗时
    private long defaultTimeCost;

    // 优选IP建连耗时
    private long selectedTimeCost;

    IPProbeOptimizeItem(String hostName, String[] ips, String defaultIp, String selectedIp, long defaultTimeCost, long selectedTimeCost) {
        this.hostName = hostName;
        this.ips = ips;
        this.defaultIp = defaultIp;
        this.selectedIp = selectedIp;
        this.defaultTimeCost = defaultTimeCost;
        this.selectedTimeCost = selectedTimeCost;
    }

    public String[] getIps() {
        return ips;
    }

    public String getDefaultIp() {
        return defaultIp;
    }

    public String getSelectedIp() {
        return selectedIp;
    }

    public long getDefaultTimeCost() {
        return defaultTimeCost;
    }

    public long getSelectedTimeCost() {
        return selectedTimeCost;
    }

    public String getHostName() {
        return hostName;
    }
}
