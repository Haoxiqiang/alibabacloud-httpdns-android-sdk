package com.alibaba.sdk.android.httpdns;

import android.util.Log;

import com.alibaba.sdk.android.httpdns.report.ReportManager;

/**
 * Created by LK on 16/10/24.
 */
public class HttpDnsUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {
    // 处理所有未捕获异常
    public void uncaughtException(Thread thread, Throwable ex) {
        try {
            Log.e(HttpDnsConfig.LOG_TAG, "Catch an uncaught exception, " + thread.getName() + ", error message: " + ex.getMessage());
            reportUncaughtError(ex);
            ex.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void reportUncaughtError(Throwable ex) {
        ReportManager reportManager = ReportManager.getInstance();
        if (reportManager != null) {
            reportManager.reportErrorUncaughtException(ex.getMessage());
        }
    }
}
