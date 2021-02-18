package com.alibaba.sdk.android.httpdns.serverip;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;

/**
 * 更新服务IP返回接口
 *
 * @author zonglin.nzl
 * @date 2020/10/19
 */
public class UpdateServerResponse {
    private boolean enable;
    private String[] serverIps;
    private String[] serverIpv6s;
    private int[] serverPorts;
    private int[] serverIpv6Ports;

    public UpdateServerResponse(String[] serverIps, String[] serverIpv6s, int[] serverPorts, int[] serverIpv6Ports) {
        this.serverIps = serverIps;
        this.serverIpv6s = serverIpv6s;
        this.serverPorts = serverPorts;
        this.serverIpv6Ports = serverIpv6Ports;
    }

    public UpdateServerResponse(boolean enable, String[] serverIps, String[] serverIpv6s, int[] serverPorts, int[] serverIpv6Ports) {
        this.enable = enable;
        this.serverIps = serverIps;
        this.serverIpv6s = serverIpv6s;
        this.serverPorts = serverPorts;
        this.serverIpv6Ports = serverIpv6Ports;
    }

    public String[] getServerIps() {
        return serverIps;
    }

    public void setServerIps(String[] serverIps) {
        this.serverIps = serverIps;
    }

    public String[] getServerIpv6s() {
        return serverIpv6s;
    }

    public void setServerIpv6s(String[] serverIpv6s) {
        this.serverIpv6s = serverIpv6s;
    }

    public int[] getServerPorts() {
        return serverPorts;
    }

    public void setServerPorts(int[] serverPorts) {
        this.serverPorts = serverPorts;
    }

    public int[] getServerIpv6Ports() {
        return serverIpv6Ports;
    }

    public void setServerIpv6Ports(int[] serverIpv6Ports) {
        this.serverIpv6Ports = serverIpv6Ports;
    }

    public boolean isEnable() {
        return enable;
    }

    public static UpdateServerResponse fromResponse(String response) throws JSONException {
        JSONObject jsonObject = new JSONObject(response);
        boolean enable = true;
        if (jsonObject.has("service_status")) {
            enable = !jsonObject.optString("service_status").equals("disable");
        }
        String[] ips = null;
        if (jsonObject.has("service_ip")) {
            JSONArray ipsArray = jsonObject.getJSONArray("service_ip");
            int len = ipsArray.length();
            ips = new String[len];
            for (int i = 0; i < len; i++) {
                ips[i] = ipsArray.getString(i);
            }
        }
        String[] ipv6s = null;
        if (jsonObject.has("service_ipv6")) {
            JSONArray ipsArray = jsonObject.getJSONArray("service_ipv6");
            int len = ipsArray.length();
            ipv6s = new String[len];
            for (int i = 0; i < len; i++) {
                ipv6s[i] = ipsArray.getString(i);
            }
        }
        int[] ports = null;
        if (jsonObject.has("service_ip_port")) {
            JSONArray ipsArray = jsonObject.getJSONArray("service_ip_port");
            int len = ipsArray.length();
            ports = new int[len];
            for (int i = 0; i < len; i++) {
                ports[i] = ipsArray.optInt(i);
            }
        }
        int[] v6Ports = null;
        if (jsonObject.has("service_ipv6_port")) {
            JSONArray ipsArray = jsonObject.getJSONArray("service_ipv6_port");
            int len = ipsArray.length();
            v6Ports = new int[len];
            for (int i = 0; i < len; i++) {
                v6Ports[i] = ipsArray.optInt(i);
            }
        }
        return new UpdateServerResponse(enable, ips, ipv6s, ports, v6Ports);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UpdateServerResponse that = (UpdateServerResponse) o;
        return enable == that.enable &&
                Arrays.equals(serverIps, that.serverIps) &&
                Arrays.equals(serverIpv6s, that.serverIpv6s) &&
                Arrays.equals(serverPorts, that.serverPorts) &&
                Arrays.equals(serverIpv6Ports, that.serverIpv6Ports);
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(new Object[]{enable});
        result = 31 * result + Arrays.hashCode(serverIps);
        result = 31 * result + Arrays.hashCode(serverIpv6s);
        result = 31 * result + Arrays.hashCode(serverPorts);
        result = 31 * result + Arrays.hashCode(serverIpv6Ports);
        return result;
    }

    @Override
    public String toString() {
        return "UpdateServerResponse{" +
                "enable=" + enable +
                ", serverIps=" + Arrays.toString(serverIps) +
                ", serverIpv6s=" + Arrays.toString(serverIpv6s) +
                ", serverPorts=" + Arrays.toString(serverPorts) +
                ", serverIpv6Ports=" + Arrays.toString(serverIpv6Ports) +
                '}';
    }
}
