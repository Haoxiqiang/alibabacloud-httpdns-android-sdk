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

    public RegionServer(String[] serverIps, int[] ports, String region) {
        this.serverIps = serverIps;
        this.ports = ports;
        this.region = region;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RegionServer that = (RegionServer) o;
        return Arrays.equals(serverIps, that.serverIps) &&
                Arrays.equals(ports, that.ports) &&
                CommonUtil.equals(region, that.region);
    }

    public boolean serverEquals(RegionServer that) {
        return Arrays.equals(serverIps, that.serverIps) &&
                Arrays.equals(ports, that.ports) &&
                CommonUtil.equals(region, that.region);
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(new Object[]{region});
        result = 31 * result + Arrays.hashCode(serverIps);
        result = 31 * result + Arrays.hashCode(ports);
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
