package com.alibaba.sdk.android.httpdns.report;


import android.app.Application;
import android.content.Context;
import android.text.TextUtils;

import com.alibaba.sdk.android.httpdns.BuildConfig;
import com.alibaba.sdk.android.httpdns.log.HttpDnsLog;
import com.alibaba.sdk.android.utils.AlicloudTracker;
import com.alibaba.sdk.android.utils.AlicloudTrackerManager;
import com.alibaba.sdk.android.utils.crashdefend.SDKMessageCallback;

import java.util.HashMap;

/**
 * utils data upload manager
 * Created by liyazhou on 2017/9/25.
 */

public class ReportManager {
    private static final String TAG = "HttpDns:ReportManager";
    private static AlicloudTracker tracker = null;
    private static AlicloudTrackerManager manager = null;
    private static ReportDispatcher mDispatcher = new ReportDispatcher();
    private static StringBuilder accountIds = new StringBuilder();
    // 上报开关，默认是打开的
    private boolean reportSwitch = true;
    private String accountId = null;

    public static void init(Context context) {
        if (context != null && context.getApplicationContext() instanceof Application) {
            if (manager == null) {
                manager = AlicloudTrackerManager.getInstance((Application) context.getApplicationContext());
            }
            if (manager != null && tracker == null) {
                tracker = manager.getTracker(ReportConfig.SDK_ID, BuildConfig.VERSION_NAME);
            }
        }
    }

    private ReportManager(String accountIds) {
        this.accountId = accountIds;
    }

    public static ReportManager getDefaultReportManager() {
        if (manager == null || tracker == null) {
            return null;
        }
        return new ReportManager(accountIds.toString());
    }

    public static ReportManager getReportManagerByAccount(String accountId) {
        if (manager == null || tracker == null) {
            return null;
        }
        return new ReportManager(accountId);
    }

    /**
     * 设置accountId，将accountId写入到通用埋点参数中
     *
     * @param accountId
     */
    public void setAccountId(String accountId) {
        this.accountId = accountId;
        accountIds.append(accountId);
    }

    private HashMap<String, String> wrapAccountId(HashMap<String, String> params) {
        if (params == null) {
            params = new HashMap<>();
        }
        params.put(ReportConfig.ACCOUNT_ID, this.accountId);
        return params;
    }

    /**
     * 上报SDK启动
     */
    public void reportSdkStart() {
        if (!reportSwitch) {
            HttpDnsLog.e(TAG + "report is disabled");
            return;
        }

        if (tracker != null) {
            mDispatcher.getExecutorService().submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        tracker.sendCustomHit(ReportConfig.EVENT_BIZ_ACTIVE, wrapAccountId(null));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        } else {
            HttpDnsLog.e(TAG + "report sdk start failed due to tracker is null");
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
            HttpDnsLog.e(TAG + "report is disabled");
            return;
        }

        if (tracker != null) {
            if (TextUtils.isEmpty(scAddr) || TextUtils.isEmpty(errCode) || TextUtils.isEmpty(errMsg)
                    || (isIpv6 != 0 && isIpv6 != 1)) {
                HttpDnsLog.e(TAG + "report error sc failed, due to invalid params");
                return;
            }

            final HashMap<String, String> params = new HashMap<>();
            params.put(ReportConfig.KEY_SC_ADDR, scAddr);
            params.put(ReportConfig.KEY_ERR_CODE, errCode);
            params.put(ReportConfig.KEY_ERR_MSG, errMsg);
            params.put(ReportConfig.KEY_IPV6, String.valueOf(isIpv6));

            mDispatcher.getExecutorService().submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        tracker.sendCustomHit(ReportConfig.EVENT_ERR_SC, wrapAccountId(params));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        } else {
            HttpDnsLog.e(TAG + "report error sc failed due to tacker is null");
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
                HttpDnsLog.e(TAG + "report is disabled");
                return;
            }

            if (tracker != null) {
                if (TextUtils.isEmpty(srvAddr) || TextUtils.isEmpty(errCode) || TextUtils.isEmpty(errMsg)
                        || (isIpv6 != 0 && isIpv6 != 1) || (isIpv6_srv != 0 && isIpv6_srv != 1)) {
                    HttpDnsLog.e(TAG + "report error http dns request failed, due to invalid params");
                    return;
                }

                final HashMap<String, String> params = new HashMap<>();
                params.put(ReportConfig.KEY_SERVER_ADDR, srvAddr);
                params.put(ReportConfig.KEY_ERR_CODE, errCode);
                params.put(ReportConfig.KEY_ERR_MSG, errMsg);
                params.put(ReportConfig.KEY_IPV6, String.valueOf(isIpv6));
                params.put(ReportConfig.KEY_IPV6_SRV, String.valueOf(isIpv6_srv));

                mDispatcher.getExecutorService().submit(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            tracker.sendCustomHit(ReportConfig.EVENT_ERR_SRV, wrapAccountId(params));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            } else {
                HttpDnsLog.e(TAG + "report error http dns request failed due to tacker is null");
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
            HttpDnsLog.e(TAG + "report is disabled");
            return;
        }

        if (tracker != null) {
            if (TextUtils.isEmpty(exception)) {
                HttpDnsLog.e(TAG + "report uncaught exception failed due to exception msg is null");
                return;
            }

            final HashMap<String, String> params = new HashMap<>();
            params.put(ReportConfig.KEY_EXCEPTION, exception);

            mDispatcher.getExecutorService().submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        tracker.sendCustomHit(ReportConfig.EVENT_ERR_UNCAUGHT_EXCEPTION, wrapAccountId(params));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        } else {
            HttpDnsLog.e(TAG + "report uncaught exception failed due to tacker is null");
        }
    }

    public static boolean registerCrash(Context context, SDKMessageCallback callback) {
        try {
            AlicloudTrackerManager manager = AlicloudTrackerManager.getInstance((Application) context.getApplicationContext());
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
