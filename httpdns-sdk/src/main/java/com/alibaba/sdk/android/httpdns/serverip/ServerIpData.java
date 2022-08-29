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
    private String[] serverV6Ips;
    private int[] serverV6Ports;

    public ServerIpData(String region, long requestTime, String[] serverIps, int[] ports, String[] serverV6Ips, int[] v6Ports) {
        this.region = region;
        this.requestTime = requestTime;
        this.serverIps = serverIps;
        this.serverPorts = ports;
        this.serverV6Ips = serverV6Ips;
        this.serverV6Ports = v6Ports;
    }

    public ServerIpData(String region, String[] serverIps, int[] ports, String[] serverV6Ips, int[] v6Ports) {
        this(region, System.currentTimeMillis(), serverIps, ports, serverV6Ips, v6Ports);
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

    public String[] getServerV6Ips() {
        return serverV6Ips;
    }

    public int[] getServerV6Ports() {
        return serverV6Ports;
    }
}
