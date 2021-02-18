package com.alibaba.sdk.android.httpdns.request;

/**
 * http响应解析
 *
 * @author zonglin.nzl
 * @date 2020/10/20
 */
public interface ResponseTranslator<T> {
    T translate(String response) throws Throwable;
}
