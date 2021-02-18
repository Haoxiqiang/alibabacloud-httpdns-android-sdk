package com.alibaba.sdk.android.httpdns;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * sdns的解析结果
 */
public class HTTPDNSResult {
    String host;
    String[] ips;
    String[] ipv6s;
    Map<String, String> extra;
    boolean expired = false;
    boolean fromDB = false;

    public HTTPDNSResult(String host, String[] ips, String[] ipv6s, Map<String, String> extra, boolean expired, boolean isFromDB) {
        this.host = host;
        this.ips = ips;
        this.ipv6s = ipv6s;
        this.extra = extra;
        this.expired = expired;
        this.fromDB = isFromDB;
    }

    public static HTTPDNSResult empty(String host) {
        return new HTTPDNSResult(host, new String[0], new String[0], new HashMap<String, String>(), false, false);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("host:");
        sb.append(host);
        sb.append(", ips:");
        sb.append(Arrays.toString(ips));
        sb.append(", ipv6s:");
        sb.append(Arrays.toString(ipv6s));
        sb.append(", extras:");
        sb.append(extra);
        sb.append(", expired:");
        sb.append(expired);
        sb.append(", fromDB:");
        sb.append(fromDB);
        return sb.toString();
    }

    public String getHost() {
        return host;
    }

    public String[] getIps() {
        return ips;
    }

    public String[] getIpv6s() {
        return ipv6s;
    }

    public Map<String, String> getExtras() {
        return extra;
    }

    public boolean isExpired() {
        return expired;
    }

    public boolean isFromDB() {
        return fromDB;
    }
}
