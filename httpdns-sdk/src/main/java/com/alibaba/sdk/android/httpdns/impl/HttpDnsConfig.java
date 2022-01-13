package com.alibaba.sdk.android.httpdns.impl;

import android.content.Context;
import android.content.SharedPreferences;

import com.alibaba.sdk.android.httpdns.BuildConfig;
import com.alibaba.sdk.android.httpdns.config.ConfigCacheHelper;
import com.alibaba.sdk.android.httpdns.config.RegionServer;
import com.alibaba.sdk.android.httpdns.config.ServerConfig;
import com.alibaba.sdk.android.httpdns.config.SpCacheItem;
import com.alibaba.sdk.android.httpdns.request.HttpRequestConfig;
import com.alibaba.sdk.android.httpdns.utils.CommonUtil;
import com.alibaba.sdk.android.httpdns.utils.Constants;
import com.alibaba.sdk.android.httpdns.utils.ThreadUtil;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;

/**
 * httpdns的配置
 *
 * @author zonglin.nzl
 * @date 2020/12/3
 */
public class HttpDnsConfig implements SpCacheItem {
    private Context context;
    private boolean enabled = true;
    /**
     * 初始服务节点
     */
    private RegionServer initServer = new RegionServer(BuildConfig.INIT_SERVER, Constants.NO_PORTS, Constants.REGION_DEFAULT);
    /**
     * ipv6的初始服务节点
     * 目前仅用于一些ipv6only的环境，避免httpdns完全失效
     */
    private String[] ipv6InitServerIps = BuildConfig.IPV6_INIT_SERVER;
    /**
     * 当前服务节点
     */
    private ServerConfig currentServer;
    /**
     * 用户的accountId
     */
    private String accountId;
    /**
     * 当前请求使用的schema
     */
    private String schema = HttpRequestConfig.HTTP_SCHEMA;
    /**
     * 当前region
     */
    private String region = Constants.REGION_DEFAULT;
    /**
     * 超时时长
     */
    private int timeout = HttpRequestConfig.DEFAULT_TIMEOUT;
    /**
     * 是否禁用服务，以避免崩溃
     */
    private boolean crashDefend;
    /**
     * 是否远程禁用服务
     */
    private boolean remoteDisabled = false;
    /**
     * 是否禁用probe能力
     */
    private boolean probeDisabled = false;

    private ConfigCacheHelper cacheHelper;


    protected ExecutorService worker = ThreadUtil.createExecutorService();
    protected ExecutorService dbWorker = ThreadUtil.createDBExecutorService();

    public HttpDnsConfig(Context context, String accountId) {
        this.context = context;
        this.accountId = accountId;
        this.currentServer = new ServerConfig(this);
        // 先从缓存读取数据，再赋值cacheHelper， 避免在读取缓存过程中，触发写缓存操作
        ConfigCacheHelper helper = new ConfigCacheHelper();
        helper.restoreFromCache(context, this);
        this.cacheHelper = helper;
    }

    public Context getContext() {
        return context;
    }

    public String getAccountId() {
        return accountId;
    }

    public ServerConfig getCurrentServer() {
        return currentServer;
    }

    public boolean isCurrentRegionMatch() {
        return CommonUtil.regionEquals(region, currentServer.getRegion());
    }

    public String getRegion() {
        return region;
    }

    public boolean isEnabled() {
        return enabled && !crashDefend && !remoteDisabled;
    }

    /**
     * 是否启用httpdns
     *
     * @param enabled
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        saveToCache();
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public String getSchema() {
        return schema;
    }

    public ExecutorService getWorker() {
        return worker;
    }

    public ExecutorService getDbWorker() {
        return dbWorker;
    }

    public void setWorker(ExecutorService worker) {
        // 给测试使用
        this.worker = worker;
        this.dbWorker = worker;
    }

    /**
     * 切换https
     *
     * @param enabled
     */
    public void setHTTPSRequestEnabled(boolean enabled) {
        if (enabled) {
            schema = HttpRequestConfig.HTTPS_SCHEMA;
        } else {
            schema = HttpRequestConfig.HTTP_SCHEMA;
        }
    }

    /**
     * 设置用户切换的region
     *
     * @param region
     */
    public void setRegion(String region) {
        this.region = region;
        saveToCache();
    }

    /**
     * 获取ipv6的服务节点
     *
     * @return
     */
    public String[] getIpv6ServerIps() {
        return ipv6InitServerIps;
    }

    public RegionServer getInitServer() {
        return this.initServer;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HttpDnsConfig that = (HttpDnsConfig) o;
        return enabled == that.enabled &&
                timeout == that.timeout &&
                CommonUtil.equals(context, that.context) &&
                CommonUtil.equals(initServer, initServer) &&
                CommonUtil.equals(accountId, that.accountId) &&
                CommonUtil.equals(schema, that.schema) &&
                CommonUtil.equals(region, that.region) &&
                CommonUtil.equals(worker, that.worker);
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(new Object[]{context, enabled, accountId, schema, currentServer, region, timeout, worker, initServer});
        return result;
    }

    /**
     * 设置初始服务IP
     * <p>
     * 线上SDK 初始化服务IP是内置写死的。
     * 本API主要用于一些测试代码使用
     *
     * @param initIps
     * @param initPorts
     */
    public void setInitServers(String[] initIps, int[] initPorts) {
        if (initIps == null) {
            return;
        }
        String[] oldInitServerIps = this.initServer.getServerIps();
        int[] oldInitPorts = this.initServer.getPorts();
        this.initServer.update(initIps, initPorts);
        if (currentServer.getServerIps() == null || CommonUtil.isSameServer(oldInitServerIps, oldInitPorts, currentServer.getServerIps(), currentServer.getPorts())) {
            // 初始IP默认region为国内
            currentServer.setServerIps(this.initServer.getRegion(), initIps, initPorts);
        }
    }

    public void crashDefend(boolean crashDefend) {
        this.crashDefend = crashDefend;
    }

    public void remoteDisable(boolean disable) {
        this.remoteDisabled = disable;
    }

    public void probeDisable(boolean disable) {
        this.probeDisabled = disable;
    }

    public boolean isProbeDisabled() {
        return probeDisabled;
    }


    // 缓存相关的 处理，暂时放这里

    public void saveToCache() {
        if (cacheHelper != null) {
            cacheHelper.saveConfigToCache(context, this);
        }
    }

    public SpCacheItem[] getCacheItem() {
        return new SpCacheItem[]{this, currentServer};
    }

    @Override
    public void restoreFromCache(SharedPreferences sp) {
        region = sp.getString(Constants.CONFIG_REGION, Constants.REGION_DEFAULT);
        enabled = sp.getBoolean(Constants.CONFIG_ENABLE, true);
    }

    @Override
    public void saveToCache(SharedPreferences.Editor editor) {
        editor.putString(Constants.CONFIG_REGION, region);
        editor.putBoolean(Constants.CONFIG_ENABLE, enabled);
    }
}
