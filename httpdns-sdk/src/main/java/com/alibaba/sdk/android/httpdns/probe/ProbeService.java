package com.alibaba.sdk.android.httpdns.probe;

import com.alibaba.sdk.android.httpdns.impl.HttpDnsConfig;

import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * IP优选服务
 *
 * @author zonglin.nzl
 * @date 2020/12/3
 */
public class ProbeService {

    private HttpDnsConfig config;
    private List<IPProbeItem> probeItems;
    private ProbeTask.SpeedTestSocketFactory socketFactory = new ProbeTask.SpeedTestSocketFactory() {
        @Override
        public Socket create() {
            return new Socket();
        }
    };
    private ConcurrentSkipListSet<String> probingHosts = new ConcurrentSkipListSet<>();

    public ProbeService(HttpDnsConfig config) {
        this.config = config;
    }

    /**
     * 进行ipv4优选
     *
     * @param host
     * @param ips
     * @param probeCallback
     */
    public void probleIpv4(String host, String[] ips, final ProbeCallback probeCallback) {
        if (config.isProbeDisabled()) {
            return;
        }
        IPProbeItem ipProbeItem = getIpProbeItem(host);
        if (ipProbeItem != null && ips != null && ips.length > 1) {
            if (probingHosts.contains(host)) {
                return;
            }
            probingHosts.add(host);
            config.getWorker().execute(new ProbeTask(socketFactory, host, ips, ipProbeItem, new ProbeCallback() {
                @Override
                public void onResult(String host, String[] sortedIps) {
                    probingHosts.remove(host);
                    if (probeCallback != null) {
                        probeCallback.onResult(host, sortedIps);
                    }
                }
            }));
        }
    }

    public void setProbeItems(List<IPProbeItem> probeItems) {
        this.probeItems = probeItems;
    }

    public void setSocketFactory(ProbeTask.SpeedTestSocketFactory socketFactory) {
        this.socketFactory = socketFactory;
    }

    private IPProbeItem getIpProbeItem(String host) {
        if (probeItems != null && probeItems.size() > 0) {
            ArrayList<IPProbeItem> list = new ArrayList<>(probeItems);
            for (IPProbeItem item : list) {
                if (host.equals(item.getHostName())) {
                    return item;
                }
            }
        }
        return null;
    }
}
