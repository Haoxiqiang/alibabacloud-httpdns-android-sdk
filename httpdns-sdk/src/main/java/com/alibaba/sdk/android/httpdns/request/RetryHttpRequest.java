package com.alibaba.sdk.android.httpdns.request;

/**
 * 失败时 重试请求
 *
 * @author zonglin.nzl
 * @date 2020/12/3
 */
public class RetryHttpRequest<T> extends HttpRequestWrapper<T> {

    private int retryCount;

    public RetryHttpRequest(HttpRequest<T> request, int retryCount) {
        super(request);
        this.retryCount = retryCount;
    }

    @Override
    public T request() throws Throwable {
        while (true) {
            try {
                return super.request();
            } catch (Throwable throwable) {
                if (retryCount > 0) {
                    retryCount--;
                } else {
                    throw throwable;
                }
            }
        }
    }
}
