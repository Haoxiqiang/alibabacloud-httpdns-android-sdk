package com.alibaba.sdk.android.httpdns.probe;

import com.alibaba.sdk.android.httpdns.utils.CommonUtil;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

/**
 * ip优选实现
 *
 * @author zonglin.nzl
 * @date 2020/10/23
 */
public class ProbeTask implements Runnable {

    public interface SpeedTestSocketFactory {
        Socket create();
    }

    private SpeedTestSocketFactory socketFactory;
    private String host;
    private String[] ips;
    private IPProbeItem ipProbeItem;
    private ProbeCallback probeCallback;

    public ProbeTask(SpeedTestSocketFactory socketFactory, String host, String[] ips, IPProbeItem ipProbeItem, ProbeCallback probeCallback) {
        this.socketFactory = socketFactory;
        this.host = host;
        this.ips = ips;
        this.ipProbeItem = ipProbeItem;
        this.probeCallback = probeCallback;
    }

    @Override
    public void run() {
        int[] speeds = new int[ips.length];
        for (int i = 0; i < ips.length; i++) {
            speeds[i] = testConnectSpeed(ips[i], ipProbeItem.getPort());
        }
        String[] result = CommonUtil.sortIpsWithSpeeds(ips, speeds);
        if (probeCallback != null) {
            probeCallback.onResult(host, result);
        }
    }

    private int testConnectSpeed(String ip, int port) {
        Socket socket = socketFactory.create();
        long start = System.currentTimeMillis();
        long end = Long.MAX_VALUE;
        SocketAddress remoteAddress = new InetSocketAddress(ip, port);
        try {
            socket.connect(remoteAddress, 5 * 1000);
            end = System.currentTimeMillis();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (end == Long.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) (end - start);

    }
}
