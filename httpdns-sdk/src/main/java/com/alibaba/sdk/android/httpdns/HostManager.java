package com.alibaba.sdk.android.httpdns;

import com.alibaba.sdk.android.httpdns.cache.DBCacheManager;
import com.alibaba.sdk.android.httpdns.cache.DBCacheUtils;
import com.alibaba.sdk.android.httpdns.cache.HostRecord;
import com.alibaba.sdk.android.httpdns.probe.IPListUpdateCallback;
import com.alibaba.sdk.android.httpdns.probe.IPProbeItem;
import com.alibaba.sdk.android.httpdns.probe.IPProbeService;
import com.alibaba.sdk.android.httpdns.probe.IPProbeServiceFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;

class HostManager {
    private static HostManager instance = new HostManager();
    private static ConcurrentMap<String, HostObject> hostMap;
    private static ConcurrentSkipListSet<String> resolvingHostSet;
    private static IPProbeService ipProbeService = IPProbeServiceFactory.getIpProbeService(new IPListUpdateCallback() {
        @Override
        public void onIPListUpdate(String hostName, String[] ips) {
            if (hostName != null && ips != null && ips.length != 0) {
                HostObject oldObject = hostMap.get(hostName);
                if (oldObject != null) {
                    HostObject newObject = new HostObject(hostName, ips, oldObject.getTtl(), oldObject.getQueryTime(), oldObject.getExtras(), oldObject.getCacheKey());
                    hostMap.put(hostName, newObject);

                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < newObject.getIps().length; i++) {
                        sb.append(newObject.getIps()[i] + ",");
                    }
                    HttpDnsLog.Loge("optimized host:" + hostName + ", ip:" + sb.toString());
                }
            }
        }
    });

    static HostManager getInstance() {
        return instance;
    }

    private HostManager() {
        hostMap = new ConcurrentHashMap<String, HostObject>();
        resolvingHostSet = new ConcurrentSkipListSet<String>();
    }

    void put(String hostName, HostObject hostObject) {
        hostMap.put(hostName, hostObject);

        if (DBCacheManager.isEnable()) {
            final HostRecord r = hostObject.toHostRecord();
            if ((r.ips != null && r.ips.size() > 0)
                    || (r.ipsv6 != null && r.ipsv6.size() > 0)) {
                DBCacheManager.store(r);
            } else {
                DBCacheManager.clean(r);
            }
        }

        launchIpProbeTask(hostName, hostObject);
    }

    HostObject get(String hostName) {
        return hostMap.get(hostName);
    }

    void updateFromDBCache() {
        if (!DBCacheManager.isEnable()) {
            return;
        }

        GlobalDispatcher.getDispatcher().submit(new Runnable() {
            @Override
            public void run() {
                try {
                    updateFromDBCacheInner();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void updateFromDBCacheInner() {
        final List<HostRecord> LOADED = DBCacheManager.loadAll();

        final String SP = DBCacheManager.getSP();

        for (HostRecord r : LOADED) {
            // 清理过期数据
            if (isDBCacheExpired(r)) {
                DBCacheManager.clean(r);
            } else {
                if (SP.equals(r.sp)) {
                    r.time = String.valueOf(System.currentTimeMillis() / 1000L);
                    HostObject hostObject = new HostObject(r);
                    hostMap.put(r.host, hostObject);

                    if (DBCacheManager.isAutoCleanCacheAfterLoad()) {
                        // 优化：清除本地缓存
                        DBCacheManager.clean(r);
                    }

                    // 持久层捞出的IP进行一次IP探测
                    launchIpProbeTask(r.host, hostObject);
                }
            }
        }
    }

    private boolean isDBCacheExpired(HostRecord r) {
        final long queryTime = DBCacheUtils.getLongSafely(r.time);
        final long currentTime = System.currentTimeMillis() / 1000L;
        // 一周后过期
        return currentTime - queryTime > 7 * 24 * 60 * 60L;
    }

    /**
     * 判断当前域名是否需要探测
     *
     * @param hostname
     * @return
     */
    private IPProbeItem getProbeItem(String hostname) {
        List<IPProbeItem> list = HttpDnsConfig.ipProbeItemList;
        if (list != null) {
            for (int i = 0; i < list.size(); i++) {
                if (hostname.equals(list.get(i).getHostName())) {
                    return list.get(i);
                }
            }
        }

        return null;
    }

    /**
     * 启动IP探测任务
     *
     * @param hostName
     * @param hostObject
     * @return true：成功启动, false:不启动
     */
    private boolean launchIpProbeTask(String hostName, HostObject hostObject) {
        if (hostObject == null || hostObject.getIps() == null ||
                hostObject.getIps().length <= 1 || ipProbeService == null) {
            return false;
        }

        // 判断是否需要探测
        IPProbeItem item = getProbeItem(hostName);
        if (item != null) {
            if (ipProbeService.getProbeStatus(hostName) == IPProbeService.IPProbeStatus.PROBING) {
                // 停止当前正在探测的任务
                ipProbeService.stopIPProbeTask(hostName);
            }

            HttpDnsLog.Loge("START PROBE");
            ipProbeService.launchIPProbeTask(hostName, item.getPort(), hostObject.getIps());
            return true;
        } else {
            return false;
        }
    }

    boolean isResolving(String hostName) {
        return resolvingHostSet.contains(hostName);
    }

    void setHostResolving(String hostName) {
        resolvingHostSet.add(hostName);
    }

    void setHostResolved(String hostName) {
        resolvingHostSet.remove(hostName);
    }

    int count() {
        return hostMap.size();
    }

    void clear() {
        hostMap.clear();
        resolvingHostSet.clear();
    }

    ArrayList<String> getAllHost() {
        return new ArrayList<String>(hostMap.keySet());
    }
}
