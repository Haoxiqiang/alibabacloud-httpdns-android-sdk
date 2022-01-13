package com.alibaba.sdk.android.httpdns.serverip;

import com.alibaba.sdk.android.httpdns.utils.CommonUtil;

/**
 * @author zonglin.nzl
 * @date 1/13/22
 */
public class Server {

    private String serverIp;
    private int port;

    public Server(String serverIp, int port) {
        this.serverIp = serverIp;
        this.port = port;
    }

    public String getServerIp() {
        return serverIp;
    }

    public int getPort(String scheme) {
        return CommonUtil.getPort(port, scheme);
    }
}
