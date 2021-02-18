package com.alibaba.sdk.android.httpdns.interpret;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * 批量解析的结果
 *
 * @author zonglin.nzl
 * @date 2020/12/9
 */
public class ResolveHostResponse {

    private HashMap<String, HostItem> items = new HashMap<>();

    private ResolveHostResponse(HashMap<String, HostItem> items) {
        this.items = items;
    }

    public ResolveHostResponse(ArrayList<HostItem> items) {
        for (HostItem item : items) {
            this.items.put(item.host, item);
        }
    }

    public HostItem getItem(String host) {
        return items.get(host);
    }

    public List<String> getHosts() {
        return new ArrayList<>(items.keySet());
    }

    public static class HostItem {
        private String host;
        private String[] ips;
        private String[] ipv6s;
        private int ttl;

        public HostItem(String host, String[] ips, String[] ipv6s, int ttl) {
            this.host = host;
            this.ips = ips;
            this.ipv6s = ipv6s;
            if (ttl <= 0) {
                this.ttl = 60;
            } else {
                this.ttl = ttl;
            }
        }

        public String[] getIps() {
            return ips;
        }

        public String[] getIpv6s() {
            return ipv6s;
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
        HashMap<String, HostItem> items = new HashMap<>();
        String host;
        String[] ips = null;
        int type;
        int ttl;
        for (int i = 0; i < dns.length(); i++) {
            JSONObject itemJson = dns.getJSONObject(i);
            host = itemJson.getString("host");
            type = itemJson.getInt("type");
            ttl = itemJson.getInt("ttl");
            if (itemJson.has("ips")) {
                JSONArray ipsArray = itemJson.getJSONArray("ips");
                int len = ipsArray.length();
                ips = new String[len];
                for (int j = 0; j < len; j++) {
                    ips[j] = ipsArray.getString(j);
                }
            }
            HostItem item = items.get(host);
            if (item == null) {
                item = new HostItem(host, null, null, ttl);
                items.put(host, item);
            }
            if (type == 1) {
                item.ips = ips;
            } else if (type == 28) {
                item.ipv6s = ips;
            }
        }
        return new ResolveHostResponse(items);
    }
}
