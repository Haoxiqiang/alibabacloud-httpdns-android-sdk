package com.alibaba.sdk.android.httpdns;

import android.util.Log;

public class HttpDnsLog {

    private static boolean shouldPrintLog = false;
    private static int CLIENT_CODE_STACK_INDEX = -1;
    private static ILogger sLogger;

    static private String getTraceInfo() {
        try {
            if (CLIENT_CODE_STACK_INDEX == -1) {
                int i = 0;
                for (StackTraceElement ste : Thread.currentThread().getStackTrace()) {
                    if (ste.getMethodName().equals("getTraceInfo")) {
                        CLIENT_CODE_STACK_INDEX = i + 1;
                        break;
                    }
                    i++;
                }
            }
            StackTraceElement ste = Thread.currentThread().getStackTrace()[CLIENT_CODE_STACK_INDEX + 1];
            return ste.getFileName() + ":" + ste.getLineNumber() + " - [" + ste.getMethodName() + "]";
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    static void setLogger(ILogger logger) {
        sLogger = logger;
    }

    public static void Logd(String msg) {
        if (shouldPrintLog) {
            if (msg != null) {
                Log.d(HttpDnsConfig.LOG_TAG, Thread.currentThread().getId() + " - " + getTraceInfo() + " - " + msg);
            }
        }
        if (sLogger != null) {
            sLogger.log(msg);
        }
    }

    public static void Logi(String msg) {
        if (shouldPrintLog) {
            if (msg != null) {
                Log.i(HttpDnsConfig.LOG_TAG, Thread.currentThread().getId() + " - " + getTraceInfo() + " - " + msg);
            }
        }
        if (sLogger != null) {
            sLogger.log(msg);
        }
    }

    public static void Loge(String msg) {
        if (shouldPrintLog) {
            if (msg != null) {
                Log.e(HttpDnsConfig.LOG_TAG, Thread.currentThread().getId() + " - " + getTraceInfo() + " - " + msg);
            }
        }
        if (sLogger != null) {
            sLogger.log(msg);
        }
    }

    public static void Logfe(String msg) {
        if (shouldPrintLog) {
            if (msg != null) {
                Log.e(HttpDnsConfig.LOG_TAG, Thread.currentThread().getId() + " - " + getTraceInfo() + " - " + msg);
            }
        }
        if (sLogger != null) {
            sLogger.log(msg);
        }
    }

    public static void printStackTrace(Throwable e) {
        if (shouldPrintLog) {
            if (e != null) {
                e.printStackTrace();
            }
        }
    }

    synchronized static void setLogEnabled(boolean shouldPrintLog) {
        HttpDnsLog.shouldPrintLog = shouldPrintLog;
    }
}
