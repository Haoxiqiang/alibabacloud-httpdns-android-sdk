package com.alibaba.sdk.android.httpdns.probe;

import com.alibaba.sdk.android.httpdns.HttpDnsLog;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

/**
 * 单个IP探测runnable
 * Created by liyazhou on 2017/12/14.
 */

class IPProbeTaskRunnable implements Runnable {
    private String ip;
    private int port;
    private CountDownLatch latch = null;
    private ConcurrentHashMap<String, Long> map;

    public IPProbeTaskRunnable(String ip, int port, CountDownLatch latch, ConcurrentHashMap<String, Long> map) {
        this.ip = ip;
        this.port = port;
        this.latch = latch;
        this.map = map;
    }

    @Override
    public void run() {
        try {
            if (this.ip == null || !isValidPort(this.port)) {
                // do nothing
                HttpDnsLog.Loge("invalid params, give up");
            } else {
                long timeCost = probeConnectionTimeCost(this.ip, this.port);
                HttpDnsLog.Logd("connect cost for ip:" + this.ip + " is " + timeCost);
                if (this.map != null) {
                    this.map.put(this.ip, timeCost);
                }
            }


            if (this.latch != null) {
                this.latch.countDown();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 探测建连时间
     *
     * @param ip
     * @param port
     * @return
     */
    private long probeConnectionTimeCost(String ip, int port) {
        Socket client = null;
        long startTime = System.currentTimeMillis();
        long endTime = Integer.MAX_VALUE;

        try {
            client = new Socket();
            SocketAddress remoteAddress = new InetSocketAddress(ip, port);
            client.connect(remoteAddress, ProbeConfig.PROBE_TIME_OUT);
            endTime = System.currentTimeMillis();
        } catch (IOException e) {
            HttpDnsLog.Loge("connect failed:" + e.toString());
        } finally {
            if (client != null) {
                try {
                    client.close();
                } catch (IOException e) {
                    HttpDnsLog.Loge("socket close failed:" + e.toString());
                }
            }
        }

        if (endTime == Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        } else {
            return endTime - startTime;
        }
    }

    private boolean isValidPort(int port) {
        return port >= 1 && port <= 65535;
    }
}
