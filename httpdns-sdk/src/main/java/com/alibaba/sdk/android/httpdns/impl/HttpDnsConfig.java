package com.alibaba.sdk.android.httpdns.impl;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

import com.alibaba.sdk.android.httpdns.BuildConfig;
import com.alibaba.sdk.android.httpdns.request.HttpRequestConfig;
import com.alibaba.sdk.android.httpdns.utils.CommonUtil;
import com.alibaba.sdk.android.httpdns.utils.ThreadUtil;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;

/**
 * httpdns的配置
 *
 * @author zonglin.nzl
 * @date 2020/12/3
 */
public class HttpDnsConfig {
    private Context context;
    private boolean enabled = true;
    private String[] initServerIps = BuildConfig.INIT_SERVER;
    private int[] initServerPorts = null;
    private String accountId;
    private String schema = HttpRequestConfig.HTTP_SCHEMA;
    private String[] serverIps = BuildConfig.INIT_SERVER;
    private int[] ports = null;
    private int lastOkServerIndex = 0;
    private int currentServerIndex = 0;
    private String region = null;
    private long serverIpsLastUpdatedTime = 0;
    private int timeout = HttpRequestConfig.DEFAULT_TIMEOUT;
    private boolean crashDefend;
    private boolean remoteDisabled = false;
    private boolean probeDisabled = false;


    protected ExecutorService worker = ThreadUtil.createExecutorService();

    private HttpDnsConfig() {
    }

    public HttpDnsConfig(Context context, String accountId) {
        this.context = context;
        this.accountId = accountId;
        readFromCache(context, this);
    }

    public Context getContext() {
        return context;
    }

    public String getAccountId() {
        return accountId;
    }

    public String[] getServerIps() {
        return serverIps;
    }

    public String getRegion() {
        return region;
    }

    public boolean isEnabled() {
        return enabled && !crashDefend && !remoteDisabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        saveToCache(context, this);
    }

    /**
     * 设置服务IP
     *
     * @param serverIps
     * @param ports
     * @return false 表示 前后服务一直，没有更新
     */
    public boolean setServerIps(String[] serverIps, int[] ports) {
        return setServerIps(region, serverIps, ports);
    }

    /**
     * 设置服务IP
     *
     * @param serverIps
     * @param ports
     * @return false 表示 前后服务一直，没有更新
     */
    public boolean setServerIps(String region, String[] serverIps, int[] ports) {
        if (isSameServer(this.serverIps, this.ports, serverIps, ports)) {
            return false;
        }
        this.region = region;
        this.serverIps = serverIps;
        this.ports = ports;
        this.lastOkServerIndex = 0;
        this.currentServerIndex = 0;
        if (!Arrays.equals(serverIps, initServerIps)) {
            // 非初始化IP，才认为是真正的更新了服务IP
            this.serverIpsLastUpdatedTime = System.currentTimeMillis();
        }
        saveToCache(context, this);
        return true;
    }

    private boolean isSameServer(String[] oldServerIps, int[] oldPorts, String[] newServerIps, int[] newPorts) {
        return Arrays.equals(oldServerIps, newServerIps) && Arrays.equals(oldPorts, newPorts);
    }

    public int[] getPorts() {
        return ports;
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

    public void setWorker(ExecutorService worker) {
        this.worker = worker;
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
            return getDefaultPort();
        }
        return ports[currentServerIndex];
    }

    private int getDefaultPort() {
        if (schema.equals(HttpRequestConfig.HTTP_SCHEMA)) {
            return 80;
        } else {
            return 443;
        }
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
            saveToCache(context, this);
            return true;
        }
        return false;
    }

    /**
     * 重置服务Ip为初始服务IP
     */
    public void resetServerIpsToInitServer() {
        if (initServerIps != null) {
            setServerIps(initServerIps, initServerPorts);
        }
    }

    /**
     * 复制自身
     * @return
     */
    public HttpDnsConfig copy() {
        HttpDnsConfig tmp = new HttpDnsConfig();
        tmp.context = context;
        tmp.accountId = accountId;
        tmp.schema = schema;
        tmp.region = region;
        tmp.serverIps = serverIps == null ? null : Arrays.copyOf(serverIps, serverIps.length);
        tmp.ports = ports == null ? null : Arrays.copyOf(ports, ports.length);
        tmp.lastOkServerIndex = lastOkServerIndex;
        tmp.currentServerIndex = currentServerIndex;
        tmp.serverIpsLastUpdatedTime = serverIpsLastUpdatedTime;
        tmp.timeout = timeout;
        tmp.worker = worker;
        tmp.initServerIps = initServerIps;
        tmp.initServerPorts = initServerPorts;
        return tmp;
    }

    /**
     * 获取初始服务个数
     * @return
     */
    public int getInitServerSize() {
        return this.initServerIps == null ? 0 : this.initServerIps.length;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HttpDnsConfig that = (HttpDnsConfig) o;
        return enabled == that.enabled &&
                lastOkServerIndex == that.lastOkServerIndex &&
                currentServerIndex == that.currentServerIndex &&
                serverIpsLastUpdatedTime == that.serverIpsLastUpdatedTime &&
                timeout == that.timeout &&
                CommonUtil.equals(context, that.context) &&
                Arrays.equals(initServerIps, that.initServerIps) &&
                Arrays.equals(initServerPorts, that.initServerPorts) &&
                CommonUtil.equals(accountId, that.accountId) &&
                CommonUtil.equals(schema, that.schema) &&
                Arrays.equals(serverIps, that.serverIps) &&
                Arrays.equals(ports, that.ports) &&
                CommonUtil.equals(region, that.region) &&
                CommonUtil.equals(worker, that.worker);
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(new Object[]{context, enabled, accountId, schema, lastOkServerIndex, currentServerIndex, region, serverIpsLastUpdatedTime, timeout, worker});
        result = 31 * result + Arrays.hashCode(initServerIps);
        result = 31 * result + Arrays.hashCode(initServerPorts);
        result = 31 * result + Arrays.hashCode(serverIps);
        result = 31 * result + Arrays.hashCode(ports);
        return result;
    }

    /**
     * 设置初始服务IP
     * @param initIps
     * @param initPorts
     */
    public void setInitServers(String[] initIps, int[] initPorts) {
        if (initIps == null) {
            return;
        }
        String[] oldInitServerIps = this.initServerIps;
        this.initServerIps = initIps;
        this.initServerPorts = initPorts;
        if (serverIps == null || isSameServer(oldInitServerIps, null, serverIps, null)) {
            setServerIps(initIps, initPorts);
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

    private static final String CONFIG_CACHE_PREFIX = "httpdns_config_";
    private static final String CONFIG_KEY_SERVERS = "serverIps";
    private static final String CONFIG_KEY_PORTS = "ports";
    private static final String CONFIG_CURRENT_INDEX = "current";
    private static final String CONFIG_LAST_INDEX = "last";
    private static final String CONFIG_SERVERS_LAST_UPDATED_TIME = "servers_last_updated_time";
    private static final String CONFIG_REGION = "region";
    private static final String CONFIG_ENABLE = "enable";

    private static void readFromCache(Context context, HttpDnsConfig config) {
        SharedPreferences sp = context.getSharedPreferences(CONFIG_CACHE_PREFIX + config.getAccountId(), Context.MODE_PRIVATE);
        config.serverIps = CommonUtil.parseStringArray(sp.getString(CONFIG_KEY_SERVERS, CommonUtil.translateStringArray(BuildConfig.INIT_SERVER)));
        config.ports = CommonUtil.parseIntArray(sp.getString(CONFIG_KEY_PORTS, null));
        config.currentServerIndex = sp.getInt(CONFIG_CURRENT_INDEX, 0);
        config.lastOkServerIndex = sp.getInt(CONFIG_LAST_INDEX, 0);
        config.serverIpsLastUpdatedTime = sp.getLong(CONFIG_SERVERS_LAST_UPDATED_TIME, 0);
        config.region = sp.getString(CONFIG_REGION, null);
        config.enabled = sp.getBoolean(CONFIG_ENABLE, true);
    }

    @SuppressLint("ApplySharedPref")
    private static void saveToCache(Context context, HttpDnsConfig config) {
        SharedPreferences.Editor editor = context.getSharedPreferences(CONFIG_CACHE_PREFIX + config.getAccountId(), Context.MODE_PRIVATE).edit();
        editor.putString(CONFIG_KEY_SERVERS, CommonUtil.translateStringArray(config.serverIps));
        editor.putString(CONFIG_KEY_PORTS, CommonUtil.translateIntArray(config.ports));
        editor.putInt(CONFIG_CURRENT_INDEX, config.currentServerIndex);
        editor.putInt(CONFIG_LAST_INDEX, config.lastOkServerIndex);
        editor.putLong(CONFIG_SERVERS_LAST_UPDATED_TIME, config.serverIpsLastUpdatedTime);
        editor.putString(CONFIG_REGION, config.region);
        editor.putBoolean(CONFIG_ENABLE, config.enabled);
        // 虽然提示建议使用apply，但是实践证明，apply是把写文件操作推迟到了一些界面切换等时机，反而影响了UI线程。不如直接在子线程写文件
        editor.commit();
    }

}
