package com.alibaba.sdk.android.httpdns.probe;

/**
 * IP优选配置项
 * Created by liyazhou on 2017/12/15.
 */
public class IPProbeItem {
    /**
     * 进行ip优选的域名
     */
    String hostName;
    /**
     * 用于测试速度的端口
     */
    int port;

    public IPProbeItem(String hostName, int port) {
        this.hostName = hostName;
        this.port = port;
    }

    public String getHostName() {
        return hostName;
    }

    public int getPort() {
        return port;
    }

}
