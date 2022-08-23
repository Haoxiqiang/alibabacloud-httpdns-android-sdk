package com.alibaba.sdk.android.httpdns.impl;

import android.content.Context;
import android.os.Looper;

import com.alibaba.sdk.android.crashdefend.CrashDefendApi;
import com.alibaba.sdk.android.crashdefend.CrashDefendCallback;
import com.alibaba.sdk.android.httpdns.BuildConfig;
import com.alibaba.sdk.android.httpdns.DegradationFilter;
import com.alibaba.sdk.android.httpdns.HTTPDNSResult;
import com.alibaba.sdk.android.httpdns.HttpDnsService;
import com.alibaba.sdk.android.httpdns.ILogger;
import com.alibaba.sdk.android.httpdns.InitConfig;
import com.alibaba.sdk.android.httpdns.RequestIpType;
import com.alibaba.sdk.android.httpdns.SyncService;
import com.alibaba.sdk.android.httpdns.cache.RecordDBHelper;
import com.alibaba.sdk.android.httpdns.interpret.HostFilter;
import com.alibaba.sdk.android.httpdns.interpret.InterpretHostCacheGroup;
import com.alibaba.sdk.android.httpdns.interpret.InterpretHostRequestHandler;
import com.alibaba.sdk.android.httpdns.interpret.InterpretHostResultRepo;
import com.alibaba.sdk.android.httpdns.interpret.InterpretHostService;
import com.alibaba.sdk.android.httpdns.interpret.ResolveHostService;
import com.alibaba.sdk.android.httpdns.log.HttpDnsLog;
import com.alibaba.sdk.android.httpdns.net.NetworkStateManager;
import com.alibaba.sdk.android.httpdns.probe.IPProbeItem;
import com.alibaba.sdk.android.httpdns.probe.ProbeService;
import com.alibaba.sdk.android.httpdns.serverip.ScheduleService;
import com.alibaba.sdk.android.httpdns.track.SessionTrackMgr;
import com.alibaba.sdk.android.httpdns.utils.CommonUtil;
import com.alibaba.sdk.android.httpdns.utils.Constants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 域名解析服务 httpdns接口的实现
 *
 * @author zonglin.nzl
 * @date 2020/10/15
 */
public class HttpDnsServiceImpl implements HttpDnsService, ScheduleService.OnServerIpUpdate, NetworkStateManager.OnNetworkChange, SyncService {

    protected HttpDnsConfig config;
    private InterpretHostResultRepo repo;
    protected InterpretHostRequestHandler requestHandler;
    protected ScheduleService scheduleService;
    protected ProbeService ipProbeService;
    protected InterpretHostService interpretHostService;
    protected ResolveHostService resolveHostService;
    private HostFilter filter;
    private SignService signService;
    private boolean resolveAfterNetworkChange = true;
    private HostInterpretRecorder recorder = new HostInterpretRecorder();

    public HttpDnsServiceImpl(Context context, final String accountId, String secret) {
        try {
            config = new HttpDnsConfig(context, accountId);
            filter = new HostFilter();
            signService = new SignService(secret);
            ipProbeService = new ProbeService(this.config);
            repo = new InterpretHostResultRepo(this.config, this.ipProbeService, new RecordDBHelper(this.config.getContext(), this.config.getAccountId()), new InterpretHostCacheGroup());
            scheduleService = new ScheduleService(this.config, this);
            requestHandler = new InterpretHostRequestHandler(config, scheduleService, signService);
            interpretHostService = new InterpretHostService(this.config, ipProbeService, requestHandler, repo, filter, recorder);
            resolveHostService = new ResolveHostService(this.config, repo, requestHandler, ipProbeService, filter, recorder);

            beforeInit();

            setupInitConfig(accountId);

            initCrashDefend(context, config);
            if (!config.isEnabled()) {
                HttpDnsLog.w("init fail, crashdefend");
                return;
            }
            NetworkStateManager.getInstance().init(context);
            NetworkStateManager.getInstance().addListener(this);
            if (config.getCurrentServer().shouldUpdateServerIp() || !config.isCurrentRegionMatch()) {
                scheduleService.updateServerIps();
            }
            favorInit(context, accountId);

            if (HttpDnsLog.isPrint()) {
                HttpDnsLog.d("httpdns service is inited " + accountId);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupInitConfig(final String accountId) {
        InitConfig config = InitConfig.getInitConfig(accountId);

        if (config != null) {
            // 先设置和网络相关的内容
            this.config.setTimeout(config.getTimeout());
            this.config.setHTTPSRequestEnabled(config.isEnableHttps());
            // 再设置一些可以提前，没有副作用的内容
            setExpiredIPEnabled(config.isEnableExpiredIp());
            if (config.getIpProbeItems() != null) {
                setIPProbeList(config.getIpProbeItems());
            }
            // 设置region 必须在 读取缓存之前
            this.config.setRegion(config.getRegion());
            // 设置 主站域名 需要在 读取缓存之前
            this.repo.setHostListWhichIpFixed(config.getHostListWithFixedIp());
            // 设置缓存控制，并读取缓存
            setCachedIPEnabled(config.isEnableCacheIp());
            this.repo.setCacheTtlChanger(config.getCacheTtlChanger());
        }

    }

    protected void beforeInit() {
        // only for test
    }


    protected void favorInit(Context context, String accountId) {
        // for different favor init
    }


    protected void initCrashDefend(Context context, final HttpDnsConfig config) {
        CrashDefendApi.registerCrashDefendSdk(context, "httpdns", BuildConfig.VERSION_NAME, 2, 7, new CrashDefendCallback() {
            @Override
            public void onSdkStart(int limitCount, int crashCount, int restoreCount) {
                config.crashDefend(false);
            }

            @Override
            public void onSdkStop(int limitCount, int crashCount, int restoreCount, long nextRestoreInterval) {
                config.crashDefend(true);
                HttpDnsLog.w("sdk is not safe to run");
            }

            @Override
            public void onSdkClosed(int restoreCount) {
                config.crashDefend(true);
                HttpDnsLog.e("sdk will not run any more");
            }
        });
    }


    public void setSecret(String secret) {
        if (!config.isEnabled()) {
            return;
        }
        if (secret == null || secret.equals("")) {
            HttpDnsLog.e("set empty secret!?");
        }
        this.signService.setSecret(secret);
    }

    @Override
    public void serverIpUpdated(boolean regionUpdated) {
        if (!config.isEnabled()) {
            return;
        }
        if (regionUpdated) {
            repo.clearMemoryCache();
        }
        requestHandler.resetStatus();
    }

    @Override
    public void setLogEnabled(boolean shouldPrintLog) {
        if (!config.isEnabled()) {
            return;
        }
        HttpDnsLog.enable(shouldPrintLog);
    }

    @Override
    public void setPreResolveHosts(ArrayList<String> hostList) {
        if (!config.isEnabled()) {
            HttpDnsLog.i("service is disabled");
            return;
        }
        if (hostList == null || hostList.size() == 0) {
            HttpDnsLog.i("setPreResolveHosts empty list");
            return;
        }
        setPreResolveHosts(hostList, RequestIpType.v4);
    }

    @Override
    public void setPreResolveHosts(ArrayList<String> hostList, RequestIpType requestIpType) {
        if (!config.isEnabled()) {
            HttpDnsLog.i("service is disabled");
            return;
        }
        if (hostList == null || hostList.size() == 0) {
            HttpDnsLog.i("setPreResolveHosts empty list");
            return;
        }
        resolveHostService.resolveHostAsync(hostList, requestIpType);
    }

    @Override
    public String getIpByHostAsync(String host) {
        if (!config.isEnabled()) {
            HttpDnsLog.i("service is disabled");
            return null;
        }
        if (!CommonUtil.isAHost(host)) {
            HttpDnsLog.i("host is invalid. " + host);
            return null;
        }
        if (CommonUtil.isAnIP(host)) {
            HttpDnsLog.i("host is ip. " + host);
            return null;
        }
        String[] ips = getIpsByHostAsync(host);
        if (ips == null || ips.length == 0) {
            return null;
        }
        return ips[0];
    }

    @Override
    public String[] getIpsByHostAsync(final String host) {
        if (!config.isEnabled()) {
            HttpDnsLog.i("service is disabled");
            return new String[0];
        }
        if (!CommonUtil.isAHost(host)) {
            HttpDnsLog.i("host is invalid. " + host);
            return new String[0];
        }
        if (CommonUtil.isAnIP(host)) {
            HttpDnsLog.i("host is ip. " + host);
            return new String[0];
        }
        return interpretHostService.interpretHostAsync(host, RequestIpType.v4, null, null).getIps();
    }

    @Override
    public String[] getIPv6sByHostAsync(String host) {
        if (!config.isEnabled()) {
            HttpDnsLog.i("service is disabled");
            return new String[0];
        }
        if (!CommonUtil.isAHost(host)) {
            HttpDnsLog.i("host is invalid. " + host);
            return new String[0];
        }
        if (CommonUtil.isAnIP(host)) {
            HttpDnsLog.i("host is ip. " + host);
            return new String[0];
        }
        return interpretHostService.interpretHostAsync(host, RequestIpType.v6, null, null).getIpv6s();
    }

    @Override
    public HTTPDNSResult getAllByHostAsync(String host) {
        if (!config.isEnabled()) {
            HttpDnsLog.i("service is disabled");
            return Constants.EMPTY;
        }
        if (!CommonUtil.isAHost(host)) {
            HttpDnsLog.i("host is invalid. " + host);
            return Constants.EMPTY;
        }
        if (CommonUtil.isAnIP(host)) {
            HttpDnsLog.i("host is ip. " + host);
            return Constants.EMPTY;
        }
        return interpretHostService.interpretHostAsync(host, RequestIpType.both, null, null);
    }

    @Override
    public void setExpiredIPEnabled(boolean enable) {
        if (!config.isEnabled()) {
            return;
        }
        interpretHostService.setEnableExpiredIp(enable);
    }

    @Override
    public void setCachedIPEnabled(boolean enable) {
        if (!config.isEnabled()) {
            return;
        }
        setCachedIPEnabled(enable, false);
    }

    @Override
    public void setCachedIPEnabled(boolean enable, boolean autoCleanCacheAfterLoad) {
        if (!config.isEnabled()) {
            return;
        }
        repo.setCachedIPEnabled(enable, autoCleanCacheAfterLoad);
    }

    @Override
    public void setAuthCurrentTime(long time) {
        if (!config.isEnabled()) {
            return;
        }
        signService.setCurrentTimestamp(time);
    }

    @Override
    public void setDegradationFilter(DegradationFilter filter) {
        if (!config.isEnabled()) {
            return;
        }
        this.filter.setFilter(filter);
    }

    @Override
    public void setPreResolveAfterNetworkChanged(boolean enable) {
        if (!config.isEnabled()) {
            return;
        }
        this.resolveAfterNetworkChange = enable;
    }

    @Override
    public void setTimeoutInterval(int timeoutInterval) {
        if (!config.isEnabled()) {
            return;
        }
        this.config.setTimeout(timeoutInterval);
    }

    @Override
    public void setHTTPSRequestEnabled(boolean enabled) {
        if (!config.isEnabled()) {
            return;
        }
        boolean changed = config.setHTTPSRequestEnabled(enabled);
        if (changed && enabled) {
            // 避免应用禁止http请求，导致初始化时的服务更新请求失败
            if (config.getCurrentServer().shouldUpdateServerIp()) {
                scheduleService.updateServerIps();
            }
        }
    }

    @Override
    public void setIPProbeList(List<IPProbeItem> ipProbeList) {
        if (!config.isEnabled()) {
            return;
        }
        ipProbeService.setProbeItems(ipProbeList);
    }

    @Override
    public String getSessionId() {
        return SessionTrackMgr.getInstance().getSessionId();
    }

    @Override
    public void setLogger(ILogger logger) {
        HttpDnsLog.setLogger(logger);
    }

    @Override
    public HTTPDNSResult getIpsByHostAsync(String host, Map<String, String> params, String cacheKey) {
        if (!config.isEnabled()) {
            HttpDnsLog.i("service is disabled");
            return Constants.EMPTY;
        }
        if (!CommonUtil.isAHost(host)) {
            HttpDnsLog.i("host is invalid. " + host);
            return Constants.EMPTY;
        }
        if (CommonUtil.isAnIP(host)) {
            HttpDnsLog.i("host is ip. " + host);
            return Constants.EMPTY;
        }
        return interpretHostService.interpretHostAsync(host, RequestIpType.v4, params, cacheKey);
    }

    @Override
    public HTTPDNSResult getIpsByHostAsync(String host, RequestIpType type, Map<String, String> params, String cacheKey) {
        if (!config.isEnabled()) {
            HttpDnsLog.i("service is disabled");
            return Constants.EMPTY;
        }
        if (!CommonUtil.isAHost(host)) {
            HttpDnsLog.i("host is invalid. " + host);
            return Constants.EMPTY;
        }
        if (CommonUtil.isAnIP(host)) {
            HttpDnsLog.i("host is ip. " + host);
            return Constants.EMPTY;
        }
        return interpretHostService.interpretHostAsync(host, type, params, cacheKey);
    }

    @Override
    public void setSdnsGlobalParams(Map<String, String> params) {
        if (!config.isEnabled()) {
            return;
        }
        requestHandler.setSdnsGlobalParams(params);
    }

    @Override
    public void clearSdnsGlobalParams() {
        if (!config.isEnabled()) {
            return;
        }
        requestHandler.clearSdnsGlobalParams();
    }

    @Override
    public void setRegion(String region) {
        if (!config.isEnabled()) {
            HttpDnsLog.i("service is disabled");
            return;
        }
        region = CommonUtil.fixRegion(region);
        if (CommonUtil.regionEquals(this.config.getRegion(), region)
                && this.config.isCurrentRegionMatch()
                && !this.config.isInitServerInUse()) {
            if (HttpDnsLog.isPrint()) {
                HttpDnsLog.d("region " + region + " is same, do not update serverIps");
            }
            return;
        }
        boolean changed = config.setRegion(region);
        if (changed) {
            repo.clearMemoryCache();
        }
        scheduleService.updateServerIps(region);
    }

    @Override
    public void enableIPv6(boolean enable) {
        // deprecated
    }

    @Override
    public String getIPv6ByHostAsync(String host) {
        if (!config.isEnabled()) {
            HttpDnsLog.i("service is disabled");
            return null;
        }
        if (!CommonUtil.isAHost(host)) {
            HttpDnsLog.i("host is invalid. " + host);
            return null;
        }
        if (CommonUtil.isAnIP(host)) {
            HttpDnsLog.i("host is ip. " + host);
            return null;
        }
        String[] ips = getIPv6sByHostAsync(host);
        if (ips == null || ips.length == 0) {
            return null;
        }
        return ips[0];
    }

    @Override
    public void onNetworkChange(String networkType) {
        if (!config.isEnabled()) {
            return;
        }
        try {
            config.getWorker().execute(new Runnable() {
                @Override
                public void run() {
                    HashMap<String, RequestIpType> allHost = repo.getAllHostWithoutFixedIP();
                    HttpDnsLog.d("network change, clean record");
                    repo.clearMemoryCacheForHostWithoutFixedIP();
                    if (resolveAfterNetworkChange && config.isEnabled()) {
                        ArrayList<String> v4List = new ArrayList<>();
                        ArrayList<String> v6List = new ArrayList<>();
                        ArrayList<String> bothList = new ArrayList<>();
                        for (Map.Entry<String, RequestIpType> entry : allHost.entrySet()) {
                            if (entry.getValue() == RequestIpType.v4) {
                                v4List.add(entry.getKey());
                            } else if (entry.getValue() == RequestIpType.v6) {
                                v6List.add(entry.getKey());
                            } else {
                                bothList.add(entry.getKey());
                            }
                        }
                        if (v4List.size() > 0) {
                            resolveHostService.resolveHostAsync(v4List, RequestIpType.v4);
                        }
                        if (v6List.size() > 0) {
                            resolveHostService.resolveHostAsync(v6List, RequestIpType.v6);
                        }
                        if (bothList.size() > 0) {
                            resolveHostService.resolveHostAsync(bothList, RequestIpType.both);
                        }
                        if (v4List.size() > 0 || v6List.size() > 0 || bothList.size() > 0) {
                            HttpDnsLog.d("network change, resolve hosts");
                        }
                    }
                }
            });
        } catch (Exception e) {
        }
    }

    @Override
    public HTTPDNSResult getByHost(String host, RequestIpType type) {
        if (!config.isEnabled()) {
            HttpDnsLog.i("service is disabled");
            return Constants.EMPTY;
        }
        if (!CommonUtil.isAHost(host)) {
            HttpDnsLog.i("host is invalid. " + host);
            return Constants.EMPTY;
        }
        if (CommonUtil.isAnIP(host)) {
            HttpDnsLog.i("host is ip. " + host);
            return Constants.EMPTY;
        }
        if (Looper.getMainLooper() == Looper.myLooper()) {
            if (HttpDnsLog.isPrint()) {
                HttpDnsLog.d("request in main thread, use async request");
            }
            return interpretHostService.interpretHostAsync(host, type, null, null);
        }
        return interpretHostService.interpretHost(host, type, null, null);
    }

    public void cleanHostCache(ArrayList<String> hosts) {
        if (hosts == null || hosts.size() == 0) {
            // 清理所有host
            repo.clear();
        } else {
            // 清理选中的host
            repo.clear(hosts);
        }
    }
}
