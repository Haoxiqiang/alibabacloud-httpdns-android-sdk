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
            if (watcher != null) {
                try {
                    watcher.onStart(getRequestConfig());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            T t = super.request();
            if (watcher != null) {
                try {
                    watcher.onSuccess(getRequestConfig(), t);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return t;
        } catch (Throwable throwable) {
            if (watcher != null) {
                try {
                    watcher.onFail(getRequestConfig(), throwable);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            throw throwable;
        }
    }

    public interface Watcher {
        void onStart(HttpRequestConfig config);

        void onSuccess(HttpRequestConfig config, Object data);

        void onFail(HttpRequestConfig config, Throwable throwable);
    }
}
