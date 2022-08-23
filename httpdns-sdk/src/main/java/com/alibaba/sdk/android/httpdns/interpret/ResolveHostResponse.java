package com.alibaba.sdk.android.httpdns.interpret;

import com.alibaba.sdk.android.httpdns.RequestIpType;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 批量解析的结果
 *
 * @author zonglin.nzl
 * @date 2020/12/9
 */
public class ResolveHostResponse {

    private ArrayList<HostItem> hostItems;

    public ResolveHostResponse(ArrayList<HostItem> items) {
        this.hostItems = items;
    }

    public HostItem getItem(String host, RequestIpType type) {
        for (HostItem item : hostItems) {
            if (item.host.equals(host) && item.type == type) {
                return item;
            }
        }
        return null;
    }

    public List<HostItem> getItems() {
        return hostItems;
    }

    public static class HostItem {
        private String host;
        private RequestIpType type;
        private String[] ips;
        private int ttl;

        public HostItem(String host, RequestIpType type, String[] ips, int ttl) {
            this.host = host;
            this.type = type;
            this.ips = ips;
            if (ttl <= 0) {
                this.ttl = 60;
            } else {
                this.ttl = ttl;
            }
        }

        public String getHost() {
            return host;
        }

        public RequestIpType getType() {
            return type;
        }

        public String[] getIps() {
            return ips;
        }

        public int getTtl() {
            return ttl;
        }
    }


    public static ResolveHostResponse fromResponse(String body) throws JSONException {
        // {"dns":[{"host":"www.taobao.com","ips":["124.239.239.235","124.239.159.105"],"type":1,"ttl":31},{"host":"www.taobao.com","ips":["240e:b1:9801:400:3:0:0:3fa","240e:b1:a820:0:3:0:0:3f6"],"type":28,"ttl":60}]}
        JSONObject jsonObject = new JSONObject(body);
        if (!jsonObject.has("dns")) {
            return null;
        }
        JSONArray dns = jsonObject.getJSONArray("dns");
        ArrayList<HostItem> items = new ArrayList<>();
        String host;
        String[] ips;
        int type;
        int ttl;
        for (int i = 0; i < dns.length(); i++) {
            JSONObject itemJson = dns.getJSONObject(i);
            host = itemJson.getString("host");
            type = itemJson.getInt("type");
            ttl = itemJson.getInt("ttl");
            ips = null;
            if (itemJson.has("ips")) {
                JSONArray ipsArray = itemJson.getJSONArray("ips");
                int len = ipsArray.length();
                ips = new String[len];
                for (int j = 0; j < len; j++) {
                    ips[j] = ipsArray.getString(j);
                }
            }

            HostItem item = null;
            if (type == 1) {
                item = new HostItem(host, RequestIpType.v4, ips, ttl);
            } else if (type == 28) {
                item = new HostItem(host, RequestIpType.v6, ips, ttl);
            }
            if (item != null) {
                items.add(item);
            }
        }
        return new ResolveHostResponse(items);
    }
}
