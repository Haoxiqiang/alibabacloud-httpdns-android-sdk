package com.alibaba.sdk.android.httpdns.report;

import android.text.TextUtils;

import com.alibaba.sdk.android.httpdns.request.HttpException;

import java.net.SocketTimeoutException;

/**
 * Created by liyazhou on 2017/9/25.
 */

public class ReportUtils {
    public static int getErrorCode(Throwable throwable) {
        if (throwable instanceof HttpException) {
            return ((HttpException) throwable).getCode();
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
}
