package com.alibaba.sdk.android.httpdns.config;

import com.alibaba.sdk.android.httpdns.utils.CommonUtil;

import java.util.Arrays;

/**
 * @author zonglin.nzl
 * @date 1/13/22
 */
public class RegionServer {
    /**
     * HttpDns的服务IP
     */
    private String[] serverIps;
    /**
     * HttpDns的服务端口，线上都是默认端口 80 或者 443
     * 此处是为了测试场景指定端口
     * 下标和{@link #serverIps} 对应
     * 如果为null 表示没有指定端口
     */
    private int[] ports;
    private String region;

    private String[] ipv6ServerIps;
    private String[] ipv6FormatIps;
    private int[] ipv6Ports;

    public RegionServer(String[] serverIps, int[] ports, String[] ipv6ServerIps, int[] ipv6Ports, String region) {
        this.serverIps = serverIps == null ? new String[0] : serverIps;
        this.ports = ports;
        this.region = region;
        this.ipv6ServerIps = ipv6ServerIps == null ? new String[0] : ipv6ServerIps;
        this.ipv6Ports = ipv6Ports;
    }

    public String[] getServerIps() {
        return serverIps;
    }

    public int[] getPorts() {
        return ports;
    }

    public String getRegion() {
        return region;
    }

    public String[] getIpv6ServerIps() {
        return ipv6ServerIps;
    }

    public String[] getIpv6ServerIpsForUse() {
        if(ipv6FormatIps != null) {
            return ipv6FormatIps;
        }
        ipv6FormatIps = new String[ipv6ServerIps.length];
        for (int i = 0; i < ipv6ServerIps.length; i++) {
            ipv6FormatIps[i] = "[" + ipv6ServerIps[i] + "]";
        }
        return ipv6FormatIps;
    }

    public int[] getIpv6Ports() {
        return ipv6Ports;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RegionServer that = (RegionServer) o;
        return Arrays.equals(serverIps, that.serverIps) &&
                Arrays.equals(ports, that.ports) &&
                Arrays.equals(ipv6ServerIps, that.ipv6ServerIps) &&
                Arrays.equals(ipv6Ports, that.ipv6Ports) &&
                CommonUtil.equals(region, that.region);
    }

    public boolean serverEquals(RegionServer that) {
        return Arrays.equals(serverIps, that.serverIps) &&
                Arrays.equals(ports, that.ports) &&
                Arrays.equals(ipv6ServerIps, that.ipv6ServerIps) &&
                Arrays.equals(ipv6Ports, that.ipv6Ports) &&
                CommonUtil.equals(region, that.region);
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(new Object[]{region});
        result = 31 * result + Arrays.hashCode(serverIps);
        result = 31 * result + Arrays.hashCode(ports);
        result = 31 * result + Arrays.hashCode(ipv6ServerIps);
        result = 31 * result + Arrays.hashCode(ipv6Ports);
        return result;
    }

    public boolean update(String[] ips, int[] ports) {
        boolean same = CommonUtil.isSameServer(this.serverIps, this.ports, ips, ports);
        if (same) {
            return false;
        }
        this.serverIps = ips;
        this.ports = ports;
        return true;
    }

    public boolean updateIpv6(String[] ips, int[] ports) {
        boolean same = CommonUtil.isSameServer(this.ipv6ServerIps, this.ipv6Ports, ips, ports);
        if (same) {
            return false;
        }
        this.ipv6ServerIps = ips;
        this.ipv6Ports = ports;
        this.ipv6FormatIps = null;
        return true;
    }

    public boolean updateAll(String region, String[] ips, int[] ports) {
        boolean same = CommonUtil.isSameServer(this.serverIps, this.ports, ips, ports);
        if (same && region.equals(this.region)) {
            return false;
        }
        this.region = region;
        this.serverIps = ips;
        this.ports = ports;
        return true;
    }
}
