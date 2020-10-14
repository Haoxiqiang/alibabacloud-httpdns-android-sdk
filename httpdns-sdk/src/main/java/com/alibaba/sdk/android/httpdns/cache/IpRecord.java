package com.alibaba.sdk.android.httpdns.cache;

/**
 * Created by tomchen on 2017/4/26.
 */

public class IpRecord {

    public long id;

    public long host_id;

    public String ip;

    public String ttl;

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(128);
        sb.append("[IpRecord] ");
        sb.append("id:");
        sb.append(id);
        sb.append("|");
        sb.append("host_id:");
        sb.append(host_id);
        sb.append("|");
        sb.append("ip:");
        sb.append(ip);
        sb.append("|");
        sb.append("ttl:");
        sb.append(ttl);
        sb.append("|");
        return sb.toString();
    }
}
