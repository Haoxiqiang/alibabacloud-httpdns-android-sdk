package com.alibaba.sdk.android.httpdns;

import java.util.Arrays;
import java.util.Map;

public class HTTPDNSResult {
    String host;
    String[] ips;
    Map<String, String> extra;

    HTTPDNSResult(String host, String[] ips, Map<String, String> extra) {
        this.host = host;
        this.ips = ips;
        this.extra = extra;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("host:");
        sb.append(host);
        sb.append(", ips:");
        sb.append(Arrays.toString(ips));
        sb.append(", extras:");
        sb.append(extra);
        return sb.toString();
    }

    public String getHost() {
        return host;
    }

    public String[] getIps() {
        return ips;
    }

    public Map<String, String> getExtras() {
        return extra;
    }
}
