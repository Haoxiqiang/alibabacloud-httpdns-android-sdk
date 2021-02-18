package com.alibaba.sdk.android.httpdns.interpret;

import com.alibaba.sdk.android.httpdns.impl.HttpDnsConfig;
import com.alibaba.sdk.android.httpdns.request.HttpRequestConfig;
import com.alibaba.sdk.android.httpdns.request.RequestCallback;

/**
 * 域名解析策略接口
 *
 * @author zonglin.nzl
 * @date 2020/12/3
 */
public interface InterpretHostCategory {
    /**
     * 解析域名
     *
     * @param config
     * @param requestConfig
     * @param callback
     */
    void interpret(HttpDnsConfig config, HttpRequestConfig requestConfig, RequestCallback<InterpretHostResponse> callback);
}
