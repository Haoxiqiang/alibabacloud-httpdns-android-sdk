package com.alibaba.sdk.android.httpdns;

import android.content.Context;
import android.content.SharedPreferences;

import com.alibaba.sdk.android.httpdns.cache.DBCacheManager;
import com.alibaba.sdk.android.httpdns.net64.Net64Mgr;
import com.alibaba.sdk.android.httpdns.report.ReportManager;
import com.alibaba.sdk.android.httpdns.report.ReportUtils;

import java.net.SocketTimeoutException;

import static com.alibaba.sdk.android.httpdns.HttpDnsLog.Loge;
import static com.alibaba.sdk.android.httpdns.StatusManager.status.DISABLE;
import static com.alibaba.sdk.android.httpdns.StatusManager.status.ENABLE;
import static com.alibaba.sdk.android.httpdns.StatusManager.status.PRE_DISABLE;

/**
 * Created by liyazhou on 17/3/24.
 * 管理当前HttpDns的状态
 */

public class StatusManager {
    private static final String HTTPDNS_CONFIG_CACHE = "httpdns_config_cache";
    private static final String STATUS = "status";
    private static final String ACTIVATED_IP_INDEX = "activiate_ip_index";
    private static final String ACTIVIATED_IP_INDEX_MODIFIED_TIME = "activiated_ip_index_modified_time";//activiatedIpIndex上次修改时间,以毫秒为单位
    private static final String DISABLE_MODIFIED_TIME = "disable_modified_time";//disable状态上次修改时间
    //private static final long ACTIVIATED_IP_INDEX_RESET_INTERVAL = BuildConfig.DEBUG ? 60 * 1000 : 2 * 60 * 60 * 1000;//时间间隔大于两小时,activiatedIpIndex重置回0。debug模式下设置1min,方便测试用
    private static final long DISABLE_RESET_INTERVAL = HttpDnsConfig.DEBUG ? 60 * 1000 : 24 * 60 * 60 * 1000; //disable机制重置时间。debug模式下设置1min,方便测试用
    private static boolean isDisabled = false;
    private static boolean isInitialized = false;
    private static SharedPreferences sp = null;
    private static volatile int activatedIpIndex = 0;
    private static volatile int startIpIndex = 0;
    private static long lastModifiedTimeMillis = 0;
    private static status currentStatus = ENABLE;

    synchronized static void init(Context context) {
        if (!isInitialized) { // 保证只会初始化一次
            synchronized (StatusManager.class) {
                if (!isInitialized) {
                    if (context != null) {
                        sp = context.getSharedPreferences(HTTPDNS_CONFIG_CACHE, Context.MODE_PRIVATE);
                    }
                    isDisabled = sp.getBoolean(STATUS, false);
                    activatedIpIndex = sp.getInt(ACTIVATED_IP_INDEX, 0);
                    startIpIndex = activatedIpIndex;

                    lastModifiedTimeMillis = sp.getLong(DISABLE_MODIFIED_TIME, 0);
                    if (System.currentTimeMillis() - lastModifiedTimeMillis >= DISABLE_RESET_INTERVAL) {
                        setDisabled(false);
                    }

                    if (isDisabled) {
                        currentStatus = DISABLE;
                    } else {
                        currentStatus = ENABLE;
                    }
                    isInitialized = true;
                }
            }
        }

    }

    static void setActiviatedIpIndex(int index) {
        if (sp != null && index >= 0 && index < HttpDnsConfig.SERVER_IPS.length) {
            activatedIpIndex = index;
            SharedPreferences.Editor editor = sp.edit();
            editor.putInt(ACTIVATED_IP_INDEX, index);
            editor.putLong(ACTIVIATED_IP_INDEX_MODIFIED_TIME, System.currentTimeMillis());
            editor.commit();
        }
    }


    synchronized static boolean isDisabled() {
        return isDisabled;
    }

    synchronized static void setDisabled(boolean disabled) {
        if (isDisabled != disabled) {
            isDisabled = disabled;
            //写入持久层
            if (sp != null) {
                SharedPreferences.Editor editor = sp.edit();
                editor.putBoolean(STATUS, isDisabled);
                editor.putLong(DISABLE_MODIFIED_TIME, System.currentTimeMillis());
                editor.commit();
            }
        }

    }

    synchronized static String getServerIp(QueryType type) {
        try {
            if (type == QueryType.QUERY_HOST || type == QueryType.SNIFF_HOST) {
                // httpdns server query
                if (currentStatus == ENABLE || currentStatus == PRE_DISABLE) {
                    // 如果是enable或者pre_disable状态,直接返回当前activiatedIpIndex指向的ip
                    return HttpDnsConfig.SERVER_IPS[activatedIpIndex];
                } else {
                    if (type == QueryType.QUERY_HOST) {
                        // 普通请求直接返回空
                        return null;
                    } else {
                        // sniff模式返回当前activiatedIpIndex指向的ip
                        return HttpDnsConfig.SERVER_IPS[activatedIpIndex];
                    }
                }
            } else if (type == QueryType.QUERY_SCHEDULE_CENTER || type == QueryType.SNIFF_SCHEDULE_CENTER) {
                // schdule center 查询.暂时直接返回null
                return null;
            } else {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    synchronized static void httpDnsRequestFailed(String hostName, String ip, Throwable throwable) {
        try {
            reportHttpDnsRequestError(ip, throwable);
            if (shouldMoveIpIndex(throwable)) {
                //timeoutException和403启动disable机制
                if (ip != null && ip.equals(HttpDnsConfig.SERVER_IPS[activatedIpIndex])) {
                    //只有和当前activiatedIpIndex指向的ip相同时才做状态迁移
                    rotateActiviatedIpIndex();

                    if (startIpIndex == activatedIpIndex) {
                        //关闭嗅探
                        Sniffer.getInstance().switchSniff(false);
                        HttpdnsScheduleCenter.getInstance().updateServerIps();
                    }

                    if (currentStatus == ENABLE) {
                        currentStatus = PRE_DISABLE;
                        Loge("enter pre_disable mode");
                    } else if (currentStatus == PRE_DISABLE) {
                        currentStatus = DISABLE;
                        Loge("enter disable mode");
                        setDisabled(true);
                        reportLocalDisable(hostName);
                        //主动发起一次嗅探
                        Sniffer.getInstance().sniff(hostName);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * httpDns请求成功
     *
     * @param hostName
     * @param ip
     * @param timeCost 请求耗时
     */
    synchronized static void httpDnsRequestSuccess(String hostName, String ip, long timeCost) {
        try {
            reportHttpDnsRequestTimeCost(hostName, ip, timeCost);
            reportHttpDnsSuccess(hostName, 1);
            if (currentStatus != ENABLE) {
                if (ip != null && ip.equals(HttpDnsConfig.SERVER_IPS[activatedIpIndex])) {
                    //只有和当前activiatedIpIndex指向的ip相同时才做状态迁移
                    Loge((currentStatus == DISABLE ? "Disable " : "Pre_disable ") + "mode finished. Enter enable mode.");
                    currentStatus = ENABLE;
                    setDisabled(false);
                    Sniffer.getInstance().stopSniff();
                    startIpIndex = activatedIpIndex;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void rotateActiviatedIpIndex() {
        if (activatedIpIndex == HttpDnsConfig.SERVER_IPS.length - 1) {
            activatedIpIndex = 0;
        } else {
            activatedIpIndex++;
        }
        setActiviatedIpIndex(activatedIpIndex);
    }

    static void onUpdateServerIpsSuccess() {
        //更新ServerIp成功,activiatedIpIndex重置为0
        setActiviatedIpIndex(0);
        startIpIndex = activatedIpIndex;
        Sniffer.getInstance().switchSniff(true);
    }

    static void onUpdateServerIpsFailed() {
        Sniffer.getInstance().switchSniff(true);
    }

    /**
     * 确定是否需要轮转IP,当前只有网络超时和服务等级不匹配需要轮转
     *
     * @param throwable
     * @return
     */
    private static boolean shouldMoveIpIndex(Throwable throwable) {
        if (throwable instanceof SocketTimeoutException) {
            return true;
        } else if (throwable instanceof HttpDnsException) {
            HttpDnsException exception = (HttpDnsException) throwable;
            if (exception.getErrorCode() == 403 && exception.getMessage().equals("ServiceLevelDeny")) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    /**
     * 上报本地disable事件
     *
     * @param hostName
     */
    private static void reportLocalDisable(String hostName) {
        try {
            ReportManager reportManager = ReportManager.getInstance();
            if (reportManager != null) {
                String scUrl;
                scUrl = HttpdnsScheduleCenter.getInstance().getScheduleCenterUrl();

                // 调用时，IP已经轮转完成，所以需要找前两个IP
                int currentIpIdex = activatedIpIndex;
                int lastIpIndex = currentIpIdex == 0 ? HttpDnsConfig.SERVER_IPS.length - 1 : currentIpIdex - 1;
                int theOneBeforeLastIpIndex = lastIpIndex == 0 ? HttpDnsConfig.SERVER_IPS.length - 1 : lastIpIndex - 1;

                if ((lastIpIndex >= 0 && lastIpIndex < HttpDnsConfig.SERVER_IPS.length) &&
                        (theOneBeforeLastIpIndex >= 0 && theOneBeforeLastIpIndex < HttpDnsConfig.SERVER_IPS.length)) {
                    String lastIp = HttpDnsConfig.SERVER_IPS[lastIpIndex];
                    String theOneBeforeLastIp = HttpDnsConfig.SERVER_IPS[theOneBeforeLastIpIndex];

                    reportManager.reportLocalDisable(hostName, scUrl, theOneBeforeLastIp + "," + lastIp);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 上报DNS解析失败场景
     *
     * @param ip
     * @param throwable
     */
    private static void reportHttpDnsRequestError(String ip, Throwable throwable) {
        try {
            ReportManager reportManager = ReportManager.getInstance();
            if (reportManager != null) {
                int errCode = ReportUtils.getErrorCode(throwable);
                String errMsg = ReportUtils.getErrorMsg(throwable);
                reportManager.reportErrorHttpDnsRequest(ip, String.valueOf(errCode), errMsg, ReportUtils.isIpv6(), Net64Mgr.getInstance().isServiceWorking() ? 1 : 0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 上报httpdns 请求时间开销
     *
     * @param host
     * @param ip
     * @param timeCost
     */
    private static void reportHttpDnsRequestTimeCost(String host, String ip, long timeCost) {
        try {
            ReportManager reportManager = ReportManager.getInstance();
            if (reportManager != null) {
                reportManager.reportHttpDnsRequestTimeCost(ip, timeCost, ReportUtils.isIpv6());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 上报HttpDns成功率统计
     *
     * @param host
     * @param isSuccess
     */
    public static void reportHttpDnsSuccess(String host, int isSuccess) {
        try {
            ReportManager reportManager = ReportManager.getInstance();
            if (reportManager != null) {
                reportManager.reportHttpDnsRequestSuccess(host, isSuccess, ReportUtils.isIpv6(), DBCacheManager.isEnable() ? 1 : 0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * ENABLE: 正常状态
     * PRE_DISABLE: ENABLE与DISABLE之间的中间状态,ENABLE下有一次请求超时进入PRE_DISBALE。PRE_DISABLE状态下超时一次进入DISABLE,成功则进入ENABLE
     * DISABLE:HttpDns不可用状态
     */
    enum status {
        ENABLE, PRE_DISABLE, DISABLE
    }
}
