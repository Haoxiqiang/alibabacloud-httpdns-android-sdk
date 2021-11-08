package com.alibaba.sdk.android.httpdns.utils;

import com.alibaba.sdk.android.httpdns.exception.HttpDnsUncaughtExceptionHandler;
import com.alibaba.sdk.android.httpdns.log.HttpDnsLog;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author zonglin.nzl
 * @date 2020/12/22
 */
public class ThreadUtil {
    private static int index = 0;

    public static ExecutorService createSingleThreadService(final String tag) {
        final ThreadPoolExecutor httpdnsThread = new ThreadPoolExecutor(0, 1, 30, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(), new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r, tag + index++);
                thread.setPriority(Thread.NORM_PRIORITY - 1);
                thread.setUncaughtExceptionHandler(new HttpDnsUncaughtExceptionHandler());
                return thread;
            }
        }, new ThreadPoolExecutor.AbortPolicy());
        return new ExecutorServiceWrapper(httpdnsThread);
    }

    public static ExecutorService createExecutorService() {
        final ThreadPoolExecutor httpdnsThread = new ThreadPoolExecutor(0, 10, 30, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(), new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r, "httpdns" + index++);
                thread.setPriority(Thread.NORM_PRIORITY - 1);
                thread.setUncaughtExceptionHandler(new HttpDnsUncaughtExceptionHandler());
                return thread;
            }
        }, new ThreadPoolExecutor.AbortPolicy());
        return new ExecutorServiceWrapper(httpdnsThread);
    }

    private static class ExecutorServiceWrapper implements ExecutorService {
        private final ThreadPoolExecutor httpdnsThread;

        public ExecutorServiceWrapper(ThreadPoolExecutor httpdnsThread) {
            this.httpdnsThread = httpdnsThread;
        }

        @Override
        public void shutdown() {
            httpdnsThread.shutdown();
        }

        @Override
        public List<Runnable> shutdownNow() {
            return httpdnsThread.shutdownNow();
        }

        @Override
        public boolean isShutdown() {
            return httpdnsThread.isShutdown();
        }

        @Override
        public boolean isTerminated() {
            return httpdnsThread.isTerminated();
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            return httpdnsThread.awaitTermination(timeout, unit);
        }

        @Override
        public <T> Future<T> submit(Callable<T> task) {
            try {
                return httpdnsThread.submit(task);
            } catch (RejectedExecutionException e) {
                HttpDnsLog.e("too many request ?", e);
                throw e;
            }
        }

        @Override
        public <T> Future<T> submit(Runnable task, T result) {
            try {
                return httpdnsThread.submit(task, result);
            } catch (RejectedExecutionException e) {
                HttpDnsLog.e("too many request ?", e);
                throw e;
            }
        }

        @Override
        public Future<?> submit(Runnable task) {
            try {
                return httpdnsThread.submit(task);
            } catch (RejectedExecutionException e) {
                HttpDnsLog.e("too many request ?", e);
                throw e;
            }
        }

        @Override
        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
            try {
                return httpdnsThread.invokeAll(tasks);
            } catch (RejectedExecutionException e) {
                HttpDnsLog.e("too many request ?", e);
                throw e;
            }
        }

        @Override
        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
            try {
                return httpdnsThread.invokeAll(tasks, timeout, unit);
            } catch (RejectedExecutionException e) {
                HttpDnsLog.e("too many request ?", e);
                throw e;
            }
        }

        @Override
        public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws ExecutionException, InterruptedException {
            try {
                return httpdnsThread.invokeAny(tasks);
            } catch (RejectedExecutionException e) {
                HttpDnsLog.e("too many request ?", e);
                throw e;
            }
        }

        @Override
        public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws ExecutionException, InterruptedException, TimeoutException {
            try {
                return httpdnsThread.invokeAny(tasks, timeout, unit);
            } catch (RejectedExecutionException e) {
                HttpDnsLog.e("too many request ?", e);
                throw e;
            }
        }

        @Override
        public void execute(Runnable command) {
            try {
                httpdnsThread.execute(command);
            } catch (Exception e) {
                HttpDnsLog.e("too many request ?", e);
                throw e;
            }
        }
    }
}
