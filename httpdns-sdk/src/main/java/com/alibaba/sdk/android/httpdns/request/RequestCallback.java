package com.alibaba.sdk.android.httpdns.request;

/**
 * http请求结果回调
 *
 * @author zonglin.nzl
 * @date 2020/10/20
 */
public interface RequestCallback<T> {
    void onSuccess(T response);

    void onFail(Throwable throwable);
}
