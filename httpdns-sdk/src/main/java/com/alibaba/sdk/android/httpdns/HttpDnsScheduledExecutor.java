package com.alibaba.sdk.android.httpdns;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;

public class HttpDnsScheduledExecutor {

    private static final int POOL_SIZE = 1;

    private static final ThreadFactory THREAD_FACTORY = new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r);
            t.setName("httpdns worker");
            t.setDaemon(false);
            t.setUncaughtExceptionHandler(new HttpDnsUncaughtExceptionHandler());
            return t;
        }
    };

    private static ScheduledExecutorService DISPATCHER;

    synchronized public static ScheduledExecutorService getScheduledExecutorService() {
        if (DISPATCHER == null) {
            DISPATCHER = new ScheduledThreadPoolExecutor(POOL_SIZE, THREAD_FACTORY);
        }
        return DISPATCHER;
    }

}
