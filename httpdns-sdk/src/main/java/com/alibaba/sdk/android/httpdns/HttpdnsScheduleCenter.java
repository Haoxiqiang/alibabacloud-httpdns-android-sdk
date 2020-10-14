package com.alibaba.sdk.android.httpdns;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.text.TextUtils;

import com.alibaba.sdk.android.httpdns.report.ReportManager;
import com.alibaba.sdk.android.httpdns.report.ReportUtils;
import com.alibaba.sdk.android.httpdns.track.SessionTrackMgr;

import java.net.URLEncoder;

public class HttpdnsScheduleCenter {
    private String accountID;
    //访问SC更新本地HttpDNS Server ip列表的频率
    private static final long SCHEDULE_CENTER_REQUEST_INTERVAL = HttpDnsConfig.DEBUG ? 2 * 60 * 1000 : 24 * 60 * 60 * 1000;

    // 触发SC更新的频率
    private static final long SCHEDULE_CENTER_TRIAL_INTERVAL = (long) (5 * 60 * 1000);

    // httpdns config SP 名称
    private static final String HTTPDNS_CONFIG_CACHE = "httpdns_config_cache";

    // server ip key
    private static final String HTTPDNS_SERVER_IPS = "httpdns_server_ips";

    private static final String HTTPDNS_SERVER_IPSV6 = "httpdns_server_ipsv6";

    private static final String HTTPDNS_FISRST_START = "httpdns_first_start";

    private static final String HTTPDNS_REGION = "httpdns_region";

    // last request time key
    private static final String SCHEDULE_CENTER_LAST_REQUEST_TIME = "schedule_center_last_request_time";

    // sc请求协议
    private static String SCHEDULE_CENTER_REQUEST_PROTOCOL = "https://";

    // sc请求超时时间
    public static final int SCHEDULE_CENTER_REQUEST_TIMEOUT_INTERVAL = 15 * 1000;

    private int scheduleCenterUrlIndex = 0;
    private SharedPreferences sp = null;
    private static boolean isInitialized = false;
    private static String httpDnsServerIps = null;
    //    private static String[] serverIpArray;
//    private static String[] serverIpv6Array;
    private static long lastModifiedTimeMillis = 0;
    private static volatile HttpdnsScheduleCenter instance = null;
    private long lastUpdateFailTime = 0;
    private long lastUpdateRegionTime = 0;
    private boolean isFirstStart;
    private int scheduleCenterUrlIndexStart = 0;
    private boolean isServerIpAllFailed = false;
    private boolean isFirstServerIpAllFailed = true;
    private String region = null;
    public static boolean isUpdateServerIpsSuccess = false;
    private Handler regionHandler = null;

    private HttpdnsScheduleCenter() {
    }

    public static HttpdnsScheduleCenter getInstance() {
        if (instance == null) {
            synchronized (HttpdnsScheduleCenter.class) {
                if (instance == null) {
                    instance = new HttpdnsScheduleCenter();
                }
            }
        }
        return instance;
    }

    synchronized void init(Context context, String accountID) {
        try {
            if (!isInitialized) { // 保证只会初始化一次
                synchronized (HttpdnsScheduleCenter.class) {
                    if (!isInitialized) {
                        setAccountId(accountID);
                        if (context != null) {
                            sp = context.getSharedPreferences(HTTPDNS_CONFIG_CACHE, Context.MODE_PRIVATE);
                        }

                        isFirstStart = sp.getBoolean(HTTPDNS_FISRST_START, true);
                        httpDnsServerIps = sp.getString(HTTPDNS_SERVER_IPS, null);
                        region = sp.getString(HTTPDNS_REGION, null);
                        if (httpDnsServerIps != null) {
                            String[] serverIpArray = httpDnsServerIps.split(";");
                            HttpDnsConfig.setServerIps(serverIpArray);
                        }

//                    if (!Net64Mgr.getInstance().isServiceEnable()) {
//                        if (httpDnsServerIps != null) {
//                            serverIpArray = httpDnsServerIps.split(";");
//                            HttpDnsConfig.setServerIps(serverIpArray);
//                        }
//                    } else {
//                        String httpDnsServerIpsv6 = sp.getString(HTTPDNS_SERVER_IPSV6, null);
//                        if (httpDnsServerIps != null || httpDnsServerIpsv6 != null) {
//                            if (httpDnsServerIps != null) {
//                                serverIpArray = httpDnsServerIps.split(";");
//                            }
//                            if (httpDnsServerIpsv6 != null) {
//                                serverIpv6Array = httpDnsServerIpsv6.split(";");
//                            }
//                            HttpDnsConfig.setServerIps(serverIpArray, serverIpv6Array);
//                        }
//                    }

                        lastModifiedTimeMillis = sp.getLong(SCHEDULE_CENTER_LAST_REQUEST_TIME, 0);
                        if (lastModifiedTimeMillis == 0 || System.currentTimeMillis() - lastModifiedTimeMillis >= SCHEDULE_CENTER_REQUEST_INTERVAL) {
                            //TODO: 访问一次Schedule Center
                            //关闭嗅探
                            Sniffer.getInstance().switchSniff(false);
                            this.updateServerIps();
                        }
                        isInitialized = true;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void setAccountId(String accountID) {
        this.accountID = accountID;
    }

    /**
     * 更新HTTPDNS服务器ip列表
     *
     * @param serverIps
     */
    synchronized boolean setServerIps(String[] serverIps) {
        try {
            if (HttpDnsConfig.setServerIps(serverIps)) {
                StringBuilder serverIpsBuilder = new StringBuilder();
                for (int i = 0; i < serverIps.length; i++) {
                    serverIpsBuilder.append(serverIps[i]);
                    serverIpsBuilder.append(";");
                }
                serverIpsBuilder.deleteCharAt(serverIpsBuilder.length() - 1);
                if (sp != null) {
                    SharedPreferences.Editor editor = sp.edit();
                    editor.putString(HTTPDNS_SERVER_IPS, serverIpsBuilder.toString());
                    editor.putLong(SCHEDULE_CENTER_LAST_REQUEST_TIME, System.currentTimeMillis());
                    editor.commit();
                    return true;
                } else {
                    return false;
                }
            } else {
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 从远程ScheduleCenter服务器获取最新的服务器ip
     */
    synchronized void updateServerIps() {
        if (System.currentTimeMillis() - lastUpdateFailTime >= SCHEDULE_CENTER_TRIAL_INTERVAL) {
            HttpDnsLog.Logd("update server ips from StartIp schedule center.");
            this.scheduleCenterUrlIndex = 0;
            this.scheduleCenterUrlIndexStart = 0;
            this.isServerIpAllFailed = false;
            this.isFirstServerIpAllFailed = true;
            isUpdateServerIpsSuccess = false;
            if (isFirstStart) {
                HttpdnsQueryServerIpTask.getInstance().setRetryTimes(HttpDnsConfig.SCHEDULE_CENTER_URLS.length - 1);
            } else {
                HttpdnsQueryServerIpTask.getInstance().setRetryTimes(HttpDnsConfig.SERVER_IPS.length - 1);
            }
            GlobalDispatcher.getDispatcher().submit(HttpdnsQueryServerIpTask.getInstance());
        } else {
            HttpDnsLog.Logd("update server ips from StartIp schedule center too often, give up. ");
            StatusManager.onUpdateServerIpsFailed();
        }
    }

    /**
     * 从远程ScheduleCenter服务器获取最新的服务器ip
     */
    synchronized void updateServerIpsRegion(Context context, String regions) {
        try {
            if (!regions.equals(this.region)) {
                this.region = regions;
                if (System.currentTimeMillis() - lastUpdateRegionTime >= SCHEDULE_CENTER_TRIAL_INTERVAL) {
                    startUpdateServerIpsRegion();
                } else {
                    long updateServerIpsRegionTimes = SCHEDULE_CENTER_TRIAL_INTERVAL - (System.currentTimeMillis() - lastUpdateRegionTime);
                    HttpDnsLog.Logi("The call time should be greater than 5 minutes. SDK will initiate an update request after "
                            + (updateServerIpsRegionTimes / (60 * 1000)) + " minutes.");
                    if (this.regionHandler == null) {
                        this.regionHandler = new Handler();
                        this.regionHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                startUpdateServerIpsRegion();
                            }
                        }, updateServerIpsRegionTimes);
                    }
                }
                if (sp == null) {
                    if (context != null) {
                        sp = context.getSharedPreferences(HTTPDNS_CONFIG_CACHE, Context.MODE_PRIVATE);
                    } else {
                        HttpDnsLog.Loge("sp failed to save, does not affect the current settings");
                        return;
                    }
                }
                SharedPreferences.Editor editor = sp.edit();
                editor.putString(HTTPDNS_REGION, this.region);
                editor.putBoolean(HTTPDNS_FISRST_START, true);
                editor.putLong(SCHEDULE_CENTER_LAST_REQUEST_TIME, 0);
                editor.commit();
            } else {
                HttpDnsLog.Logi("region should be different");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startUpdateServerIpsRegion() {
        HttpDnsLog.Logd("update server ips from StartIp schedule center.");
        this.lastUpdateRegionTime = System.currentTimeMillis();
        this.scheduleCenterUrlIndex = 0;
        this.scheduleCenterUrlIndexStart = 0;
        this.isServerIpAllFailed = false;
        this.isFirstStart = true;
        this.isFirstServerIpAllFailed = true;
        isUpdateServerIpsSuccess = false;
        HttpdnsQueryServerIpTask.getInstance().setRetryTimes(HttpDnsConfig.SCHEDULE_CENTER_URLS.length - 1);
        GlobalDispatcher.getDispatcher().submit(HttpdnsQueryServerIpTask.getInstance());
        this.regionHandler = null;
    }

    /**
     * 返回当前指向的ScheduleCenter IP或域名
     */
    synchronized String getScheduleCenterUrl() {
        try {
            return SCHEDULE_CENTER_REQUEST_PROTOCOL + this.getIp() + "/" + (accountID == null ? HttpDnsConfig.accountID : accountID)
                    + "/ss?platform=android&sdk_version=" + BuildConfig.VERSION_NAME
                    + "&sid=" + SessionTrackMgr.getInstance().getSessionId() + "&net=" + SessionTrackMgr.getInstance().getNetType()
                    + "&bssid=" + URLEncoder.encode(SessionTrackMgr.getInstance().getBssid(), "UTF-8")
                    + (TextUtils.isEmpty(region) ? "" : ("&region=" + region));
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    //判断使用启动ip还是服务ip
    private String getIp() {
        String ip;
        if (isFirstStart || isServerIpAllFailed) {
            ip = HttpDnsConfig.SCHEDULE_CENTER_URLS[this.scheduleCenterUrlIndexStart];
        } else {
            ip = HttpDnsConfig.SERVER_IPS[this.scheduleCenterUrlIndex];
        }
        return ip;
    }

    private void getNextIpIndex() {
        if (this.scheduleCenterUrlIndex < HttpDnsConfig.SERVER_IPS.length - 1) {
            this.scheduleCenterUrlIndex++;
        } else {
            this.scheduleCenterUrlIndex = 0;
        }
    }

    private void getNextIpIndexStart() {
        if (this.scheduleCenterUrlIndexStart < HttpDnsConfig.SCHEDULE_CENTER_URLS.length - 1) {
            this.scheduleCenterUrlIndexStart++;
        } else {
            this.scheduleCenterUrlIndexStart = 0;
        }
    }

    synchronized void scheduleCenterRequestSuccess(HttpdnsScheduleCenterResponse response) {
        this.scheduleCenterUrlIndex = 0;
        HttpDns.switchDnsService(response.isEnabled());

        if (this.setServerIps(response.getServerIps())) {
            HttpDnsLog.Logd("StartIp Scheduler center update success");
            lastUpdateFailTime = System.currentTimeMillis();
            StatusManager.onUpdateServerIpsSuccess();
        }
    }

    synchronized void scheduleCenterRequestSuccess(HttpdnsScheduleCenterResponse response, long timeCost) {
        try {
            reportScRequestTimeCost(getScheduleCenterUrl(), timeCost);
            this.scheduleCenterUrlIndex = 0;
            this.scheduleCenterUrlIndexStart = 0;
            isServerIpAllFailed = false;
            this.isFirstServerIpAllFailed = true;
            HttpDns.switchDnsService(response.isEnabled());

            if (this.setServerIps(response.getServerIps())) {
                HttpDnsLog.Logd("StartIp Scheduler center update success" + "    StartIp isFirstStart：" + isFirstStart);
                isUpdateServerIpsSuccess = true;
                lastUpdateFailTime = System.currentTimeMillis();
                StatusManager.onUpdateServerIpsSuccess();
                if (isFirstStart) {
                    SharedPreferences.Editor editor = sp.edit();
                    editor.putBoolean(HTTPDNS_FISRST_START, false);
                    editor.commit();
                    isFirstStart = false;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    synchronized void scheduleCenterRequestFailed(Throwable ex) {
        try {
            isUpdateServerIpsSuccess = false;
            reportScRequestError(ex);
//            if (ex instanceof SocketTimeoutException) {
            if (isFirstStart) {//第一次启动时，更新startIp指针
                this.getNextIpIndexStart();
            } else {
                if (!isServerIpAllFailed) {//全部失败，不在更改该指针
                    this.getNextIpIndex();
                }
                if (this.scheduleCenterUrlIndex == 0) {
                    //ScheduleCenterURLS全部尝试了一遍,还是失败,使用启动ip进行更新
                    isServerIpAllFailed = true;
                    if (isFirstServerIpAllFailed) {//第一次全部失败时,新建更新请求
                        isFirstServerIpAllFailed = false;
                        this.scheduleCenterUrlIndexStart = 0;
                        HttpDnsLog.Logd("StartIp Scheduler center update from StartIp");
                        HttpdnsQueryServerIpTask.getInstance().setRetryTimes(HttpDnsConfig.SCHEDULE_CENTER_URLS.length - 1);
                        GlobalDispatcher.getDispatcher().submit(HttpdnsQueryServerIpTask.getInstance());
                    } else {
                        this.getNextIpIndexStart();
                        if (this.scheduleCenterUrlIndexStart == 0) {
                            lastUpdateFailTime = System.currentTimeMillis();
                            HttpDnsLog.Loge("StartIp Scheduler center update failed");
                            StatusManager.onUpdateServerIpsFailed();
                        }
                    }
                }
            }
//            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 上报sc请求错误
     *
     * @param ex
     */
    private void reportScRequestError(Throwable ex) {
        try {
            ReportManager reportManager = ReportManager.getInstance();
            if (reportManager != null) {
                int errCode = ReportUtils.getErrorCode(ex);
                String errMsg = ex.getMessage();
                reportManager.reportErrorSCRequest(getScheduleCenterUrl(), String.valueOf(errCode), errMsg, ReportUtils.isIpv6());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 上报SC请求时间
     *
     * @param scAddr
     * @param timeCost
     */
    private void reportScRequestTimeCost(String scAddr, long timeCost) {
        try {
            ReportManager reportManager = ReportManager.getInstance();
            if (reportManager != null) {
                reportManager.reportSCRequestTimeCost(scAddr, timeCost, ReportUtils.isIpv6());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
