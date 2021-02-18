package com.alibaba.sdk.android.httpdns.log;

import android.util.Log;

import com.alibaba.sdk.android.httpdns.ILogger;

import java.util.HashSet;

/**
 * 日志工具类
 *
 * @author zonglin.nzl
 * @date 2020/10/16
 */
public class HttpDnsLog {

    public static final String TAG = "httpdns";
    private static boolean printToLogcat = false;
    private static HashSet<ILogger> loggers = new HashSet<ILogger>();

    /**
     * 设置日志接口
     * 不受{@link #printToLogcat} 控制
     *
     * @param logger
     */
    public static void setLogger(ILogger logger) {
        if (logger != null) {
            loggers.add(logger);
        }
    }

    /**
     * 移除日志接口
     *
     * @param logger
     */
    public static void removeLogger(ILogger logger) {
        if (logger != null) {
            loggers.remove(logger);
        }
    }

    /**
     * logcat开关
     *
     * @param enable
     */
    public static void enable(boolean enable) {
        HttpDnsLog.printToLogcat = enable;
    }

    public static void e(String errLog) {
        if (HttpDnsLog.printToLogcat) {
            Log.e(TAG, errLog);
        }
        if (HttpDnsLog.loggers.size() > 0) {
            for (ILogger logger : HttpDnsLog.loggers) {
                logger.log("[E]" + errLog);
            }
        }
    }

    public static void i(String infoLog) {
        if (HttpDnsLog.printToLogcat) {
            Log.i(TAG, infoLog);
        }
        if (HttpDnsLog.loggers.size() > 0) {
            for (ILogger logger : HttpDnsLog.loggers) {
                logger.log("[I]" + infoLog);
            }
        }
    }

    public static void d(String debugLog) {
        if (HttpDnsLog.printToLogcat) {
            Log.d(TAG, debugLog);
        }
        if (HttpDnsLog.loggers.size() > 0) {
            for (ILogger logger : HttpDnsLog.loggers) {
                logger.log("[D]" + debugLog);
            }
        }
    }

    public static void w(String warnLog) {
        if (HttpDnsLog.printToLogcat) {
            Log.w(TAG, warnLog);
        }
        if (HttpDnsLog.loggers.size() > 0) {
            for (ILogger logger : HttpDnsLog.loggers) {
                logger.log("[W]" + warnLog);
            }
        }
    }

    public static void e(String errLog, Throwable throwable) {
        if (HttpDnsLog.printToLogcat) {
            Log.e(TAG, errLog, throwable);
        }
        if (HttpDnsLog.loggers.size() > 0) {
            for (ILogger logger : HttpDnsLog.loggers) {
                logger.log("[E]" + errLog);
            }
            printStackTrace(throwable);
        }
    }

    public static void w(String warnLog, Throwable throwable) {
        if (HttpDnsLog.printToLogcat) {
            Log.e(TAG, warnLog, throwable);
        }
        if (HttpDnsLog.loggers.size() > 0) {
            for (ILogger logger : HttpDnsLog.loggers) {
                logger.log("[W]" + warnLog);
            }
            printStackTrace(throwable);
        }
    }

    private static void printStackTrace(Throwable throwable) {
        if (HttpDnsLog.loggers.size() > 0) {
            for (ILogger logger : HttpDnsLog.loggers) {
                logger.log(Log.getStackTraceString(throwable));
            }
        }
    }

    /**
     * ip数组转成字符串方便输出
     *
     * @param ips
     * @return
     */
    public static String wrap(String[] ips) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[");
        for (int i = 0; i < ips.length; i++) {
            if (i != 0) {
                stringBuilder.append(",");
            }
            stringBuilder.append(ips[i]);
        }
        stringBuilder.append("]");
        return stringBuilder.toString();
    }
}
