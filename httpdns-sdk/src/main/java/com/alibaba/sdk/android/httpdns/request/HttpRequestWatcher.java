package com.alibaba.sdk.android.httpdns.request;

/**
 * 监听网络请求，用于附加业务逻辑
 *
 * @author zonglin.nzl
 * @date 2020/12/3
 */
public class HttpRequestWatcher<T> extends HttpRequestWrapper<T> {

    private Watcher watcher;

    public HttpRequestWatcher(HttpRequest<T> request, Watcher watcher) {
        super(request);
        this.watcher = watcher;
    }

    @Override
    public T request() throws Throwable {
        try {
            T t = super.request();
            if (watcher != null) {
                watcher.onSuccess(getRequestConfig(), t);
            }
            return t;
        } catch (Throwable throwable) {
            if (watcher != null) {
                watcher.onFail(getRequestConfig(), throwable);
            }
            throw throwable;
        }
    }

    public interface Watcher {
        void onSuccess(HttpRequestConfig config, Object data);

        void onFail(HttpRequestConfig config, Throwable throwable);
    }
}
