package com.alibaba.sdk.android.httpdns.probe;

/**
 * Created by liyazhou on 2017/12/15.
 */

public class IPProbeItem {
    String hostName;
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
