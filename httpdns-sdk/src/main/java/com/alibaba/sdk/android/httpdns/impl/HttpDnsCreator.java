package com.alibaba.sdk.android.httpdns.impl;

import android.content.Context;

import com.alibaba.sdk.android.httpdns.HttpDnsService;

/**
 * httpdns服务创建接口
 * @author zonglin.nzl
 * @date 2020/12/4
 */
public interface HttpDnsCreator {
    public HttpDnsService create(Context context, String accountId, String secretKey);
}
