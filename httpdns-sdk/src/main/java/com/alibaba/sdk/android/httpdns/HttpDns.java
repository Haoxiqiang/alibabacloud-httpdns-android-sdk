package com.alibaba.sdk.android.httpdns;

import android.content.Context;
import android.text.TextUtils;

import com.alibaba.sdk.android.httpdns.cache.DBCacheManager;
import com.alibaba.sdk.android.httpdns.net64.Net64Mgr;
import com.alibaba.sdk.android.httpdns.probe.IPProbeItem;
import com.alibaba.sdk.android.httpdns.report.ReportManager;
import com.alibaba.sdk.android.httpdns.report.ReportUtils;
import com.alibaba.sdk.android.httpdns.track.SessionTrackMgr;
import com.alibaba.sdk.android.utils.AMSConfigUtils;
import com.alibaba.sdk.android.utils.AMSDevReporter;
import com.alibaba.sdk.android.utils.crashdefend.SDKMessageCallback;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

public class HttpDns implements HttpDnsService {
    private boolean isExpiredIPEnabled = false;
    private static HostManager hostManager = HostManager.getInstance();
    private static DegradationFilter degradationFilter = null;
    static HttpDns instance = null;
    private static boolean inited = false;

    private static String sAccountId = null;
    private static String sSecretKey = null;
    private static Context sContext = null;

    /**
     * 获取HttpDnsService对象
     *
     * @param applicationContext 当前APP的Context
     * @param accountID          HttpDns控制台分配的AccountID
     * @return
     */
    public synchronized static HttpDnsService getService(final Context applicationContext, final String accountID) {
        if (instance == null && applicationContext != null) {
            sContext = applicationContext.getApplicationContext();
            setAccountId(accountID);
            EnableManager.init(sContext);
            ReportManager rm = ReportManager.getInstance(sContext);
            boolean registerResult = rm.registerCrash(new SDKMessageCallback() {
                @Override
                public void crashDefendMessage(int limit, int count) {
                    inited = true;
                    if (limit > count) {
                        EnableManager.safe(true);
                    } else {
                        HttpDnsLog.Loge("crash limit exceeds, httpdns disabled");
                        EnableManager.safe(false);
                    }
                }
            });

            if (!inited) {
                HttpDnsLog.Loge("sdk crash defend not returned");
            }


            if (EnableManager.isEnable()) {
                initHttpDns(sContext, getAccountId(), getSecretKey());
            } else {
                instance = new HttpDns(sContext, getAccountId());
            }
        }
        return instance;
    }

    /**
     * 获取HttpDnsService对象，并启用鉴权功能
     *
     * @param applicationContext 当前APP的Context
     * @param accountID          HttpDns控制台分配的AccountID
     * @param secretKey          用户鉴权私钥
     * @return
     */
    public synchronized static HttpDnsService getService(final Context applicationContext, final String accountID, final String secretKey) {
        if (instance == null && applicationContext != null) {
            sContext = applicationContext.getApplicationContext();
            setAccountId(accountID);
            setSecretKey(secretKey);
            EnableManager.init(sContext);
            ReportManager rm = ReportManager.getInstance(sContext);
            boolean registerResult = rm.registerCrash(new SDKMessageCallback() {
                @Override
                public void crashDefendMessage(int limit, int count) {
                    inited = true;
                    if (limit > count) {
                        EnableManager.safe(true);
                    } else {
                        HttpDnsLog.Loge("crash limit exceeds, httpdns disabled");
                        EnableManager.safe(false);
                    }
                }
            });

            if (!inited) {
                HttpDnsLog.Loge("sdk crash defend not returned");
            }

            if (EnableManager.isEnable()) {
                initHttpDns(sContext, getAccountId(), getSecretKey());
            } else {
                instance = new HttpDns(sContext, getAccountId());
            }
        }
        return instance;
    }

    /**
     * 获取HttpDnsService对象，初始化时不传入任何参数，靠统一接入服务获取相关参数
     *
     * @param applicationContext 当前APP的Context
     * @return
     */
    public synchronized static HttpDnsService getService(final Context applicationContext) {
        if (instance == null && applicationContext != null) {
            sContext = applicationContext.getApplicationContext();
            EnableManager.init(sContext);
            ReportManager rm = ReportManager.getInstance(sContext);
            boolean registerResult = rm.registerCrash(new SDKMessageCallback() {
                @Override
                public void crashDefendMessage(int limit, int count) {
                    inited = true;
                    if (limit > count) {
                        EnableManager.safe(true);
                    } else {
                        HttpDnsLog.Loge("crash limit exceeds, httpdns disabled");
                        EnableManager.safe(false);
                    }
                }
            });

            if (!inited) {
                HttpDnsLog.Loge("sdk crash defend not returned");
            }


            if (EnableManager.isEnable()) {
                initHttpDns(sContext, getAccountId(), getSecretKey());
            } else {
                instance = new HttpDns(sContext, getAccountId());
            }
        }
        return instance;
    }

    /**
     * @param context
     * @param accountID
     * @param secretKey
     */
    private static void initHttpDns(Context context, String accountID, String secretKey) {
        if (instance == null) {
            Map<String, Object> extInfo = new HashMap<String, Object>();
            extInfo.put(AMSDevReporter.AMSSdkExtInfoKeyEnum.AMS_EXTINFO_KEY_VERSION.toString(), BuildConfig.VERSION_NAME);
            AMSDevReporter.asyncReport(context, AMSDevReporter.AMSSdkTypeEnum.AMS_HTTPDNS, extInfo);
            NetworkStateManager.setContext(context);
            QueryHostTask.setContext(context);
            DBCacheManager.init(context);
            DBCacheManager.updateSp(context);
            StatusManager.init(context);
            HttpdnsScheduleCenter.getInstance().init(context, accountID);
            if (!TextUtils.isEmpty(secretKey)) {
                AuthManager.setSecretKey(secretKey);
            }
            reportActive(context, accountID);

            instance = new HttpDns(context, accountID);
        }
    }

    private HttpDns(Context context, String accountID) {
        HttpDnsConfig.setAccountID(accountID);
    }

    /**
     * 上报日活
     *
     * @param applicationContext
     * @param accountId
     */
    private static void reportActive(Context applicationContext, String accountId) {
        if (applicationContext != null && !TextUtils.isEmpty(accountId)) {
            ReportManager.getInstance(applicationContext).setAccountId(accountId);
            ReportManager.getInstance(applicationContext).reportSdkStart();
        } else {
            HttpDnsLog.Loge("report active failed due to missing context or accountid");
        }
    }

    @Override
    public void setLogEnabled(boolean shouldPrintLog) {
        HttpDnsLog.setLogEnabled(shouldPrintLog);
    }

    @Override
    public void setPreResolveHosts(final ArrayList<String> list) {
        if (!EnableManager.isEnable()) {
            HttpDnsLog.Loge("HttpDns service turned off");
            return;
        }
        for (int i = 0; i < list.size(); i++) {
            String hostName = list.get(i);

            if (!HttpDnsUtil.isAHost(hostName)) {
                continue;
            }

            if (!hostManager.isResolving(hostName)) {
                GlobalDispatcher.getDispatcher().submit(new QueryHostTask(hostName, QueryType.QUERY_HOST));
            }
        }
    }

    @Override
    public String getIpByHostAsync(String hostName) {
        try {
            if (!EnableManager.isEnable()) {
                HttpDnsLog.Loge("HttpDns service turned off");
                return null;
            }

            String[] ips = getIpsByHostAsync(hostName);

            if (ips == null) {
                return null;
            }

            if (ips.length > 0) {
                return ips[0];
            } else {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    //同步方法下线,先保留实现,方法改为private
    private String getIpByHost(String hostName) {
        if (!EnableManager.isEnable()) {
            HttpDnsLog.Loge("HttpDns service turned off");
            return null;
        }

        String[] ips = getIpsByHost(hostName);

        if (ips == null) {
            return null;
        }

        if (ips.length > 0) {
            return ips[0];
        } else {
            return null;
        }
    }

    //同步方法下线,先保留实现,方法改为private
    private String[] getIpsByHost(String hostName) {
        if (!EnableManager.isEnable()) {
            HttpDnsLog.Loge("HttpDns service turned off");
            return HttpDnsConfig.NO_IPS;
        }

        if (!HttpDnsUtil.isAHost(hostName)) {
            return HttpDnsConfig.NO_IPS;
        }
        if (HttpDnsUtil.isAnIP(hostName)) {
            return new String[]{hostName};
        }
        if (degradationFilter != null && degradationFilter.shouldDegradeHttpDNS(hostName)) {
            return HttpDnsConfig.NO_IPS;
        }

        //如果是disabled状态,通过异步解析逻辑发起嗅探
        if (StatusManager.isDisabled()) {
            return this.getIpsByHostAsync(hostName);
        }

        HostObject host = hostManager.get(hostName);
        if (host != null && host.isExpired() && isExpiredIPEnabled) {
            if (!hostManager.isResolving(hostName)) {
                HttpDnsLog.Logd("refresh host async: " + hostName);
                GlobalDispatcher.getDispatcher().submit(new QueryHostTask(hostName, QueryType.QUERY_HOST));
            }
            return host.getIps();
        }
        if (host == null || host.isExpired()) {
            HttpDnsLog.Logd("refresh host sync: " + hostName);
            Future<String[]> future = GlobalDispatcher.getDispatcher().submit(new QueryHostTask(hostName, QueryType.QUERY_HOST));
            try {
                String[] result = future.get();
                return result;
            } catch (Exception e) {
                HttpDnsLog.printStackTrace(e);
            }
            return HttpDnsConfig.NO_IPS;
        }
        return host.getIps();
    }

    @Override
    public String[] getIpsByHostAsync(String hostName) {
        if (!EnableManager.isEnable()) {
            HttpDnsLog.Loge("HttpDns service turned off");
            return HttpDnsConfig.NO_IPS;
        }

        if (!HttpDnsUtil.isAHost(hostName)) {
            // 用户输入错误情况下暂不上报调用接口成功率
            return HttpDnsConfig.NO_IPS;
        }
        if (HttpDnsUtil.isAnIP(hostName)) {
            // 输入IP情况下暂不调用接口成功率
            return new String[]{hostName};
        }
        if (degradationFilter != null && degradationFilter.shouldDegradeHttpDNS(hostName)) {
            // 用户自定义降级逻辑，不上报
            return HttpDnsConfig.NO_IPS;
        }

        final HostObject host = hostManager.get(hostName);
        boolean isExpired = false;
        if ((host == null || (isExpired = host.isExpired())) && !hostManager.isResolving(hostName)) {
            if (StatusManager.isDisabled()) {
                Sniffer.getInstance().sniff(hostName);
            } else {
                HttpDnsLog.Logd("refresh host async: " + hostName);
                QueryHostTask task = new QueryHostTask(hostName, QueryType.QUERY_HOST);
                GlobalDispatcher.getDispatcher().submit(task);
            }
        } else if (host != null) {
            //如果有SDNS的缓存，重新请求
            if (host.getCacheKey() != null) {
                if (StatusManager.isDisabled()) {
                    Sniffer.getInstance().sniff(hostName);
                } else {
                    HttpDnsLog.Logd("refresh host async: " + hostName);
                    QueryHostTask task = new QueryHostTask(hostName, QueryType.QUERY_HOST);
                    GlobalDispatcher.getDispatcher().submit(task);
                }
            }
        }

        if (host == null) {
            // 上报获取失败
            reportUserGetIP(hostName, 0);
            return HttpDnsConfig.NO_IPS;
        }

        // disable情况下直接返回空
        if (StatusManager.isDisabled()) {
            HttpDnsLog.Logd("[HttpDns] disabled return Nil.");
            // 上报获取失败
            reportUserGetIP(hostName, 0);
            return HttpDnsConfig.NO_IPS;
        }

        //如果有SDNS的缓存，返回空
        if (host.getCacheKey() != null) {
            return HttpDnsConfig.NO_IPS;
        }

        // 用户允许过期的情况下，返回ip
        if (isExpiredIPEnabled) {
            reportHttpDnsSuccess(hostName, 1);
            // 上报获取成功
            reportUserGetIP(hostName, 1);
            return host.getIps();
        }

        // 来自本地缓存
        if (host.isFromCache()) {
            HttpDnsLog.Logd("[HttpDns] ips from cache:" + Arrays.toString(host.getIps()));
            return host.getIps();
        }

        // 不过期的情况
        if (!isExpired) {
            HttpDnsLog.Logd("[HttpDns] not expired return " + Arrays.toString(host.getIps()));
            reportHttpDnsSuccess(hostName, 1);
            reportUserGetIP(hostName, 1);
            return host.getIps();
        }

        HttpDnsLog.Loge("[HttpDns] return Nil.");

        reportUserGetIP(hostName, 0);
        return HttpDnsConfig.NO_IPS;
    }

    @Override
    public void setExpiredIPEnabled(boolean enable) {
        if (!EnableManager.isEnable()) {
            HttpDnsLog.Loge("HttpDns service turned off");
            return;
        }

        this.isExpiredIPEnabled = enable;

        //上报
        ReportManager reportManager = ReportManager.getInstance();
        if (reportManager != null) {
            reportManager.reportSetExpiredIpEnabled(enable ? 1 : 0);
        }
    }

    @Override
    public void setCachedIPEnabled(boolean enable) {
        setCachedIPEnabled(enable, true);
    }

    @Override
    public void setCachedIPEnabled(boolean enable, boolean autoCleanCacheAfterLoad) {
        try {
            if (!EnableManager.isEnable()) {
                HttpDnsLog.Loge("HttpDns service turned off");
                return;
            }

            HttpDnsLog.Loge("Httpdns DB cache enable = " + enable + ". autoCleanCacheAfterLoad = " + autoCleanCacheAfterLoad);

            DBCacheManager.enable(enable, autoCleanCacheAfterLoad);
            HostManager.getInstance().updateFromDBCache();

            //上报
            ReportManager reportManager = ReportManager.getInstance();
            if (reportManager != null) {
                reportManager.reportSetCacheEnabled(enable ? 1 : 0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setAuthCurrentTime(long time) {
        if (!EnableManager.isEnable()) {
            HttpDnsLog.Loge("HttpDns service turned off");
            return;
        }

        AuthManager.setAuthCurrentTime(time);
    }

    @Override
    public void setDegradationFilter(DegradationFilter filter) {
        if (!EnableManager.isEnable()) {
            HttpDnsLog.Loge("HttpDns service turned off");
            return;
        }

        this.degradationFilter = filter;
    }

    @Override
    public void setPreResolveAfterNetworkChanged(boolean enable) {
        if (!EnableManager.isEnable()) {
            HttpDnsLog.Loge("HttpDns service turned off");
            return;
        }
        NetworkStateManager.isPreResolveAfterNetworkChangedEnabled = enable;
    }

    @Override
    public void setTimeoutInterval(int timeoutInterval) {
        if (!EnableManager.isEnable()) {
            HttpDnsLog.Loge("HttpDns service turned off");
            return;
        }

        HttpDnsConfig.setTimeoutInterval(timeoutInterval);
    }

    @Override
    public void setHTTPSRequestEnabled(boolean enabled) {
        if (!EnableManager.isEnable()) {
            HttpDnsLog.Loge("HttpDns service turned off");
            return;
        }

        HttpDnsConfig.setHTTPSRequestEnabled(enabled);
    }

    @Override
    public void setIPProbeList(List<IPProbeItem> ipProbeList) {
        if (!EnableManager.isEnable()) {
            HttpDnsLog.Loge("HttpDns service turned off");
            return;
        }

        HttpDnsConfig.setIpProbeItemList(ipProbeList);
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
    public HTTPDNSResult getIpsByHostAsync(String hostName, Map<String, String> extra, String cacheKey) {
        Map<String, String> extras = HttpDnsConfig.extra;
        extras.putAll(extra);//putAll可以合并两个MAP，如果有相同的key那么用后面的覆盖前面的

        if (!EnableManager.isEnable()) {
            HttpDnsLog.Loge("HttpDns service turned off");
            return new HTTPDNSResult(hostName, HttpDnsConfig.NO_IPS, extras);
        }

        if (!HttpDnsUtil.isAHost(hostName)) {
            // 用户输入错误情况下暂不上报调用接口成功率
            return new HTTPDNSResult(hostName, HttpDnsConfig.NO_IPS, extras);
        }
        if (HttpDnsUtil.isAnIP(hostName)) {
            // 输入IP情况下暂不调用接口成功率
            return new HTTPDNSResult(hostName, new String[]{hostName}, extras);
        }

        if (degradationFilter != null && degradationFilter.shouldDegradeHttpDNS(hostName)) {
            // 用户自定义降级逻辑，不上报
            return new HTTPDNSResult(hostName, HttpDnsConfig.NO_IPS, extras);
        }

        final HostObject host = hostManager.get(hostName);
        boolean isExpired = false;
        if ((host == null || (isExpired = host.isExpired())) && !hostManager.isResolving(hostName)) {
            if (StatusManager.isDisabled()) {
                Sniffer.getInstance().sniff(hostName);
            } else {
                HttpDnsLog.Logd("refresh host async: " + hostName);
                QueryHostTask task = new QueryHostTask(hostName, QueryType.QUERY_HOST, extras, cacheKey);
                GlobalDispatcher.getDispatcher().submit(task);
            }
        } else if (host != null) {
            //判断非上次cacheKey
            if (!cacheKey.equals(host.getCacheKey())) {
                if (StatusManager.isDisabled()) {
                    Sniffer.getInstance().sniff(hostName);
                } else {
                    HttpDnsLog.Logd("refresh host async: " + hostName);
                    QueryHostTask task = new QueryHostTask(hostName, QueryType.QUERY_HOST, extras, cacheKey);
                    GlobalDispatcher.getDispatcher().submit(task);
                }
            }
        }

        if (host == null) {
            // 上报获取失败
            reportUserGetIP(hostName, 0);
            return new HTTPDNSResult(hostName, HttpDnsConfig.NO_IPS, extras);
        }

        // disable情况下直接返回空
        if (StatusManager.isDisabled()) {
            HttpDnsLog.Logd("[HttpDns] disabled return Nil.");
            // 上报获取失败
            reportUserGetIP(hostName, 0);
            return new HTTPDNSResult(hostName, HttpDnsConfig.NO_IPS, extras);
        }

        //非上次cacheKey直接返回空
        if (!cacheKey.equals(host.getCacheKey())) {
            return new HTTPDNSResult(hostName, HttpDnsConfig.NO_IPS, extras);
        }

        // 用户允许过期的情况下，返回ip
        if (isExpiredIPEnabled) {
            reportHttpDnsSuccess(hostName, 1);
            // 上报获取成功
            reportUserGetIP(hostName, 1);
            return new HTTPDNSResult(hostName, host.getIps(), host.getExtra());
        }

        // 来自本地缓存
        if (host.isFromCache()) {
            HttpDnsLog.Logd("[HttpDns] ips from cache:" + Arrays.toString(host.getIps()));
            return new HTTPDNSResult(hostName, host.getIps(), host.getExtra());
        }

        // 不过期的情况
        if (!isExpired) {
            HttpDnsLog.Logd("[HttpDns] not expired return " + Arrays.toString(host.getIps()));
            reportHttpDnsSuccess(hostName, 1);
            reportUserGetIP(hostName, 1);
            return new HTTPDNSResult(hostName, host.getIps(), host.getExtra());
        }

        HttpDnsLog.Loge("[HttpDns] return Nil.");

        reportUserGetIP(hostName, 0);
        return new HTTPDNSResult(hostName, HttpDnsConfig.NO_IPS, extras);
    }

    @Override
    public void setSdnsGlobalParams(Map<String, String> params) {
        HttpDnsConfig.setSdnsGlobalParams(params);
    }

    @Override
    public void clearSdnsGlobalParams() {
        HttpDnsConfig.clearSdnsGlobalParams();
    }

    @Override
    public void setRegion(String region) {
        if (!EnableManager.isEnable()) {
            HttpDnsLog.Loge("HttpDns service turned off");
            return;
        }

        if (TextUtils.isEmpty(region)) {
            HttpDnsLog.Loge("region cannot be empty");
        } else {
            HttpdnsScheduleCenter.getInstance().updateServerIpsRegion(sContext, region);
        }
    }


    @Override
    public void enableIPv6(boolean enable) {
        Net64Mgr.getInstance().enableIPv6(enable);

        //上报
        try {
            ReportManager reportManager = ReportManager.getInstance();
            if (reportManager != null) {
                reportManager.reportIpv6Enabled(enable ? 1 : 0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getIPv6ByHostAsync(String hostName) {
        try {
            if (!EnableManager.isEnable()) {
                HttpDnsLog.Loge("HttpDns service turned off");
                return null;
            }

            if (Net64Mgr.getInstance().isEnable()) {
                // 触发一次v4,v6请求;
                getIpsByHostAsync(hostName);
                final HostObject host = hostManager.get(hostName);

                if (host != null) {
                    final String ipv6 = Net64Mgr.getInstance().getIPv6ByHostAsync(hostName);
                    if (isExpiredIPEnabled) {
                        HttpDnsLog.Logd("ipv6 is expired enable, hostName: " + hostName + " ipv6: " + ipv6);
                        return ipv6;
                    } else if (!host.isExpired()) {
                        HttpDnsLog.Logd("ipv6 is not expired, hostName: " + hostName + " ipv6: " + ipv6);
                        return ipv6;
                    } else if (host.isFromCache()) {
                        HttpDnsLog.Logd("ipv6 is from cache, hostName: " + hostName + " ipv6: " + ipv6);
                        return ipv6;
                    } else {
                        HttpDnsLog.Logd("ipv6 is expired.");
                        return null;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    synchronized static void switchDnsService(boolean enabled) {
        EnableManager.enable(enabled);
        if (!EnableManager.isEnable()) {
            HttpDnsLog.Loge("httpdns service disabled");
        }
    }

    private static void disableReport() {
        ReportManager.getInstance().forceDisableReportSwitch();
    }

    /**
     * 上报HttpDns成功率统计
     *
     * @param host
     * @param isSuccess
     */
    private static void reportHttpDnsSuccess(String host, int isSuccess) {
        ReportManager reportManager = ReportManager.getInstance();
        if (reportManager != null) {
            reportManager.reportHttpDnsRequestSuccess(host, isSuccess, ReportUtils.isIpv6(), DBCacheManager.isEnable() ? 1 : 0);
        }
    }

    /**
     * 上报用户调用getIP情况
     */
    private static void reportUserGetIP(String host, int success) {
        ReportManager reportManager = ReportManager.getInstance();
        if (reportManager != null) {
            reportManager.reportUserGetIpSuccess(host, success, ReportUtils.isIpv6(), DBCacheManager.isEnable() ? 1 : 0);
        }
    }

    private static void setAccountId(String accountId) {
        sAccountId = accountId;
    }

    private static void setSecretKey(String secretKey) {
        sSecretKey = secretKey;
    }

    private static String getAccountId() {
        if (!TextUtils.isEmpty(sAccountId)) {
            return sAccountId;
        }

        sAccountId = AMSConfigUtils.getAccountId(sContext);
        return sAccountId;
    }

    private static String getSecretKey() {
        if (!TextUtils.isEmpty(sSecretKey)) {
            return sSecretKey;
        }

        sSecretKey = AMSConfigUtils.getHttpdnsSecretKey(sContext);
        return sSecretKey;
    }
}
