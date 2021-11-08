package com.alibaba.sdk.android.httpdns.interpret;

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

    public InterpretHostResultRepo(HttpDnsConfig config, ProbeService ipProbeService, RecordDBHelper dbHelper, InterpretHostCacheGroup cacheGroup) {
        this.config = config;
        this.ipProbeService = ipProbeService;
        this.dbHelper = dbHelper;
        this.cacheGroup = cacheGroup;
    }

    private void readFromDB(boolean cleanCache) {
        List<HostRecord> records = dbHelper.readFromDb();
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

    private HostRecord save(String host, RequestIpType type, String extra, String cacheKey, String[] ips, int ttl) {
        InterpretHostCache cache = cacheGroup.getCache(cacheKey);
        return cache.update(host, type, extra, cacheKey, ips, ttl);
    }

    private void updateInner(String host, RequestIpType type, String cacheKey, String[] ips) {
        InterpretHostCache cache = cacheGroup.getCache(cacheKey);
        HostRecord record = cache.updateIps(host, type, ips);
        if (enableCache) {
            ArrayList<HostRecord> records = new ArrayList<>();
            records.add(record);
            dbHelper.insertOrUpdate(records);
        }
    }

    /**
     * 保存结果
     *
     * @param host
     * @param type
     * @param response
     */
    public void save(String host, RequestIpType type, String extra, String cacheKey, InterpretHostResponse response) {
        ArrayList<HostRecord> records = new ArrayList<>();
        switch (type) {
            case v4:
                records.add(save(host, RequestIpType.v4, extra, cacheKey, response.getIps(), response.getTtl()));
                break;
            case v6:
                records.add(save(host, RequestIpType.v6, extra, cacheKey, response.getIpsv6(), response.getTtl()));
                break;
            case both:
                HostRecord v4Record = save(host, RequestIpType.v4, extra, cacheKey, response.getIps(), response.getTtl());
                records.add(v4Record);
                HostRecord v6Record = save(host, RequestIpType.v6, extra, cacheKey, response.getIpsv6(), response.getTtl());
                records.add(v6Record);
                break;
        }
        if (enableCache) {
            dbHelper.insertOrUpdate(records);
        }
    }

    public void save(RequestIpType type, ResolveHostResponse resolveHostResponse) {
        ArrayList<HostRecord> records = new ArrayList<>();
        for (String host : resolveHostResponse.getHosts()) {
            switch (type) {
                case v4:
                    records.add(save(host, RequestIpType.v4, null, null, resolveHostResponse.getItem(host).getIps(), resolveHostResponse.getItem(host).getTtl()));
                    break;
                case v6:
                    records.add(save(host, RequestIpType.v6, null, null, resolveHostResponse.getItem(host).getIpv6s(), resolveHostResponse.getItem(host).getTtl()));
                    break;
                case both:
                    records.add(save(host, RequestIpType.v4, null, null, resolveHostResponse.getItem(host).getIps(), resolveHostResponse.getItem(host).getTtl()));
                    records.add(save(host, RequestIpType.v6, null, null, resolveHostResponse.getItem(host).getIpv6s(), resolveHostResponse.getItem(host).getTtl()));
                    break;
            }
        }
        if (enableCache) {
            dbHelper.insertOrUpdate(records);
        }
    }

    /**
     * 更新ip
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
     * 清除已解析的结果
     */
    public void clear() {
        List<HostRecord> recordsToBeDeleted = cacheGroup.clearAll();
        if (enableCache && recordsToBeDeleted.size() > 0) {
            dbHelper.delete(recordsToBeDeleted);
        }
    }

    public void clear(ArrayList<String> hosts) {
        if (hosts == null || hosts.size() == 0) {
            clear();
            return;
        }
        List<HostRecord> recordsToBeDeleted = cacheGroup.clearAll(hosts);
        if (recordsToBeDeleted.size() > 0 && enableCache) {
            dbHelper.delete(recordsToBeDeleted);
        }
    }

    public HashMap<String, RequestIpType> getAllHost() {
        return cacheGroup.getCache(null).getAllHost();
    }

    public void setCachedIPEnabled(boolean enable, final boolean autoCleanCacheAfterLoad) {
        enableCache = enable;
        config.getWorker().execute(new Runnable() {
            @Override
            public void run() {
                readFromDB(autoCleanCacheAfterLoad);
            }
        });

    }
}
