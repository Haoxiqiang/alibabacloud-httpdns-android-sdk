package com.alibaba.sdk.android.httpdns;

/**
 * Created by liyazhou on 17/3/27.
 * HttpDnsException用于描述请求HttpDns的错误返回
 */

public class HttpDnsException extends Exception {
    private int errorCode;

    public HttpDnsException(int errorCode, String detailMessage) {
        super(detailMessage);
        this.errorCode = errorCode;
    }

    public int getErrorCode() {
        return this.errorCode;
    }
}
