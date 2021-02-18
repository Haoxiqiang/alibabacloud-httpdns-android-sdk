package com.alibaba.sdk.android.httpdns.request;

/**
 * 数据请求转异步
 *
 * @author zonglin.nzl
 * @date 2020/12/2
 */
public abstract class AsyncRequestTask<T> implements Runnable {

    private RequestCallback<T> callback;

    public AsyncRequestTask(RequestCallback<T> callback) {
        this.callback = callback;
    }

    /**
     * 请求数据，不需要直接调用，除非想要同步获取请求数据
     *
     * @return
     * @throws Throwable
     */
    public abstract T request() throws Throwable;

    @Override
    public void run() {
        try {
            T t = request();
            callback.onSuccess(t);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            callback.onFail(throwable);
        }
    }
}
