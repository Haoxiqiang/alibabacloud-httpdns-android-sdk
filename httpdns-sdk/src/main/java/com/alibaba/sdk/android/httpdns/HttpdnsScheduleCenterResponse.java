package com.alibaba.sdk.android.httpdns;

import org.json.JSONArray;
import org.json.JSONObject;

public class HttpdnsScheduleCenterResponse {
    private static final String SERVICE_TAG = "service_status";
    private static final String SERVICE_IP_TAG = "service_ip";
    private static final String SERVICE_IPV6_TAG = "service_ipv6";
    private boolean enabled = true; //一旦为false,整个httpdns将不可用,默认为true
    private String[] serverIp;

    HttpdnsScheduleCenterResponse(String jsonString) {
        try {
            JSONObject object = new JSONObject(jsonString);
            HttpDnsLog.Logd("StartIp Schedule center response:" + object.toString());
            if (object.has(SERVICE_TAG)) {
                enabled = object.getString(SERVICE_TAG).equals("disable") ? false : true;
            }

            if (object.has(SERVICE_IP_TAG)) {
                JSONArray serverIpArray = object.getJSONArray(SERVICE_IP_TAG);
                serverIp = new String[serverIpArray.length()];
                for (int i = 0; i < serverIpArray.length(); i++) {
                    serverIp[i] = (String) serverIpArray.get(i);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public String[] getServerIps() {
        return this.serverIp;
    }
}
