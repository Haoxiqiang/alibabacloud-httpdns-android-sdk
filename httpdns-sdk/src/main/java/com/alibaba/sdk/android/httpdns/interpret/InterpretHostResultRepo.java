package com.alibaba.sdk.android.httpdns.interpret;

import com.alibaba.sdk.android.httpdns.HTTPDNSResult;
import com.alibaba.sdk.android.httpdns.RequestIpType;
import com.alibaba.sdk.android.httpdns.cache.HostRecord;
import com.alibaba.sdk.android.httpdns.cache.RecordDBHelper;
import com.alibaba.sdk.android.httpdns.impl.HttpDnsConfig;
import com.alibaba.sdk.android.httpdns.log.HttpDnsLog;
import com.alibaba.sdk.android.httpdns.probe.ProbeService;
import com.alibaba.sdk.android.httpdns.probe.ProbeCallback;
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
    private RecordDBHelper dbHelper;
    private boolean enableCache = false;
    private HttpDnsConfig config;
    private ProbeService ipProbeService;

    public InterpretHostResultRepo(HttpDnsConfig config, ProbeService ipProbeService) {
        dbHelper = new RecordDBHelper(config.getContext(), config.getAccountId());
        this.config = config;
        this.ipProbeService = ipProbeService;
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
                records.add(save(host, RequestIpType.v4, extra, cacheKey, response.getIps(), response.getTtl()));
                records.add(save(host, RequestIpType.v6, extra, cacheKey, response.getIpsv6(), response.getTtl()));
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

        String key;
        HostRecord record;
        switch (type) {
            case v6:
                key = CommonUtil.formKey(host, RequestIpType.v6, cacheKey);
                record = interpretResults.get(key);
                if (record == null) {
                    return null;
                }
                return new HTTPDNSResult(host, new String[0], record.getIps(), CommonUtil.toMap(record.getExtra()), record.isExpired(), record.isFromDB());
            case v4:
                key = CommonUtil.formKey(host, RequestIpType.v4, cacheKey);
                record = interpretResults.get(key);
                if (record == null) {
                    return null;
                }
                return new HTTPDNSResult(host, record.getIps(), new String[0], CommonUtil.toMap(record.getExtra()), record.isExpired(), record.isFromDB());
            default:
                key = CommonUtil.formKey(host, RequestIpType.v4, cacheKey);
                String keyv6 = CommonUtil.formKey(host, RequestIpType.v6, cacheKey);
                record = interpretResults.get(key);
                HostRecord recordv6 = interpretResults.get(keyv6);
                if (record == null || recordv6 == null) {
                    return null;
                }
                if (!CommonUtil.equals(record.getExtra(), recordv6.getExtra())) {
                    HttpDnsLog.w("extra is not same for v4 and v6");
                }
                String extra = record.getExtra() != null ? record.getExtra() : recordv6.getExtra();
                boolean expired = record.isExpired() || recordv6.isExpired();
                return new HTTPDNSResult(host, record.getIps(), recordv6.getIps(), CommonUtil.toMap(extra), expired, record.isFromDB() || recordv6.isFromDB());
        }
    }


    /**
     * 清除已解析的结果
     */
    public void clear() {
        if (enableCache) {
            dbHelper.delete(new ArrayList<HostRecord>(interpretResults.values()));
        }
        interpretResults.clear();
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
