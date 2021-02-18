package com.alibaba.sdk.android.httpdns.request;

/**
 * 网络请求的包装类
 *
 * @author zonglin.nzl
 * @date 2020/12/3
 */
public class HttpRequestWrapper<T> extends HttpRequest<T> {

    private HttpRequest<T> innerRequest;

    public HttpRequestWrapper(HttpRequest<T> request) {
        this.innerRequest = request;
    }

    @Override
    public HttpRequestConfig getRequestConfig() {
        return innerRequest.getRequestConfig();
    }

    @Override
    public T request() throws Throwable {
        return innerRequest.request();
    }
}
