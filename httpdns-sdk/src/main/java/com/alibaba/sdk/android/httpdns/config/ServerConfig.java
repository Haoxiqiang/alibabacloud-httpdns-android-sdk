package com.alibaba.sdk.android.httpdns.config;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

import com.alibaba.sdk.android.httpdns.BuildConfig;
import com.alibaba.sdk.android.httpdns.impl.HttpDnsConfig;
import com.alibaba.sdk.android.httpdns.utils.CommonUtil;
import com.alibaba.sdk.android.httpdns.utils.Constants;

import java.util.Arrays;

/**
 * 服务节点配置
 * 维护 服务节点的一些状态
 *
 * @author zonglin.nzl
 * @date 1/12/22
 */
public class ServerConfig extends RegionServer {

    private final HttpDnsConfig config;
    private int lastOkServerIndex = 0;
    private int currentServerIndex = 0;
    private long serverIpsLastUpdatedTime = 0;

    public ServerConfig(HttpDnsConfig config) {
        super(config.getInitServer().getServerIps(), config.getInitServer().getPorts(), config.getInitServer().getRegion());
        this.config = config;
        readFromCache(config.getContext(), this);
    }

    /**
     * 获取当前使用的服务IP
     *
     * @return
     */
    public String getServerIp() {
        final String[] serverIps = getServerIps();
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
        final int[] ports = getPorts();
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
        return System.currentTimeMillis() - serverIpsLastUpdatedTime >= 24 * 60 * 60 * 1000;
    }


    /**
     * 设置服务IP
     *
     * @param serverIps
     * @param ports
     * @return false 表示 前后服务一直，没有更新
     */
    public boolean setServerIps(String region, String[] serverIps, int[] ports) {
        region = CommonUtil.fixRegion(region);
        boolean changed = updateAll(region, serverIps, ports);
        if (changed) {
            this.lastOkServerIndex = 0;
            this.currentServerIndex = 0;
            if (!CommonUtil.isSameServer(serverIps, ports, config.getInitServer().getServerIps(), config.getInitServer().getPorts())) {
                // 非初始化IP，才认为是真正的更新了服务IP
                this.serverIpsLastUpdatedTime = System.currentTimeMillis();
            }
            saveToCache(config.getContext(), this);
            return true;
        } else {
            return false;
        }
    }


    /**
     * 切换域名解析服务
     *
     * @param ip   请求失败的服务IP
     * @param port 请求失败的服务端口
     * @return 是否切换回了最开始的服务。当请求切换的ip和port不是当前ip和port时，说明这个切换请求是无效的，不切换，返回false 认为没有切换回最开始的ip
     */
    public boolean shiftServer(String ip, int port) {
        final String[] serverIps = getServerIps();
        final int[] ports = getPorts();
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
        final String[] serverIps = getServerIps();
        final int[] ports = getPorts();
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
        if (!super.equals(o)) return false;
        ServerConfig that = (ServerConfig) o;
        return lastOkServerIndex == that.lastOkServerIndex &&
                currentServerIndex == that.currentServerIndex &&
                serverIpsLastUpdatedTime == that.serverIpsLastUpdatedTime &&
                config.equals(that.config);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(new Object[]{super.hashCode(), config, lastOkServerIndex, currentServerIndex, serverIpsLastUpdatedTime});
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
        String[] serverIps = CommonUtil.parseStringArray(sp.getString(CONFIG_KEY_SERVERS, CommonUtil.translateStringArray(BuildConfig.INIT_SERVER)));
        int[] ports = CommonUtil.parsePorts(sp.getString(CONFIG_KEY_PORTS, null));
        config.currentServerIndex = sp.getInt(CONFIG_CURRENT_INDEX, 0);
        config.lastOkServerIndex = sp.getInt(CONFIG_LAST_INDEX, 0);
        config.serverIpsLastUpdatedTime = sp.getLong(CONFIG_SERVERS_LAST_UPDATED_TIME, 0);
        String currentServerRegion = sp.getString(CONFIG_CURRENT_SERVER_REGION, Constants.REGION_DEFAULT);
        config.updateAll(currentServerRegion, serverIps, ports);
    }

    @SuppressLint("ApplySharedPref")
    private static void saveToCache(Context context, ServerConfig config) {
        SharedPreferences.Editor editor = context.getSharedPreferences(CONFIG_CACHE_PREFIX + config.config.getAccountId(), Context.MODE_PRIVATE).edit();
        editor.putString(CONFIG_KEY_SERVERS, CommonUtil.translateStringArray(config.getServerIps()));
        editor.putString(CONFIG_KEY_PORTS, CommonUtil.translateIntArray(config.getPorts()));
        editor.putInt(CONFIG_CURRENT_INDEX, config.currentServerIndex);
        editor.putInt(CONFIG_LAST_INDEX, config.lastOkServerIndex);
        editor.putLong(CONFIG_SERVERS_LAST_UPDATED_TIME, config.serverIpsLastUpdatedTime);
        editor.putString(CONFIG_CURRENT_SERVER_REGION, config.getRegion());
        // 虽然提示建议使用apply，但是实践证明，apply是把写文件操作推迟到了一些界面切换等时机，反而影响了UI线程。不如直接在子线程写文件
        editor.commit();
    }
}
