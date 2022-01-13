package com.alibaba.sdk.android.httpdns.config;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

import com.alibaba.sdk.android.httpdns.BuildConfig;
import com.alibaba.sdk.android.httpdns.impl.HttpDnsConfig;
import com.alibaba.sdk.android.httpdns.utils.CommonUtil;

import java.util.Arrays;

/**
 * 服务节点配置
 * 维护 服务节点的一些状态
 *
 * @author zonglin.nzl
 * @date 1/12/22
 */
public class ServerConfig {

    private HttpDnsConfig config;
    private String[] serverIps;
    private int[] ports;
    private int lastOkServerIndex = 0;
    private int currentServerIndex = 0;
    private String currentServerRegion = null;
    private long serverIpsLastUpdatedTime = 0;

    public ServerConfig(HttpDnsConfig config, String[] serverIps, int[] ports) {
        this.config = config;
        this.serverIps = serverIps;
        this.ports = ports;
        readFromCache(config.getContext(), this);
    }

    public String getCurrentServerRegion() {
        return currentServerRegion;
    }

    public String[] getCurrentServerIps() {
        return serverIps;
    }

    public int[] getPorts() {
        return ports;
    }

    /**
     * 获取当前使用的服务IP
     *
     * @return
     */
    public String getServerIp() {
        if (serverIps == null || currentServerIndex >= serverIps.length || currentServerIndex < 0) {
            return null;
        }
        return serverIps[currentServerIndex];
    }

    /**
     * 获取当前使用的服务端口
     *
     * @return
     */
    public int getPort() {
        if (ports == null || currentServerIndex >= ports.length || currentServerIndex < 0) {
            return CommonUtil.getPort(-1, config.getSchema());
        }
        return CommonUtil.getPort(ports[currentServerIndex], config.getSchema());
    }

    /**
     * 是否应该更新服务IP
     *
     * @return
     */
    public boolean shouldUpdateServerIp() {
        return System.currentTimeMillis() - serverIpsLastUpdatedTime >= 24 * 60 * 60 * 1000 && serverIps != null && serverIps.length > 0;
    }


    /**
     * 设置服务IP
     *
     * @param serverIps
     * @param ports
     * @return false 表示 前后服务一直，没有更新
     */
    public boolean setServerIps(String region, String[] serverIps, int[] ports) {
        if (CommonUtil.isSameServer(this.serverIps, this.ports, serverIps, ports)) {
            return false;
        }
        this.currentServerRegion = region;
        this.serverIps = serverIps;
        this.ports = ports;
        this.lastOkServerIndex = 0;
        this.currentServerIndex = 0;
        if (!Arrays.equals(serverIps, config.getInitServerIps())) {
            // 非初始化IP，才认为是真正的更新了服务IP
            this.serverIpsLastUpdatedTime = System.currentTimeMillis();
        }
        saveToCache(config.getContext(), this);
        return true;
    }


    /**
     * 切换域名解析服务
     *
     * @param ip   请求失败的服务IP
     * @param port 请求失败的服务端口
     * @return
     */
    public boolean shiftServer(String ip, int port) {
        if (serverIps == null) {
            return false;
        }
        if (!(ip.equals(serverIps[currentServerIndex]) && (ports == null || ports[currentServerIndex] == port))) {
            return false;
        }
        currentServerIndex++;
        if (currentServerIndex >= serverIps.length) {
            currentServerIndex = 0;
        }
        return currentServerIndex == lastOkServerIndex;
    }

    /**
     * 标记当前好用的域名解析服务
     *
     * @param serverIp
     * @param port
     * @return 标记成功与否
     */
    public boolean markOkServer(String serverIp, int port) {
        if (serverIps == null) {
            return false;
        }
        if (serverIps[currentServerIndex].equals(serverIp) && (ports == null || ports[currentServerIndex] == port)) {
            lastOkServerIndex = currentServerIndex;
            saveToCache(config.getContext(), this);
            return true;
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServerConfig that = (ServerConfig) o;
        return lastOkServerIndex == that.lastOkServerIndex &&
                currentServerIndex == that.currentServerIndex &&
                serverIpsLastUpdatedTime == that.serverIpsLastUpdatedTime &&
                Arrays.equals(serverIps, that.serverIps) &&
                Arrays.equals(ports, that.ports) &&
                currentServerRegion.equals(that.currentServerRegion);
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(new Object[]{lastOkServerIndex, currentServerIndex, currentServerRegion, serverIpsLastUpdatedTime});
        result = 31 * result + Arrays.hashCode(serverIps);
        result = 31 * result + Arrays.hashCode(ports);
        return result;
    }

    private static final String CONFIG_CACHE_PREFIX = "httpdns_config_server_";
    private static final String CONFIG_KEY_SERVERS = "serverIps";
    private static final String CONFIG_KEY_PORTS = "ports";
    private static final String CONFIG_CURRENT_INDEX = "current";
    private static final String CONFIG_LAST_INDEX = "last";
    private static final String CONFIG_SERVERS_LAST_UPDATED_TIME = "servers_last_updated_time";
    private static final String CONFIG_CURRENT_SERVER_REGION = "server_region";

    private static void readFromCache(Context context, ServerConfig config) {
        SharedPreferences sp = context.getSharedPreferences(CONFIG_CACHE_PREFIX + config.config.getAccountId(), Context.MODE_PRIVATE);
        config.serverIps = CommonUtil.parseStringArray(sp.getString(CONFIG_KEY_SERVERS, CommonUtil.translateStringArray(BuildConfig.INIT_SERVER)));
        config.ports = CommonUtil.parseIntArray(sp.getString(CONFIG_KEY_PORTS, null));
        config.currentServerIndex = sp.getInt(CONFIG_CURRENT_INDEX, 0);
        config.lastOkServerIndex = sp.getInt(CONFIG_LAST_INDEX, 0);
        config.serverIpsLastUpdatedTime = sp.getLong(CONFIG_SERVERS_LAST_UPDATED_TIME, 0);
        config.currentServerRegion = sp.getString(CONFIG_CURRENT_SERVER_REGION, null);
    }

    @SuppressLint("ApplySharedPref")
    private static void saveToCache(Context context, ServerConfig config) {
        SharedPreferences.Editor editor = context.getSharedPreferences(CONFIG_CACHE_PREFIX + config.config.getAccountId(), Context.MODE_PRIVATE).edit();
        editor.putString(CONFIG_KEY_SERVERS, CommonUtil.translateStringArray(config.serverIps));
        editor.putString(CONFIG_KEY_PORTS, CommonUtil.translateIntArray(config.ports));
        editor.putInt(CONFIG_CURRENT_INDEX, config.currentServerIndex);
        editor.putInt(CONFIG_LAST_INDEX, config.lastOkServerIndex);
        editor.putLong(CONFIG_SERVERS_LAST_UPDATED_TIME, config.serverIpsLastUpdatedTime);
        editor.putString(CONFIG_CURRENT_SERVER_REGION, config.currentServerRegion);
        // 虽然提示建议使用apply，但是实践证明，apply是把写文件操作推迟到了一些界面切换等时机，反而影响了UI线程。不如直接在子线程写文件
        editor.commit();
    }
}
