package com.alibaba.sdk.android.httpdns.report;


import android.app.Application;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.alibaba.sdk.android.httpdns.BuildConfig;
import com.alibaba.sdk.android.utils.AlicloudTracker;
import com.alibaba.sdk.android.utils.AlicloudTrackerManager;
import com.alibaba.sdk.android.utils.crashdefend.SDKMessageCallback;

import java.util.HashMap;
import java.util.Map;

/**
 * utils data upload manager
 * Created by liyazhou on 2017/9/25.
 */

public class ReportManager {
    private static final String TAG = "HttpDns:ReportManager";
    private static volatile ReportManager instance = null;
    private AlicloudTracker tracker = null;
    private AlicloudTrackerManager manager = null;
    // 上报开关，默认是打开的
    private boolean reportSwitch = true;
    private boolean forceDisableReportSwitch = false;

    private ReportDispatcher mDispatcher = new ReportDispatcher();

    private ReportManager(Context context) {
        if (context != null && context.getApplicationContext() instanceof Application) {
            manager = AlicloudTrackerManager.getInstance((Application) context.getApplicationContext());
            if (manager != null) {
                this.tracker = manager.getTracker(ReportConfig.SDK_ID, BuildConfig.VERSION_NAME);
            }
        }
    }

    public static ReportManager getInstance(Context context) {
        if (instance == null) {
            synchronized (ReportManager.class) {
                if (instance == null) {
                    instance = new ReportManager(context);
                }
            }
        }
        return instance;
    }

    public static ReportManager getInstance() {
        return instance;
    }

    /**
     * 上报开关
     *
     * @param enable
     */
    public void setReportSwitch(boolean enable) {
        synchronized (ReportManager.class) {
            if (!forceDisableReportSwitch) {
                reportSwitch = enable;
            }
        }
    }

    public void forceDisableReportSwitch() {
        synchronized (ReportManager.class) {
            forceDisableReportSwitch = true;
            reportSwitch = false;
        }
    }

    /**
     * 设置accountId，将accountId写入到通用埋点参数中
     *
     * @param accountId
     */
    public void setAccountId(String accountId) {
        if (this.tracker != null) {
            this.tracker.setGlobalProperty(ReportConfig.ACCOUNT_ID, accountId);
        } else {
            Log.e(TAG, "tracker null, set global properties failed");
        }
    }

    /**
     * 上报SDK启动
     */
    public void reportSdkStart() {
        if (!reportSwitch) {
            Log.e(TAG, "report is disabled");
            return;
        }

        if (this.tracker != null) {
            mDispatcher.getExecutorService().submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        tracker.sendCustomHit(ReportConfig.EVENT_BIZ_ACTIVE, null);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        } else {
            Log.e(TAG, "report sdk start failed due to tracker is null");
        }
    }

    /**
     * 上报sniffer事件
     *
     * @param host    嗅探的域名
     * @param scAddr  当前sc的地址
     * @param srvAddr 当前httpdns server地址
     */
    public void reportSniffer(String host, String scAddr, String srvAddr) {
        if (!reportSwitch) {
            Log.e(TAG, "report is disabled");
            return;
        }

        if (this.tracker != null) {
            if (TextUtils.isEmpty(host) || TextUtils.isEmpty(scAddr) || TextUtils.isEmpty(srvAddr)) {
                Log.e(TAG, "report sniffer failed due to missing params");
                return;
            }

            final Map<String, String> params = new HashMap<>();
            params.put(ReportConfig.KEY_HOST, host);
            params.put(ReportConfig.KEY_SC_ADDR, scAddr);
            params.put(ReportConfig.KEY_SERVER_ADDR, srvAddr);

            mDispatcher.getExecutorService().submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        tracker.sendCustomHit(ReportConfig.EVENT_BIZ_SNIFFER, params);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        } else {
            Log.e(TAG, "report sniffer failed due to tracker is null");
        }
    }

    /**
     * 上报disable事件
     *
     * @param host
     * @param scAddr
     * @param srvAddrs：连续两次访问失败的httpdns服务器地址 如："0.0.0.0,1.0.0.1"
     */
    public void reportLocalDisable(String host, String scAddr, String srvAddrs) {
        if (!reportSwitch) {
            Log.e(TAG, "report is disabled");
            return;
        }

        if (this.tracker != null) {
            if (TextUtils.isEmpty(host) || TextUtils.isEmpty(scAddr) || TextUtils.isEmpty(srvAddrs)) {
                Log.e(TAG, "report local disable failed due to missing params");
                return;
            }

            final Map<String, String> params = new HashMap<>();
            params.put(ReportConfig.KEY_HOST, host);
            params.put(ReportConfig.KEY_SC_ADDR, scAddr);
            params.put(ReportConfig.KEY_SERVER_ADDR, srvAddrs);

            mDispatcher.getExecutorService().submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        tracker.sendCustomHit(ReportConfig.EVENT_BIZ_LOCAL_DISABLE, params);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        } else {
            Log.e(TAG, "report local disable failed due to tracker is null");
        }
    }

    /**
     * 用户调用启用持久化缓存功能接口时
     *
     * @param enable 是否启用持久环缓存，0为关闭，1为启用
     */
    public void reportSetCacheEnabled(int enable) {
        if (!reportSwitch) {
            Log.e(TAG, "report is disabled");
            return;
        }

        if (this.tracker != null) {
            if (enable != 0 && enable != 1) {
                Log.e(TAG, "report cache failed, due to invalid param enable, enable can only be 0 or 1");
                return;
            }

            final Map<String, String> params = new HashMap<>();
            params.put(ReportConfig.KEY_ENABLE, String.valueOf(enable));

            mDispatcher.getExecutorService().submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        tracker.sendCustomHit(ReportConfig.EVENT_BIZ_CACHE, params);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        } else {
            Log.e(TAG, "report cache failed due to tracker is null");
        }
    }


    /**
     * 用户调用setExpiredIPEnabled接口，允许过期ip
     *
     * @param enable 是否允许过期ip，0为不允许，1为允许
     */
    public void reportSetExpiredIpEnabled(int enable) {
        if (!reportSwitch) {
            Log.e(TAG, "report is disabled");
            return;
        }

        if (this.tracker != null) {
            if (enable != 0 && enable != 1) {
                Log.e(TAG, "report set expired ip enabled failed, due to invalid param enable, enable can only be 0 or 1");
                return;
            }

            final Map<String, String> params = new HashMap<>();
            params.put(ReportConfig.KEY_ENABLE, String.valueOf(enable));

            mDispatcher.getExecutorService().submit(new Runnable() {
                @Override
                public void run() {

                    try {
                        tracker.sendCustomHit(ReportConfig.EVENT_BIZ_EXPIRED_IP, params);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        } else {
            Log.e(TAG, "report set expired ip enabled failed due to tracker is null");
        }
    }

    /**
     * 允许解析返回ipv6地址
     *
     * @param enable 0为关闭，1为启用
     */
    public void reportIpv6Enabled(int enable) {
        try {
            if (!reportSwitch) {
                Log.e(TAG, "report is disabled");
                return;
            }

            if (this.tracker != null) {
                if (enable != 0 && enable != 1) {
                    Log.e(TAG, "report ipv6 failed, due to invalid param enable, enable can only be 0 or 1");
                    return;
                }

                final Map<String, String> params = new HashMap<>();
                params.put(ReportConfig.KEY_ENABLE, String.valueOf(enable));

                mDispatcher.getExecutorService().submit(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            tracker.sendCustomHit(ReportConfig.EVENT_BIZ_IPV6_ENABLE, params);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            } else {
                Log.e(TAG, "report ipv6 failed due to tracker is null");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * 允许ipv6解析服务
     *
     * @param enable 0为关闭，1为启用
     */
    public void reportIpv6SrvEnabled(int enable) {
        try {
            if (!reportSwitch) {
                Log.e(TAG, "report is disabled");
                return;
            }

            if (this.tracker != null) {
                if (enable != 0 && enable != 1) {
                    Log.e(TAG, "report ipv6_srv failed, due to invalid param enable, enable can only be 0 or 1");
                    return;
                }

                final Map<String, String> params = new HashMap<>();
                params.put(ReportConfig.KEY_ENABLE, String.valueOf(enable));

                mDispatcher.getExecutorService().submit(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            tracker.sendCustomHit(ReportConfig.EVENT_BIZ_IPV6_SRV_ENABLE, params);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });

            } else {
                Log.e(TAG, "report ipv6_srv failed due to tracker is null");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * SC请求异常埋点
     *
     * @param scAddr  SC服务器ip/host
     * @param errCode 错误码
     * @param errMsg  错误信息
     * @param isIpv6  是否ipv6，0为否，1位是
     */
    public void reportErrorSCRequest(String scAddr, String errCode, String errMsg, int isIpv6) {
        if (!reportSwitch) {
            Log.e(TAG, "report is disabled");
            return;
        }

        if (this.tracker != null) {
            if (TextUtils.isEmpty(scAddr) || TextUtils.isEmpty(errCode) || TextUtils.isEmpty(errMsg)
                    || (isIpv6 != 0 && isIpv6 != 1)) {
                Log.e(TAG, "report error sc failed, due to invalid params");
                return;
            }

            final Map<String, String> params = new HashMap<>();
            params.put(ReportConfig.KEY_SC_ADDR, scAddr);
            params.put(ReportConfig.KEY_ERR_CODE, errCode);
            params.put(ReportConfig.KEY_ERR_MSG, errMsg);
            params.put(ReportConfig.KEY_IPV6, String.valueOf(isIpv6));

            mDispatcher.getExecutorService().submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        tracker.sendCustomHit(ReportConfig.EVENT_ERR_SC, params);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        } else {
            Log.e(TAG, "report error sc failed due to tacker is null");
        }
    }

    /**
     * httpdns请求异常埋点
     *
     * @param srvAddr httpdns服务器ip/host
     * @param errCode 错误码
     * @param errMsg  错误信息
     * @param isIpv6  是否ipv6，0为否，1位是
     */
    public void reportErrorHttpDnsRequest(String srvAddr, String errCode, String errMsg, int isIpv6, int isIpv6_srv) {
        try {
            if (!reportSwitch) {
                Log.e(TAG, "report is disabled");
                return;
            }

            if (this.tracker != null) {
                if (TextUtils.isEmpty(srvAddr) || TextUtils.isEmpty(errCode) || TextUtils.isEmpty(errMsg)
                        || (isIpv6 != 0 && isIpv6 != 1) || (isIpv6_srv != 0 && isIpv6_srv != 1)) {
                    Log.e(TAG, "report error http dns request failed, due to invalid params");
                    return;
                }

                final Map<String, String> params = new HashMap<>();
                params.put(ReportConfig.KEY_SERVER_ADDR, srvAddr);
                params.put(ReportConfig.KEY_ERR_CODE, errCode);
                params.put(ReportConfig.KEY_ERR_MSG, errMsg);
                params.put(ReportConfig.KEY_IPV6, String.valueOf(isIpv6));
                params.put(ReportConfig.KEY_IPV6_SRV, String.valueOf(isIpv6_srv));

                mDispatcher.getExecutorService().submit(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            tracker.sendCustomHit(ReportConfig.EVENT_ERR_SRV, params);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            } else {
                Log.e(TAG, "report error http dns request failed due to tacker is null");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * 非捕获异常埋点
     *
     * @param exception 异常信息
     */
    public void reportErrorUncaughtException(String exception) {
        if (!reportSwitch) {
            Log.e(TAG, "report is disabled");
            return;
        }

        if (this.tracker != null) {
            if (TextUtils.isEmpty(exception)) {
                Log.e(TAG, "report uncaught exception failed due to exception msg is null");
                return;
            }

            final Map<String, String> params = new HashMap<>();
            params.put(ReportConfig.KEY_EXCEPTION, exception);

            mDispatcher.getExecutorService().submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        tracker.sendCustomHit(ReportConfig.EVENT_ERR_UNCAUGHT_EXCEPTION, params);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        } else {
            Log.e(TAG, "report uncaught exception failed due to tacker is null");
        }
    }


    /**
     * SC访问耗时埋点
     *
     * @param scAddr SC服务器地址
     * @param cost   请求耗时(ms)
     * @param isIpv6 是否ipv6，0为否，1位是
     */
    public void reportSCRequestTimeCost(String scAddr, long cost, int isIpv6) {
        if (!reportSwitch) {
            Log.e(TAG, "report is disabled");
            return;
        }

        if (this.tracker != null) {
            if (TextUtils.isEmpty(scAddr) || cost <= 0 || (isIpv6 != 0 && isIpv6 != 1)) {
                Log.e(TAG, "report sc request time cost failed due to invalid params");
                return;
            }

            if (cost > ReportConfig.MAX_SC_REQUEST_TIME) {
                cost = ReportConfig.MAX_SC_REQUEST_TIME;
            }

            final Map<String, String> params = new HashMap<>();
            params.put(ReportConfig.KEY_SC_ADDR, scAddr);
            params.put(ReportConfig.KEY_COST, String.valueOf(cost));
            params.put(ReportConfig.KEY_IPV6, String.valueOf(isIpv6));

            mDispatcher.getExecutorService().submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        tracker.sendCustomHit(ReportConfig.EVENT_PERF_SC, params);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        } else {
            Log.e(TAG, "report sc request time cost failed due to tacker is null");
        }
    }


    /**
     * httpdns访问耗时埋点
     *
     * @param srvAddr httpdns server 地址
     * @param cost    请求耗时(ms)
     * @param isIpv6  是否ipv6，0为否，1位是
     */
    public void reportHttpDnsRequestTimeCost(String srvAddr, long cost, int isIpv6) {
        if (!reportSwitch) {
            Log.e(TAG, "report is disabled");
            return;
        }

        if (this.tracker != null) {
            if (TextUtils.isEmpty(srvAddr) || cost <= 0 || (isIpv6 != 0 && isIpv6 != 1)) {
                Log.e(TAG, "report http dns request time cost failed due to invalid param");
                return;
            }

            if (cost > ReportConfig.MAX_HTTPDNS_REQUEST_TIME) {
                cost = ReportConfig.MAX_HTTPDNS_REQUEST_TIME;
            }

            final Map<String, String> params = new HashMap<>();
            params.put(ReportConfig.KEY_SERVER_ADDR, srvAddr);
            params.put(ReportConfig.KEY_COST, String.valueOf(cost));
            params.put(ReportConfig.KEY_IPV6, String.valueOf(isIpv6));

            mDispatcher.getExecutorService().submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        tracker.sendCustomHit(ReportConfig.EVENT_PERF_SRV, params);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        } else {
            Log.e(TAG, "report http dns request time cost failed due to tacker is null");
        }
    }


    /**
     * SDK解析成功埋点
     *
     * @param host        查询的host
     * @param success     返回的ip是否为空（成功=1，失败=0）
     * @param isIpv6      是否ipv6，0为否，1位是
     * @param isCacheOpen 是否启用持久环缓存，0为关闭，1为启用
     */
    public void reportHttpDnsRequestSuccess(String host, int success, int isIpv6, int isCacheOpen) {
        if (!reportSwitch) {
            Log.e(TAG, "report is disabled");
            return;
        }

        if (this.tracker != null) {
            if (TextUtils.isEmpty(host) ||
                    (success != 0 && success != 1) ||
                    (isIpv6 != 0 && isIpv6 != 1) ||
                    (isCacheOpen != 0 && isCacheOpen != 1)) {
                Log.e(TAG, "report http dns success failed due to invalid params");
                return;
            }

            final Map<String, String> params = new HashMap<>();
            params.put(ReportConfig.KEY_HOST, host);
            params.put(ReportConfig.KEY_SUCCESS, String.valueOf(success));
            params.put(ReportConfig.KEY_IPV6, String.valueOf(isIpv6));
            params.put(ReportConfig.KEY_CACHE_OPEN, String.valueOf(isCacheOpen));

            mDispatcher.getExecutorService().submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        tracker.sendCustomHit(ReportConfig.EVENT_PERF_GET_IP_SUCCESS, params);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        } else {
            Log.e(TAG, "report http dns succes failed due to tacker is null");
        }
    }

    /**
     * 用户通过getIpByHostAsync获取ip的成功率
     *
     * @param host        查询的host
     * @param success     返回的ip是否为空（成功=1，失败=0）
     * @param isIpv6      是否ipv6，0为否，1位是
     * @param isCacheOpen 是否启用持久环缓存，0为关闭，1为启用
     */
    public void reportUserGetIpSuccess(String host, int success, int isIpv6, int isCacheOpen) {
        if (!reportSwitch) {
            Log.e(TAG, "report is disabled");
            return;
        }

        if (this.tracker != null) {
            if (TextUtils.isEmpty(host) ||
                    (success != 0 && success != 1) ||
                    (isIpv6 != 0 && isIpv6 != 1) ||
                    (isCacheOpen != 0 && isCacheOpen != 1)) {
                Log.e(TAG, "report http dns success failed due to invalid params");
                return;
            }

            final Map<String, String> params = new HashMap<>();
            params.put(ReportConfig.KEY_HOST, host);
            params.put(ReportConfig.KEY_SUCCESS, String.valueOf(success));
            params.put(ReportConfig.KEY_IPV6, String.valueOf(isIpv6));
            params.put(ReportConfig.KEY_CACHE_OPEN, String.valueOf(isCacheOpen));

            mDispatcher.getExecutorService().submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        tracker.sendCustomHit(ReportConfig.EVENT_PERF_USER_GET_IP_SUCCESS, params);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        } else {
            Log.e(TAG, "report http dns succes failed due to tacker is null");
        }
    }

    /**
     * 上报IP优选功能
     *
     * @param host
     * @param defaultIp
     * @param selectedIp
     * @param defaultIpCost
     * @param selectedIpCost
     * @param ipCount
     */
    public void reportIpSelection(String host, String defaultIp, String selectedIp, long defaultIpCost, long selectedIpCost, int ipCount) {
        if (!reportSwitch) {
            Log.e(TAG, "report is disabled");
            return;
        }

        if (this.tracker != null) {
            if (TextUtils.isEmpty(host) || TextUtils.isEmpty(defaultIp) || TextUtils.isEmpty(selectedIp) || ipCount <= 0) {
                Log.e(TAG, "report ip selection failed due to invalid params");
                return;
            }

            if (defaultIpCost > ReportConfig.MAX_IP_PROBE_TIME) {
                defaultIpCost = ReportConfig.MAX_IP_PROBE_TIME;
            }

            if (selectedIpCost > ReportConfig.MAX_IP_PROBE_TIME) {
                selectedIpCost = ReportConfig.MAX_IP_PROBE_TIME;
            }

            final Map<String, String> params = new HashMap<>();
            params.put(ReportConfig.KEY_HOST, host);
            params.put(ReportConfig.KEY_DEFAULT_IP, defaultIp);
            params.put(ReportConfig.KEY_SELECTED_IP, selectedIp);
            params.put(ReportConfig.KEY_DEFAULT_IP_COST, String.valueOf(defaultIpCost));
            params.put(ReportConfig.KEY_SELECTED_IP_COST, String.valueOf(selectedIpCost));
            params.put(ReportConfig.KEY_IP_COUNT, String.valueOf(ipCount));

            mDispatcher.getExecutorService().submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        tracker.sendCustomHit(ReportConfig.EVENT_PERF_IP_SELECTION, params);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        } else {
            Log.e(TAG, "report ip selection failed due to tacker is null");
        }
    }

    public boolean registerCrash(SDKMessageCallback callback) {
        try {
            if (manager != null) {
                return manager.registerCrashDefend(ReportConfig.SDK_ID, BuildConfig.VERSION_NAME, ReportConfig.CRASH_DEFEND_LIMIT, ReportConfig.CRASH_DEFEND_TIME, callback);
            } else {
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


}
