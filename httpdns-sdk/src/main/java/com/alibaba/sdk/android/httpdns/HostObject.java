package com.alibaba.sdk.android.httpdns;

import android.text.Html;

import com.alibaba.sdk.android.httpdns.cache.DBCacheManager;
import com.alibaba.sdk.android.httpdns.cache.DBCacheUtils;
import com.alibaba.sdk.android.httpdns.cache.HostRecord;
import com.alibaba.sdk.android.httpdns.cache.IpRecord;
import com.alibaba.sdk.android.httpdns.net64.Net64Mgr;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

class HostObject {
    private String hostName;
    private String[] ips;
    private long ttl;
    private long queryTime;
    private String extra;
    private String cacheKey;

    /*
     * 此处异常不能在构造函数内部消化
     * json解析异常，HostObject对象字段缺失，比如getIps()可能会返回Null，导致crash
     * HostObject构造错误说明解析结果有误，由上层解析流程处理
     * */
    HostObject(String jsonString) throws Exception {
        JSONObject jsonObject = new JSONObject(jsonString);
        hostName = jsonObject.getString("host");
        try {
            if (jsonObject.has("ips")) {
                JSONArray ipsArray = jsonObject.getJSONArray("ips");
                int len = ipsArray.length();
                ips = new String[len];
                for (int i = 0; i < len; i++) {
                    ips[i] = ipsArray.getString(i);
                }
            }
            // ipv6解析逻辑
            if (Net64Mgr.getInstance().isEnable()) {
                if (jsonObject.has("ipsv6")) {
                    JSONArray ipsv6Array = jsonObject.getJSONArray("ipsv6");
                    if (ipsv6Array != null) {
                        List<String> ipsv6 = new ArrayList<>();
                        for (int i = 0; i < ipsv6Array.length(); i++) {
                            ipsv6.add(ipsv6Array.getString(i));
                        }
                        Net64Mgr.getInstance().put(hostName, ipsv6);
                    }
                }
            }
            //额外参数解析逻辑
            if (jsonObject.has("extra")) {
                extra = jsonObject.getString("extra");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        ttl = jsonObject.getLong("ttl");
        queryTime = System.currentTimeMillis() / 1000;
    }

    HostObject(HostRecord r) {
        hostName = r.host;
        queryTime = DBCacheUtils.getLongSafely(r.time);
        // 优化：ttl时间设置为-1000
        ttl = -1000;
        if (r.ips != null && r.ips.size() > 0) {
            final int N = r.ips.size();
            ips = new String[N];
            for (int i = 0; i < N; i++) {
                ips[i] = r.ips.get(i).ip;
            }
        }

        if (Net64Mgr.getInstance().isEnable()) {
            final ArrayList<String> V6_LIST = new ArrayList<>();
            if (r.ipsv6 != null && r.ipsv6.size() > 0) {
                for (int i = 0; i < r.ipsv6.size(); i++) {
                    V6_LIST.add(r.ipsv6.get(i).ip);
                }
            }
            Net64Mgr.getInstance().put(hostName, V6_LIST);
        }
        extra = r.extra;
        cacheKey = r.cacheKey;
    }

    HostObject(String hostName, String[] ips, long ttl, long queryTime, String extra, String cacheKey) {
        this.hostName = hostName;
        this.ips = ips;
        this.ttl = ttl;
        this.queryTime = queryTime;
        this.extra = extra;
        this.cacheKey = cacheKey;
    }

    HostRecord toHostRecord() {
        HostRecord r = new HostRecord();
        r.host = hostName;
        r.time = String.valueOf(queryTime);
        r.sp = DBCacheManager.getSP();

        if (ips != null && ips.length > 0) {
            r.ips = new ArrayList<>();
            for (String ip : ips) {
                IpRecord ipRecord = new IpRecord();
                ipRecord.ip = ip;
                ipRecord.ttl = String.valueOf(ttl);
                r.ips.add(ipRecord);
            }
        }

        if (Net64Mgr.getInstance().isEnable()) {
            List<String> ipv6List = Net64Mgr.getInstance().get(hostName);
            if (ipv6List != null && ipv6List.size() > 0) {
                r.ipsv6 = new ArrayList<>();
                for (String ipv6 : ipv6List) {
                    IpRecord ipv6Record = new IpRecord();
                    ipv6Record.ip = ipv6;
                    ipv6Record.ttl = String.valueOf(ttl);
                    r.ipsv6.add(ipv6Record);
                }
            }
        }

        r.extra = extra;
        r.cacheKey = cacheKey;
        return r;
    }


    String[] getIps() {
        return ips;
    }

    long getTtl() {
        return ttl;
    }

    long getQueryTime() {
        return queryTime;
    }

    String getExtras() {
        return extra;
    }

    Map<String, String> getExtra() {
        Map<String, String> extras = new HashMap<>();
        if (extra != null) {
            try {
                JSONObject jsonObjectExtra = new JSONObject((Html.fromHtml((Html.fromHtml(extra)).toString())).toString());
                Iterator var2 = jsonObjectExtra.keys();
                while (var2.hasNext()) {
                    String var3 = (String) var2.next();
                    extras.put(var3, jsonObjectExtra.get(var3) == null ? null : jsonObjectExtra.get(var3).toString());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return extras;
    }

    void setCacheKey(String cacheKey) {
        this.cacheKey = cacheKey;
    }

    String getCacheKey() {
        return cacheKey;
    }

    @Override
    public String toString() {
        String ret = "host: " + hostName + " ip cnt: " + ips.length + " ttl: " + ttl;
        for (int i = 0; i < ips.length; i++) {
            ret += "\n ip: " + ips[i];
        }
        return ret;
    }

    boolean isExpired() {
        return this.getQueryTime() + this.getTtl() < System.currentTimeMillis() / 1000
                || isFromCache();
    }

    boolean isFromCache() {
        return this.getTtl() == -1000;
    }
}
