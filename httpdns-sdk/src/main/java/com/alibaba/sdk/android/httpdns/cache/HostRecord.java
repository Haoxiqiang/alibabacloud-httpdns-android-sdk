package com.alibaba.sdk.android.httpdns.cache;

import com.alibaba.sdk.android.httpdns.RequestIpType;
import com.alibaba.sdk.android.httpdns.utils.CommonUtil;

import java.util.Arrays;

/**
 * ip解析结果记录
 * 注意计算hash 和 equal实现，没有使用fromDB字段
 *
 * @author zonglin.nzl
 * @date 2020/12/11
 */
public class HostRecord {
    private long id = -1;
    private String host;
    private String[] ips;
    private int type;
    private int ttl;
    private long queryTime;
    private String extra;
    private String cacheKey;
    private boolean fromDB = false;

    public static HostRecord create(String host, RequestIpType type, String extra, String cacheKey, String[] ips, int ttl) {
        HostRecord record = new HostRecord();
        record.host = host;
        record.type = type.ordinal();
        record.ips = ips;
        record.ttl = ttl;
        record.queryTime = System.currentTimeMillis();
        record.extra = extra;
        record.cacheKey = cacheKey;
        return record;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > queryTime + ttl * 1000;
    }

    public void setFromDB(boolean fromDB) {
        this.fromDB = fromDB;
    }

    public boolean isFromDB() {
        return fromDB;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String[] getIps() {
        return ips;
    }

    public void setIps(String[] ips) {
        this.ips = ips;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getTtl() {
        return ttl;
    }

    public void setTtl(int ttl) {
        this.ttl = ttl;
    }

    public long getQueryTime() {
        return queryTime;
    }

    public void setQueryTime(long queryTime) {
        this.queryTime = queryTime;
    }

    public String getExtra() {
        return extra;
    }

    public void setExtra(String extra) {
        this.extra = extra;
    }

    public String getCacheKey() {
        return cacheKey;
    }

    public void setCacheKey(String cacheKey) {
        this.cacheKey = cacheKey;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HostRecord record = (HostRecord) o;
        return id == record.id &&
                type == record.type &&
                ttl == record.ttl &&
                queryTime == record.queryTime &&
                CommonUtil.equals(host, record.host) &&
                Arrays.equals(ips, record.ips) &&
                CommonUtil.equals(extra, record.extra) &&
                CommonUtil.equals(cacheKey, record.cacheKey);
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(new Object[]{id, host, type, ttl, queryTime, extra, cacheKey});
        result = 31 * result + Arrays.hashCode(ips);
        return result;
    }

    @Override
    public String toString() {
        return "HostRecord{" +
                "id=" + id +
                ", host='" + host + '\'' +
                ", ips=" + Arrays.toString(ips) +
                ", type=" + type +
                ", ttl=" + ttl +
                ", queryTime=" + queryTime +
                ", extra='" + extra + '\'' +
                ", cacheKey='" + cacheKey + '\'' +
                ", fromDB=" + fromDB +
                '}';
    }
}
