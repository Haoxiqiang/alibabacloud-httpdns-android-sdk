package com.alibaba.sdk.android.httpdns.serverip;

/**
 * 服务IP存储的数据
 * @author zonglin.nzl
 * @date 2020/12/4
 */
public class ServerIpData {
    private String region;
    private long requestTime;
    private String[] serverIps;
    private int[] serverPorts;

    public ServerIpData(String region, long requestTime, String[] serverIps, int[] ports) {
        this.region = region;
        this.requestTime = requestTime;
        this.serverIps = serverIps;
        this.serverPorts = ports;
    }

    public ServerIpData(String region, String[] serverIps, int[] ports) {
        this(region, System.currentTimeMillis(), serverIps, ports);
    }

    public String getRegion() {
        return region;
    }

    public long getRequestTime() {
        return requestTime;
    }

    public String[] getServerIps() {
        return serverIps;
    }

    public int[] getServerPorts() {
        return serverPorts;
    }
}
