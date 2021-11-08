package com.alibaba.sdk.android.httpdns.interpret;

import com.alibaba.sdk.android.httpdns.HTTPDNSResult;
import com.alibaba.sdk.android.httpdns.RequestIpType;
import com.alibaba.sdk.android.httpdns.cache.HostRecord;
import com.alibaba.sdk.android.httpdns.cache.RecordDBHelper;
import com.alibaba.sdk.android.httpdns.impl.HttpDnsConfig;
import com.alibaba.sdk.android.httpdns.log.HttpDnsLog;
import com.alibaba.sdk.android.httpdns.probe.ProbeCallback;
import com.alibaba.sdk.android.httpdns.probe.ProbeService;
import com.alibaba.sdk.android.httpdns.utils.CommonUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 域名解析结果
 *
 * @author zonglin.nzl
 * @date 2020/12/8
 */
public class InterpretHostResultRepo {

    private ConcurrentHashMap<String, HostRecord> interpretResults = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, HTTPDNSResult> httpDnsResults = new ConcurrentHashMap<>();
    private RecordDBHelper dbHelper;
    private boolean enableCache = false;
    private HttpDnsConfig config;
    private ProbeService ipProbeService;

    public InterpretHostResultRepo(HttpDnsConfig config, ProbeService ipProbeService, RecordDBHelper dbHelper) {
        dbHelper = new RecordDBHelper(config.getContext(), config.getAccountId());
        this.config = config;
        this.ipProbeService = ipProbeService;
        this.dbHelper = dbHelper;
    }

    private void readFromDB(boolean cleanCache) {
        List<HostRecord> records = dbHelper.readFromDb();
        for (HostRecord record : records) {
            interpretResults.put(CommonUtil.formKey(record.getHost(), RequestIpType.values()[record.getType()], record.getCacheKey()), record);
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
        String key = CommonUtil.formKey(host, type, cacheKey);
        HostRecord record = interpretResults.get(key);
        if (record == null) {
            record = HostRecord.create(host, type, extra, cacheKey, ips, ttl);
            interpretResults.put(key, record);
        } else {
            record.setQueryTime(System.currentTimeMillis());
            record.setIps(ips);
            record.setTtl(ttl);
            record.setExtra(extra);
            record.setFromDB(false);
        }
        return record;
    }

    private void updateInner(String host, RequestIpType type, String cacheKey, String[] ips) {
        String key = CommonUtil.formKey(host, type, cacheKey);
        HostRecord record = interpretResults.get(key);
        if (record == null) {
            return;
        }
        record.setIps(ips);
        if (enableCache) {
            ArrayList<HostRecord> records = new ArrayList<>();
            records.add(record);
            dbHelper.insertOrUpdate(records);
        }
        HTTPDNSResult result = httpDnsResults.get(key);
        if (result != null) {
            result.udpateIps(ips, type);
        }

        key = CommonUtil.formKey(host, RequestIpType.both, cacheKey);
        result = httpDnsResults.get(key);
        if (result != null) {
            result.udpateIps(ips, type);
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
        String keyboth;
        HTTPDNSResult bothResult;
        switch (type) {
            case v4:
                records.add(save(host, RequestIpType.v4, extra, cacheKey, response.getIps(), response.getTtl()));
                keyboth = CommonUtil.formKey(host, RequestIpType.both, cacheKey);
                bothResult = httpDnsResults.get(keyboth);
                if (bothResult != null) {
                    bothResult.update(records);
                }
                break;
            case v6:
                records.add(save(host, RequestIpType.v6, extra, cacheKey, response.getIpsv6(), response.getTtl()));
                keyboth = CommonUtil.formKey(host, RequestIpType.both, cacheKey);
                bothResult = httpDnsResults.get(keyboth);
                if (bothResult != null) {
                    bothResult.update(records);
                }
                break;
            case both:
                HostRecord v4Record = save(host, RequestIpType.v4, extra, cacheKey, response.getIps(), response.getTtl());
                records.add(v4Record);
                String v4key = CommonUtil.formKey(host, RequestIpType.v4, cacheKey);
                HTTPDNSResult v4Result = httpDnsResults.get(v4key);
                if (v4Result != null) {
                    v4Result.update(v4Record);
                }
                HostRecord v6Record = save(host, RequestIpType.v6, extra, cacheKey, response.getIpsv6(), response.getTtl());
                records.add(v6Record);
                String v6key = CommonUtil.formKey(host, RequestIpType.v6, cacheKey);
                HTTPDNSResult v6Result = httpDnsResults.get(v6key);
                if (v6Result != null) {
                    v6Result.update(v6Record);
                }
                break;
        }
        String key = CommonUtil.formKey(host, type, cacheKey);
        HTTPDNSResult result = httpDnsResults.get(key);
        if (result != null) {
            result.update(records);
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
        String key = CommonUtil.formKey(host, type, cacheKey);
        HTTPDNSResult result = httpDnsResults.get(key);
        if (result == null) {
            result = buildHttpResult(host, type, cacheKey);
            if (result != null) {
                httpDnsResults.put(key, result);
            }
        }
        return result;
    }

    private HTTPDNSResult buildHttpResult(String host, RequestIpType type, String cacheKey) {
        String key;
        HostRecord record;
        HTTPDNSResult result = null;
        switch (type) {
            case v6:
                key = CommonUtil.formKey(host, RequestIpType.v6, cacheKey);
                record = interpretResults.get(key);
                if (record != null) {
                    result = new HTTPDNSResult(host);
                    result.update(record);
                }
                break;
            case v4:
                key = CommonUtil.formKey(host, RequestIpType.v4, cacheKey);
                record = interpretResults.get(key);
                if (record != null) {
                    result = new HTTPDNSResult(host);
                    result.update(record);
                }
                break;
            default:
                key = CommonUtil.formKey(host, RequestIpType.v4, cacheKey);
                String keyv6 = CommonUtil.formKey(host, RequestIpType.v6, cacheKey);
                record = interpretResults.get(key);
                HostRecord recordv6 = interpretResults.get(keyv6);
                if (record == null || recordv6 == null) {
                    return result;
                }
                result = new HTTPDNSResult(host);
                ArrayList<HostRecord> records = new ArrayList<>();
                records.add(record);
                records.add(recordv6);
                result.update(records);
                break;
        }
        return result;
    }


    /**
     * 清除已解析的结果
     */
    public void clear() {
        if (enableCache) {
            dbHelper.delete(new ArrayList<HostRecord>(interpretResults.values()));
        }
        interpretResults.clear();
        httpDnsResults.clear();
    }

    public void clear(ArrayList<String> hosts) {
        if (hosts == null || hosts.size() == 0) {
            clear();
            return;
        }
        ArrayList<String> hostKeys = new ArrayList<>(interpretResults.keySet());
        ArrayList<HostRecord> recordsToBeDeleted = new ArrayList<>();
        for (String hostKey : hostKeys) {
            if (isTargetKey(hostKey, hosts)) {
                HostRecord record = interpretResults.remove(hostKey);
                recordsToBeDeleted.add(record);
            }
        }
        if (recordsToBeDeleted.size() > 0 && enableCache) {
            dbHelper.delete(recordsToBeDeleted);
        }

        hostKeys = new ArrayList<>(httpDnsResults.keySet());
        for (String hostKey : hostKeys) {
            if (isTargetKey(hostKey, hosts)) {
                httpDnsResults.remove(hostKey);
            }
        }
    }

    private boolean isTargetKey(String hostKey, ArrayList<String> hosts) {
        String host = CommonUtil.parseHost(hostKey);
        return host != null && hosts.contains(host);
    }

    public HashMap<String, RequestIpType> getAllHost() {
        HashMap<String, RequestIpType> all = new HashMap<>();
        for (HostRecord record : interpretResults.values()) {
            RequestIpType type = all.get(record.getHost());
            if (record.getCacheKey() != null) {
                // ignore sdns
                continue;
            } else if (type == null) {
                type = RequestIpType.values()[record.getType()];
            } else if (type.ordinal() != record.getType()) {
                type = RequestIpType.both;
            }
            all.put(record.getHost(), type);
        }
        return all;
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
