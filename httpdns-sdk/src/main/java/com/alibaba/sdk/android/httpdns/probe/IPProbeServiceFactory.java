package com.alibaba.sdk.android.httpdns.probe;

/**
 * Created by liyazhou on 2017/12/14.
 */

public final class IPProbeServiceFactory {
    private static IPProbeService ipProbeService = null;

    synchronized public static IPProbeService getIpProbeService(IPListUpdateCallback callback) {
        if (ipProbeService == null) {
            ipProbeService = new IPProbeServiceImpl();
            ipProbeService.setIPListUpdateCallback(callback);
        }

        return ipProbeService;
    }
}
