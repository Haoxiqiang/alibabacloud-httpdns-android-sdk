package com.alibaba.sdk.android.httpdns.interpret;

import com.alibaba.sdk.android.httpdns.cache.HostRecord;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @author zonglin.nzl
 * @date 11/8/21
 */
public class InterpretHostCacheGroup {

    private final InterpretHostCache defaultCache = new InterpretHostCache();
    private final HashMap<String, InterpretHostCache> sdnsCaches = new HashMap<>();
    private final Object lock = new Object();


    public InterpretHostCache getCache(String cacheKey) {
        if (cacheKey == null || cacheKey.isEmpty()) {
            return defaultCache;
        }
        InterpretHostCache cache = sdnsCaches.get(cacheKey);
        if (cache == null) {
            synchronized (lock) {
                cache = sdnsCaches.get(cacheKey);
                if (cache == null) {
                    cache = new InterpretHostCache();
                    sdnsCaches.put(cacheKey, cache);
                }
            }
        }
        return cache;
    }

    public List<HostRecord> clearAll() {
        ArrayList<HostRecord> records = new ArrayList<>();
        records.addAll(defaultCache.clear());
        if (sdnsCaches.size() > 0) {
            synchronized (lock) {
                for (InterpretHostCache cache : sdnsCaches.values()) {
                    records.addAll(cache.clear());
                }
            }
        }
        return records;
    }

    public List<HostRecord> clearAll(List<String> hosts) {
        ArrayList<HostRecord> records = new ArrayList<>();
        records.addAll(defaultCache.clear(hosts));
        if (sdnsCaches.size() > 0) {
            synchronized (lock) {
                for (InterpretHostCache cache : sdnsCaches.values()) {
                    records.addAll(cache.clear(hosts));
                }
            }
        }
        return records;
    }
}
