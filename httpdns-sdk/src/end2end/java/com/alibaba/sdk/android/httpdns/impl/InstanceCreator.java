package com.alibaba.sdk.android.httpdns.impl;

import android.content.Context;

import com.alibaba.sdk.android.httpdns.HttpDnsService;

/**
 * @author zonglin.nzl
 * @date 2020/12/4
 */
public class InstanceCreator implements HttpDnsCreator {
    @Override
    public HttpDnsService create(Context context, String accountId, String secretKey) {
        return new HttpDnsServiceTestImpl(context.getApplicationContext(), accountId, secretKey);
    }
}
