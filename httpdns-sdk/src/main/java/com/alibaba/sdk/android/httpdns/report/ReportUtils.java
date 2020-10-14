package com.alibaba.sdk.android.httpdns.report;

import android.text.TextUtils;

import com.alibaba.sdk.android.httpdns.HttpDnsException;

import java.net.SocketTimeoutException;

/**
 * Created by liyazhou on 2017/9/25.
 */

public class ReportUtils {
    public static int getErrorCode(Throwable throwable) {
        if (throwable instanceof HttpDnsException) {
            return ((HttpDnsException) throwable).getErrorCode();
        } else if (throwable instanceof SocketTimeoutException) {
            return ReportConfig.ERROR_CODE_TIMEOUT;
        } else {
            return ReportConfig.ERROR_CODE_DEFAULT;
        }
    }

    public static String getErrorMsg(Throwable throwable) {
        if (throwable != null && !TextUtils.isEmpty(throwable.getMessage())) {
            return throwable.getMessage();
        } else {
            if (throwable instanceof SocketTimeoutException) {
                return ReportConfig.ERROR_MSG_TIMEOUT;
            } else {
                return ReportConfig.ERROR_MSG_DEFAULT;
            }
        }
    }


    /**
     * 判断当前是否是ipv6环境
     *
     * @return 目前统一返回0
     */
    public static int isIpv6() {
        return 0;
    }
}
