package com.alibaba.sdk.android.httpdns.interpret;

import com.alibaba.sdk.android.httpdns.CacheTtlChanger;
import com.alibaba.sdk.android.httpdns.HTTPDNSResult;
import com.alibaba.sdk.android.httpdns.RequestIpType;
import com.alibaba.sdk.android.httpdns.cache.HostRecord;
import com.alibaba.sdk.android.httpdns.cache.RecordDBHelper;
import com.alibaba.sdk.android.httpdns.impl.HttpDnsConfig;
import com.alibaba.sdk.android.httpdns.log.HttpDnsLog;
import com.alibaba.sdk.android.httpdns.probe.ProbeCallback;
import com.alibaba.sdk.android.httpdns.probe.ProbeService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * 域名解析结果
 *
 * @author zonglin.nzl
 * @date 2020/12/8
 */
public class InterpretHostResultRepo {

    private RecordDBHelper dbHelper;
    private boolean enableCache = false;
    private HttpDnsConfig config;
    private ProbeService ipProbeService;
    private InterpretHostCacheGroup cacheGroup;
    private CacheTtlChanger cacheTtlChanger;
    private ArrayList<String> hostListWhichIpFixed = new ArrayList<>();

    public InterpretHostResultRepo(HttpDnsConfig config, ProbeService ipProbeService, RecordDBHelper dbHelper, InterpretHostCacheGroup cacheGroup) {
        this.config = config;
        this.ipProbeService = ipProbeService;
        this.dbHelper = dbHelper;
        this.cacheGroup = cacheGroup;
    }

    private void readFromDB(boolean cleanCache) {
        final String region = config.getRegion();
        List<HostRecord> records = dbHelper.readFromDb(region);
        for (HostRecord record : records) {
            InterpretHostCache cache = cacheGroup.getCache(record.getCacheKey());
            cache.put(record);
        }
        if (cleanCache) {
            dbHelper.delete(records);
        } else {
            ArrayList<HostRecord> expired = new ArrayList<>();
            for (HostRecord record : records) {
                if (record.isExpired()) {
                    expired.add(record);
                }
            }
            dbHelper.delete(expired);
        }
        if (!config.getRegion().equals(region)) {
            // 防止刚读取完，region变化了
            cacheGroup.clearAll();
        }
        for (final HostRecord record : records) {
            if (!record.isExpired() && RequestIpType.values()[record.getType()] == RequestIpType.v4) {
                ipProbeService.probleIpv4(record.getHost(), record.getIps(), new ProbeCallback() {
                    @Override
                    public void onResult(String host, String[] sortedIps) {
                        update(host, RequestIpType.v4, record.getCacheKey(), sortedIps);
                    }
                });
            }
        }
    }

    private HostRecord save(String region, String host, RequestIpType type, String extra, String cacheKey, String[] ips, int ttl) {

        if (cacheTtlChanger != null) {
            ttl = cacheTtlChanger.changeCacheTtl(host, type, ttl);
        }

        InterpretHostCache cache = cacheGroup.getCache(cacheKey);
        return cache.update(region, host, type, extra, cacheKey, ips, ttl);
    }

    private void updateInner(String host, RequestIpType type, String cacheKey, String[] ips) {
        InterpretHostCache cache = cacheGroup.getCache(cacheKey);
        HostRecord record = cache.updateIps(host, type, ips);
        if (enableCache || hostListWhichIpFixed.contains(host)) {
            final ArrayList<HostRecord> records = new ArrayList<>();
            records.add(record);
            try {
                config.getDbWorker().execute(new Runnable() {
                    @Override
                    public void run() {
                        dbHelper.insertOrUpdate(records);
                    }
                });
            } catch (Exception e) {
            }
        }
    }

    /**
     * 保存解析结果
     *
     * @param region
     * @param host
     * @param type
     * @param response
     */
    public void save(String region, String host, RequestIpType type, String extra, String cacheKey, InterpretHostResponse response) {
        final ArrayList<HostRecord> records = new ArrayList<>();
        switch (type) {
            case v4:
                records.add(save(region, host, RequestIpType.v4, extra, cacheKey, response.getIps(), response.getTtl()));
                break;
            case v6:
                records.add(save(region, host, RequestIpType.v6, extra, cacheKey, response.getIpsv6(), response.getTtl()));
                break;
            case both:
                HostRecord v4Record = save(region, host, RequestIpType.v4, extra, cacheKey, response.getIps(), response.getTtl());
                records.add(v4Record);
                HostRecord v6Record = save(region, host, RequestIpType.v6, extra, cacheKey, response.getIpsv6(), response.getTtl());
                records.add(v6Record);
                break;
        }
        if (enableCache || hostListWhichIpFixed.contains(host)) {
            try {
                config.getDbWorker().execute(new Runnable() {
                    @Override
                    public void run() {
                        dbHelper.insertOrUpdate(records);
                    }
                });
            } catch (Exception e) {
            }
        }
    }

    /**
     * 保存预解析结果
     *
     * @param region
     * @param type
     * @param resolveHostResponse
     */
    public void save(String region, RequestIpType type, ResolveHostResponse resolveHostResponse) {
        final ArrayList<HostRecord> records = new ArrayList<>();
        for (ResolveHostResponse.HostItem item : resolveHostResponse.getItems()) {
            HostRecord record = save(region, item.getHost(), item.getType(), null, null, item.getIps(), item.getTtl());
            if (enableCache || hostListWhichIpFixed.contains(item.getHost())) {
                records.add(record);
            }
        }
        if (records.size() > 0) {
            try {
                config.getDbWorker().execute(new Runnable() {
                    @Override
                    public void run() {
                        dbHelper.insertOrUpdate(records);
                    }
                });
            } catch (Exception e) {
            }
        }
    }

    /**
     * 更新ip, 一般用于更新ip的顺序
     *
     * @param host
     * @param type
     * @param ips
     */
    public void update(String host, RequestIpType type, String cacheKey, String[] ips) {
        switch (type) {
            case v4:
                updateInner(host, RequestIpType.v4, cacheKey, ips);
                break;
            case v6:
                updateInner(host, RequestIpType.v6, cacheKey, ips);
                break;
            default:
                HttpDnsLog.e("update both is impossible for " + host);
                break;
        }
    }


    /**
     * 获取之前保存的解析结果
     *
     * @param host
     * @param type
     * @return
     */
    public HTTPDNSResult getIps(String host, RequestIpType type, String cacheKey) {
        InterpretHostCache cache = cacheGroup.getCache(cacheKey);
        return cache.getResult(host, type);
    }

    /**
     * 仅清除内容缓存
     */
    public void clearMemoryCache() {
        cacheGroup.clearAll();
    }

    /**
     * 仅清除非主站域名的内存缓存
     */
    public void clearMemoryCacheForHostWithoutFixedIP() {
        cacheGroup.clearAll(new ArrayList<String>(getAllHostWithoutFixedIP().keySet()));
    }

    /**
     * 清除所有已解析结果
     */
    public void clear() {
        final List<HostRecord> recordsToBeDeleted = cacheGroup.clearAll();
        if (enableCache && recordsToBeDeleted.size() > 0) {
            try {
                config.getDbWorker().execute(new Runnable() {
                    @Override
                    public void run() {
                        dbHelper.delete(recordsToBeDeleted);
                    }
                });
            } catch (Exception e) {
            }
        }
    }

    /**
     * 清除指定域名的已解析结果
     *
     * @param hosts
     */
    public void clear(ArrayList<String> hosts) {
        if (hosts == null || hosts.size() == 0) {
            clear();
            return;
        }
        final List<HostRecord> recordsToBeDeleted = cacheGroup.clearAll(hosts);
        if (recordsToBeDeleted.size() > 0 && enableCache) {
            try {
                config.getDbWorker().execute(new Runnable() {
                    @Override
                    public void run() {
                        dbHelper.delete(recordsToBeDeleted);
                    }
                });
            } catch (Exception e) {
            }
        }
    }

    /**
     * 获取当前所有已缓存结果的域名
     *
     * @return
     */
    public HashMap<String, RequestIpType> getAllHostWithoutFixedIP() {
        HashMap<String, RequestIpType> result = cacheGroup.getCache(null).getAllHost();
        for (String host : hostListWhichIpFixed) {
            result.remove(host);
        }
        return result;
    }

    /**
     * 配置 本地缓存开关，触发缓存读取逻辑
     *
     * @param enable
     * @param autoCleanCacheAfterLoad
     */
    public void setCachedIPEnabled(boolean enable, final boolean autoCleanCacheAfterLoad) {
        enableCache = enable;
        try {
            config.getDbWorker().execute(new Runnable() {
                @Override
                public void run() {
                    readFromDB(autoCleanCacheAfterLoad);
                }
            });
        } catch (Throwable tr) {
        }
    }

    /**
     * 设置自定义ttl的接口，用于控制缓存的时长
     *
     * @param changer
     */
    public void setCacheTtlChanger(CacheTtlChanger changer) {
        cacheTtlChanger = changer;
    }

    /**
     * 设置主站域名，主站域名的缓存策略和其它域名不同
     * 1. 内存缓存不轻易清除
     * 2. 默认本地缓存，本地缓存的ttl有效
     *
     * @param hostListWhichIpFixed
     */
    public void setHostListWhichIpFixed(List<String> hostListWhichIpFixed) {
        this.hostListWhichIpFixed.clear();
        if (hostListWhichIpFixed != null) {
            this.hostListWhichIpFixed.addAll(hostListWhichIpFixed);
        }
    }
}
