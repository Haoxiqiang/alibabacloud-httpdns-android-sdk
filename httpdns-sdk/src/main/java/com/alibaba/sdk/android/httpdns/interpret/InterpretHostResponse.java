package com.alibaba.sdk.android.httpdns.interpret;

import com.alibaba.sdk.android.httpdns.RequestIpType;
import com.alibaba.sdk.android.httpdns.utils.CommonUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;

/**
 * 域名解析服务返回的数据结构
 *
 * @author zonglin.nzl
 */
public class InterpretHostResponse {
    private String hostName;
    private String[] ips;
    private String[] ipsv6;
    private int ttl;
    private String extra;

    /**
     * 数据解析
     *
     * @param jsonString
     * @return
     * @throws JSONException
     */
    public static InterpretHostResponse fromResponse(String jsonString) throws JSONException {
        JSONObject jsonObject = new JSONObject(jsonString);
        String hostName = jsonObject.getString("host");
        String[] ips = null;
        String[] ipsv6 = null;
        String extra = null;
        int ttl = 0;
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
            if (jsonObject.has("ipsv6")) {
                JSONArray ipsv6Array = jsonObject.getJSONArray("ipsv6");
                if (ipsv6Array != null && ipsv6Array.length() != 0) {
                    ipsv6 = new String[ipsv6Array.length()];
                    for (int i = 0; i < ipsv6Array.length(); i++) {
                        ipsv6[i] = ipsv6Array.getString(i);
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
        ttl = jsonObject.getInt("ttl");
        return new InterpretHostResponse(hostName, ips, ipsv6, ttl, extra);
    }

    public static InterpretHostResponse createEmpty(String host, int ttl) {
        return new InterpretHostResponse(host, null, null, ttl, null);
    }

    public InterpretHostResponse(String hostName, String[] ips, String[] ipsv6, int ttl, String extra) {
        this.hostName = hostName;
        if (ips != null) {
            this.ips = ips;
        } else {
            this.ips = new String[0];
        }
        if (ipsv6 != null) {
            this.ipsv6 = ipsv6;
        } else {
            this.ipsv6 = new String[0];
        }
        if (ttl > 0) {
            this.ttl = ttl;
        } else {
            this.ttl = 60;
        }
        this.extra = extra;
    }

    public String getHostName() {
        return hostName;
    }

    public String[] getIps() {
        return ips;
    }

    public String[] getIpsv6() {
        return ipsv6;
    }

    public int getTtl() {
        return ttl;
    }

    public String getExtras() {
        return extra;
    }

    @Override
    public String toString() {
        String ret = "host: " + hostName + " ip cnt: " + (ips != null ? ips.length : 0) + " ttl: " + ttl;
        if (ips != null) {
            for (int i = 0; i < ips.length; i++) {
                ret += "\n ip: " + ips[i];
            }
        }
        ret += "\n ipv6 cnt: " + (ipsv6 != null ? ipsv6.length : 0);
        if (ipsv6 != null) {
            for (int i = 0; i < ipsv6.length; i++) {
                ret += "\n ipv6: " + ipsv6[i];
            }
        }
        ret += "\n extra: " + extra;
        return ret;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        InterpretHostResponse that = (InterpretHostResponse) o;
        return ttl == that.ttl &&
                hostName.equals(that.hostName) &&
                Arrays.equals(ips, that.ips) &&
                Arrays.equals(ipsv6, that.ipsv6) &&
                CommonUtil.equals(extra, that.extra);
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(new Object[]{hostName, ttl, extra});
        result = 31 * result + Arrays.hashCode(ips);
        result = 31 * result + Arrays.hashCode(ipsv6);
        return result;
    }
}
