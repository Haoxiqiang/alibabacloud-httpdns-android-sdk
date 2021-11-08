package com.alibaba.sdk.android.httpdns.interpret;

import com.alibaba.sdk.android.httpdns.HTTPDNSResult;
import com.alibaba.sdk.android.httpdns.RequestIpType;
import com.alibaba.sdk.android.httpdns.cache.HostRecord;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author zonglin.nzl
 * @date 11/8/21
 */
public class InterpretHostCache {

    private ConcurrentHashMap<String, HostRecord> v4Records = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, HostRecord> v6Records = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, HTTPDNSResult> v4HttpDnsResults = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, HTTPDNSResult> v6HttpDnsResults = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, HTTPDNSResult> bothHttpDnsResults = new ConcurrentHashMap<>();

    public HTTPDNSResult getResult(String host, RequestIpType type) {
        HTTPDNSResult result = null;
        switch (type) {
            case v6:
                result = v6HttpDnsResults.get(host);
                break;
            case v4:
                result = v4HttpDnsResults.get(host);
                break;
            case both:
                result = bothHttpDnsResults.get(host);
                break;
        }
        if (result == null) {
            result = buildHttpResult(host, type);
            if (result != null) {
                switch (type) {
                    case v6:
                        v6HttpDnsResults.put(host, result);
                        break;
                    case v4:
                        v4HttpDnsResults.put(host, result);
                        break;
                    case both:
                        bothHttpDnsResults.put(host, result);
                        break;
                }

            }
        }
        return result;
    }

    private HTTPDNSResult buildHttpResult(String host, RequestIpType type) {
        HostRecord record;
        HTTPDNSResult result = null;
        switch (type) {
            case v6:
                record = v6Records.get(host);
                if (record != null) {
                    result = new HTTPDNSResult(host);
                    result.update(record);
                }
                break;
            case v4:
                record = v4Records.get(host);
                if (record != null) {
                    result = new HTTPDNSResult(host);
                    result.update(record);
                }
                break;
            default:
                record = v4Records.get(host);
                HostRecord recordv6 = v6Records.get(host);
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

    public HostRecord update(String host, RequestIpType type, String extra, String cacheKey, String[] ips, int ttl) {
        HostRecord record = null;
        switch (type) {
            case v4:
                record = v4Records.get(host);
                if (record == null) {
                    record = HostRecord.create(host, type, extra, cacheKey, ips, ttl);
                    v4Records.put(host, record);
                } else {
                    record.setQueryTime(System.currentTimeMillis());
                    record.setIps(ips);
                    record.setTtl(ttl);
                    record.setExtra(extra);
                    record.setFromDB(false);
                }
                break;
            case v6:
                record = v6Records.get(host);
                if (record == null) {
                    record = HostRecord.create(host, type, extra, cacheKey, ips, ttl);
                    v6Records.put(host, record);
                } else {
                    record.setQueryTime(System.currentTimeMillis());
                    record.setIps(ips);
                    record.setTtl(ttl);
                    record.setExtra(extra);
                    record.setFromDB(false);
                }
                break;
            default:
                throw new IllegalStateException("type should be v4 or b6");
        }
        updateResult(host, type, record);
        return record;
    }

    private void updateResult(String host, RequestIpType type, HostRecord record) {
        if (type == RequestIpType.v4) {
            HTTPDNSResult result = v4HttpDnsResults.get(host);
            if (result != null) {
                result.update(record);
            }
        } else if (type == RequestIpType.v6) {
            HTTPDNSResult result = v6HttpDnsResults.get(host);
            if (result != null) {
                result.update(record);
            }
        }
        HTTPDNSResult result = bothHttpDnsResults.get(host);
        if (result != null) {
            result.update(record);
        }
    }

    public void put(HostRecord record) {
        if (record.getType() == RequestIpType.v4.ordinal()) {
            v4Records.put(record.getHost(), record);
        } else if (record.getType() == RequestIpType.v6.ordinal()) {
            v6Records.put(record.getHost(), record);
        }
    }

    public HostRecord updateIps(String host, RequestIpType type, String[] ips) {
        HostRecord record = null;
        switch (type) {
            case v4:
                record = v4Records.get(host);
                if (record == null) {
                    return null;
                }
                record.setIps(ips);
                break;
            case v6:
                record = v6Records.get(host);
                if (record == null) {
                    return null;
                }
                record.setIps(ips);
                break;
            default:
                throw new IllegalStateException("type should be v4 or b6");
        }
        updateResultIps(host, type, ips);
        return record;
    }

    private void updateResultIps(String host, RequestIpType type, String[] ips) {
        if (type == RequestIpType.v4) {
            HTTPDNSResult result = v4HttpDnsResults.get(host);
            if (result != null) {
                result.updateIps(ips, type);
            }
        } else if (type == RequestIpType.v6) {
            HTTPDNSResult result = v6HttpDnsResults.get(host);
            if (result != null) {
                result.updateIps(ips, type);
            }
        }
        HTTPDNSResult result = bothHttpDnsResults.get(host);
        if (result != null) {
            result.updateIps(ips, type);
        }
    }

    public List<HostRecord> clear() {
        ArrayList<HostRecord> list = new ArrayList<>();
        list.addAll(v4Records.values());
        list.addAll(v6Records.values());
        v4Records.clear();
        v6Records.clear();
        v4HttpDnsResults.clear();
        v6HttpDnsResults.clear();
        bothHttpDnsResults.clear();
        return list;
    }

    public List<HostRecord> clear(List<String> hosts) {
        ArrayList<HostRecord> records = new ArrayList<>();
        for (String host : hosts) {
            HostRecord tmp = v4Records.remove(host);
            if (tmp != null) {
                records.add(tmp);
            }
        }
        for (String host : hosts) {
            HostRecord tmp = v6Records.remove(host);
            if (tmp != null) {
                records.add(tmp);
            }
        }
        for (String host : hosts) {
            v4HttpDnsResults.remove(host);
            v6HttpDnsResults.remove(host);
            bothHttpDnsResults.remove(host);
        }
        return records;
    }

    public HashMap<String, RequestIpType> getAllHost() {
        HashMap<String, RequestIpType> all = new HashMap<>();
        for (HostRecord record : v4Records.values()) {
            all.put(record.getHost(), RequestIpType.v4);
        }
        for (HostRecord record : v6Records.values()) {
            RequestIpType type = all.get(record.getHost());
            if (type == null) {
                all.put(record.getHost(), RequestIpType.v6);
            } else {
                all.put(record.getHost(), RequestIpType.both);
            }
        }
        return all;
    }
}
