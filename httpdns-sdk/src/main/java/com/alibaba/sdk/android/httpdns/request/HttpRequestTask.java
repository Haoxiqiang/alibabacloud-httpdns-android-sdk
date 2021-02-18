package com.alibaba.sdk.android.httpdns.request;

/**
 * 网络请求的异步任务
 *
 * @author zonglin.nzl
 * @date 2020/12/2
 */
public class HttpRequestTask<T> extends AsyncRequestTask<T> {

    private HttpRequest<T> httpRequest;

    public HttpRequestTask(HttpRequest<T> httpRequest, RequestCallback<T> callback) {
        super(callback);
        this.httpRequest = httpRequest;
    }

    @Override
    public T request() throws Throwable {
        return httpRequest.request();
    }
}
