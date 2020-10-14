package com.alibaba.sdk.android.httpdns.report;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by tomchen on 2019/2/1
 *
 * @author lianke
 */
final class ReportDispatcher {

    private static final String DEMO_NAME = "report_thread";

    private final ThreadFactory mThreadFactory = new ThreadFactory() {
        @Override
        public Thread newThread(Runnable runnable) {
            Thread result = new Thread(runnable, DEMO_NAME);
            result.setDaemon(false);
            return result;
        }
    };

    private ExecutorService mExecutorService;

    synchronized ExecutorService getExecutorService() {
        if (mExecutorService == null) {
            mExecutorService = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 1, TimeUnit.SECONDS,
                    new SynchronousQueue<Runnable>(), mThreadFactory);
        }
        return mExecutorService;
    }
}
