package com.alibaba.sdk.android.httpdns.config;

import android.content.SharedPreferences;

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
public class ServerConfig extends RegionServer implements SpCacheItem {

    private final HttpDnsConfig config;
    private int lastOkServerIndex = 0;
    private int currentServerIndex = 0;
    private long serverIpsLastUpdatedTime = 0;

    private int lastOkServerIndexForV6 = 0;
    private int currentServerIndexForV6 = 0;

    public ServerConfig(HttpDnsConfig config) {
        super(config.getInitServer().getServerIps(), config.getInitServer().getPorts(), config.getInitServer().getIpv6ServerIps(), config.getInitServer().getIpv6Ports(), config.getInitServer().getRegion());
        this.config = config;
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
     * 获取当前使用的服务IP ipv6
     *
     * @return
     */
    public String getServerIpForV6() {
        final String[] serverIps = getIpv6ServerIps();
        if (serverIps == null || currentServerIndexForV6 >= serverIps.length || currentServerIndexForV6 < 0) {
            return null;
        }
        return serverIps[currentServerIndexForV6];
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
     * 获取当前使用的服务端口
     *
     * @return
     */
    public int getPortForV6() {
        final int[] ports = getIpv6Ports();
        if (ports == null || currentServerIndexForV6 >= ports.length || currentServerIndexForV6 < 0) {
            return CommonUtil.getPort(-1, config.getSchema());
        }
        return CommonUtil.getPort(ports[currentServerIndexForV6], config.getSchema());
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
     * @param serverV6Ips
     * @param v6Ports
     * @return false 表示 前后服务一直，没有更新
     */
    public boolean setServerIps(String region, String[] serverIps, int[] ports, String[] serverV6Ips, int[] v6Ports) {
        region = CommonUtil.fixRegion(region);
        if (serverIps == null || serverIps.length == 0) {
            serverIps = config.getInitServer().getServerIps();
            ports = config.getInitServer().getPorts();
        }
        if(serverV6Ips == null || serverV6Ips.length == 0) {
            serverV6Ips = config.getInitServer().getIpv6ServerIps();
            v6Ports = config.getInitServer().getIpv6Ports();
        }
        boolean changed = updateRegionAndIpv4(region, serverIps, ports);
        boolean v6changed = updateIpv6(serverV6Ips, v6Ports);
        if (changed) {
            this.lastOkServerIndex = 0;
            this.currentServerIndex = 0;
        }
        if (v6changed) {
            this.lastOkServerIndexForV6 = 0;
            this.currentServerIndexForV6 = 0;
        }
        if (!CommonUtil.isSameServer(serverIps, ports, config.getInitServer().getServerIps(), config.getInitServer().getPorts())
                || !CommonUtil.isSameServer(serverV6Ips, v6Ports, config.getInitServer().getIpv6ServerIps(), config.getInitServer().getIpv6Ports())) {
            // 非初始化IP，才认为是真正的更新了服务IP
            this.serverIpsLastUpdatedTime = System.currentTimeMillis();
            // 非初始IP才有缓存的必要
            config.saveToCache();
        }
        return changed || v6changed;
    }


    /**
     * 切换域名解析服务
     *
     * @param ip   请求失败的服务IP
     * @param port 请求失败的服务端口
     * @return 是否切换回了最开始的服务。当请求切换的ip和port不是当前ip和port时，说明这个切换请求是无效的，不切换，返回false 认为没有切换回最开始的ip
     */
    public boolean shiftServer(String ip, int port) {
        return shiftServerV4(ip, port);
    }

    private boolean shiftServerV4(String ip, int port) {
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

    public boolean shiftServerV6(String ip, int port) {
        final String[] serverIps = getIpv6ServerIps();
        final int[] ports = getIpv6Ports();
        if (serverIps == null) {
            return false;
        }
        if (!(ip.equals(serverIps[currentServerIndexForV6]) && (ports == null || ports[currentServerIndexForV6] == port))) {
            return false;
        }
        currentServerIndexForV6++;
        if (currentServerIndexForV6 >= serverIps.length) {
            currentServerIndexForV6 = 0;
        }
        return currentServerIndexForV6 == lastOkServerIndexForV6;
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
            if (lastOkServerIndex != currentServerIndex) {
                lastOkServerIndex = currentServerIndex;
                config.saveToCache();
            }
            return true;
        }
        return false;
    }

    /**
     * 标记当前好用的域名解析服务
     *
     * @param serverIp
     * @param port
     * @return 标记成功与否
     */
    public boolean markOkServerV6(String serverIp, int port) {
        final String[] serverIps = getIpv6ServerIps();
        final int[] ports = getIpv6Ports();
        if (serverIps == null) {
            return false;
        }
        if (serverIps[currentServerIndexForV6].equals(serverIp) && (ports == null || ports[currentServerIndexForV6] == port)) {
            if (lastOkServerIndexForV6 != currentServerIndexForV6) {
                lastOkServerIndexForV6 = currentServerIndexForV6;
                config.saveToCache();
            }
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
                lastOkServerIndexForV6 == that.lastOkServerIndexForV6 &&
                currentServerIndexForV6 == that.currentServerIndexForV6 &&
                serverIpsLastUpdatedTime == that.serverIpsLastUpdatedTime &&
                config.equals(that.config);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(new Object[]{super.hashCode(), config, lastOkServerIndex, currentServerIndex, lastOkServerIndexForV6, currentServerIndexForV6, serverIpsLastUpdatedTime});
    }

    @Override
    public void restoreFromCache(SharedPreferences sp) {
        String[] serverIps = CommonUtil.parseStringArray(sp.getString(Constants.CONFIG_KEY_SERVERS, CommonUtil.translateStringArray(getServerIps())));
        int[] ports = CommonUtil.parsePorts(sp.getString(Constants.CONFIG_KEY_PORTS, CommonUtil.translateIntArray(getPorts())));
        String[] serverV6Ips = CommonUtil.parseStringArray(sp.getString(Constants.CONFIG_KEY_SERVERS_IPV6, CommonUtil.translateStringArray(getIpv6ServerIps())));
        int[] v6Ports = CommonUtil.parsePorts(sp.getString(Constants.CONFIG_KEY_PORTS_IPV6, CommonUtil.translateIntArray(getIpv6Ports())));
        String currentServerRegion = sp.getString(Constants.CONFIG_CURRENT_SERVER_REGION, getRegion());
        updateAll(currentServerRegion, serverIps, ports, serverV6Ips, v6Ports);
        currentServerIndex = sp.getInt(Constants.CONFIG_CURRENT_INDEX, 0);
        lastOkServerIndex = sp.getInt(Constants.CONFIG_LAST_INDEX, 0);
        currentServerIndexForV6 = sp.getInt(Constants.CONFIG_CURRENT_INDEX_IPV6, 0);
        lastOkServerIndexForV6 = sp.getInt(Constants.CONFIG_LAST_INDEX_IPV6, 0);
        serverIpsLastUpdatedTime = sp.getLong(Constants.CONFIG_SERVERS_LAST_UPDATED_TIME, 0);
    }

    @Override
    public void saveToCache(SharedPreferences.Editor editor) {
        editor.putString(Constants.CONFIG_KEY_SERVERS, CommonUtil.translateStringArray(getServerIps()));
        editor.putString(Constants.CONFIG_KEY_PORTS, CommonUtil.translateIntArray(getPorts()));
        editor.putInt(Constants.CONFIG_CURRENT_INDEX, currentServerIndex);
        editor.putInt(Constants.CONFIG_LAST_INDEX, lastOkServerIndex);
        editor.putString(Constants.CONFIG_KEY_SERVERS_IPV6, CommonUtil.translateStringArray(getIpv6ServerIps()));
        editor.putString(Constants.CONFIG_KEY_PORTS_IPV6, CommonUtil.translateIntArray(getIpv6Ports()));
        editor.putInt(Constants.CONFIG_CURRENT_INDEX_IPV6, currentServerIndexForV6);
        editor.putInt(Constants.CONFIG_LAST_INDEX_IPV6, lastOkServerIndexForV6);
        editor.putLong(Constants.CONFIG_SERVERS_LAST_UPDATED_TIME, serverIpsLastUpdatedTime);
        editor.putString(Constants.CONFIG_CURRENT_SERVER_REGION, getRegion());
    }
}
