package com.alibaba.sdk.android.httpdns;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by tomchen on 2017/8/1.
 */

public class GlobalDispatcher {

    private static final int NUMBER_OF_CORES = 0;

    private static final int NUMBER_OF_MAX = Integer.MAX_VALUE;

    private static final int KEEP_ALIVE_TIME = 1;

    private static final TimeUnit KEEP_ALIVE_TIME_UNIT = TimeUnit.SECONDS;

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

    private static final ExecutorService DISPATCHER = new ThreadPoolExecutor(
            NUMBER_OF_CORES, NUMBER_OF_MAX, KEEP_ALIVE_TIME, KEEP_ALIVE_TIME_UNIT,
            new SynchronousQueue<Runnable>(), THREAD_FACTORY);

    public static ExecutorService getDispatcher() {
        return DISPATCHER;
    }
}
