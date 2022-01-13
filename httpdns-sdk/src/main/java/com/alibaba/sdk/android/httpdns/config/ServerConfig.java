package com.alibaba.sdk.android.httpdns.config;

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
public class ServerConfig extends RegionServer implements SpCacheItem {

    private final HttpDnsConfig config;
    private int lastOkServerIndex = 0;
    private int currentServerIndex = 0;
    private long serverIpsLastUpdatedTime = 0;

    public ServerConfig(HttpDnsConfig config) {
        super(config.getInitServer().getServerIps(), config.getInitServer().getPorts(), config.getInitServer().getRegion());
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
        // 这里判断serverIp是否存在 其实 在测试场景才会生效
        return System.currentTimeMillis() - serverIpsLastUpdatedTime >= 24 * 60 * 60 * 1000 && getServerIps() != null && getServerIps().length > 0;
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
                // 非初始IP才有缓存的必要
                config.saveToCache();
            }
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
            if (lastOkServerIndex != currentServerIndex) {
                lastOkServerIndex = currentServerIndex;
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
                serverIpsLastUpdatedTime == that.serverIpsLastUpdatedTime &&
                config.equals(that.config);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(new Object[]{super.hashCode(), config, lastOkServerIndex, currentServerIndex, serverIpsLastUpdatedTime});
    }

    @Override
    public void restoreFromCache(SharedPreferences sp) {
        String[] serverIps = CommonUtil.parseStringArray(sp.getString(Constants.CONFIG_KEY_SERVERS, CommonUtil.translateStringArray(BuildConfig.INIT_SERVER)));
        int[] ports = CommonUtil.parsePorts(sp.getString(Constants.CONFIG_KEY_PORTS, null));
        String currentServerRegion = sp.getString(Constants.CONFIG_CURRENT_SERVER_REGION, Constants.REGION_DEFAULT);
        updateAll(currentServerRegion, serverIps, ports);
        currentServerIndex = sp.getInt(Constants.CONFIG_CURRENT_INDEX, 0);
        lastOkServerIndex = sp.getInt(Constants.CONFIG_LAST_INDEX, 0);
        serverIpsLastUpdatedTime = sp.getLong(Constants.CONFIG_SERVERS_LAST_UPDATED_TIME, 0);
    }

    @Override
    public void saveToCache(SharedPreferences.Editor editor) {
        editor.putString(Constants.CONFIG_KEY_SERVERS, CommonUtil.translateStringArray(getServerIps()));
        editor.putString(Constants.CONFIG_KEY_PORTS, CommonUtil.translateIntArray(getPorts()));
        editor.putInt(Constants.CONFIG_CURRENT_INDEX, currentServerIndex);
        editor.putInt(Constants.CONFIG_LAST_INDEX, lastOkServerIndex);
        editor.putLong(Constants.CONFIG_SERVERS_LAST_UPDATED_TIME, serverIpsLastUpdatedTime);
        editor.putString(Constants.CONFIG_CURRENT_SERVER_REGION, getRegion());
    }
}
