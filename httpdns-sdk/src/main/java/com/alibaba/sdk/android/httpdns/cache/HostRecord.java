package com.alibaba.sdk.android.httpdns.cache;

import java.util.ArrayList;

/**
 * Created by tomchen on 2017/4/26.
 */

public class HostRecord {

    public long id;

    public String host;

    public String sp;

    public String time;

    public ArrayList<IpRecord> ips;

    public ArrayList<IpRecord> ipsv6;

    public String extra;

    public String cacheKey;

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(128);
        sb.append("[HostRecord] ");
        sb.append("id:");
        sb.append(id);
        sb.append("|");
        sb.append("host:");
        sb.append(host);
        sb.append("|");
        sb.append("sp:");
        sb.append(sp);
        sb.append("|");
        sb.append("time:");
        sb.append(time);
        sb.append("|");
        sb.append("ips:");
        if (ips != null && ips.size() > 0)
            for (IpRecord ip : ips) {
                sb.append(ip);
            }
        sb.append("|");
        sb.append("ipsv6:");
        if (ipsv6 != null && ipsv6.size() > 0)
            for (IpRecord ip : ipsv6) {
                sb.append(ip);
            }
        sb.append("|");
        sb.append("extra:");
        sb.append(extra);
        sb.append("|");
        sb.append("cacheKey:");
        sb.append(cacheKey);
        sb.append("|");
        return sb.toString();
    }
}
