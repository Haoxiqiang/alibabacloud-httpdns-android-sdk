package com.alibaba.sdk.android.httpdns.test.utils;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author zonglin.nzl
 * @date 11/4/21
 */
public class MultiThreadTestHelper {

    public static void start(final TestTask testTask) {
        final CountDownLatch testLatch = new CountDownLatch(testTask.threadCount);
        ExecutorService service = Executors.newFixedThreadPool(testTask.threadCount);
        final CountDownLatch countDownLatch = new CountDownLatch(testTask.threadCount);
        final Throwable[] t = new Throwable[1];
        for (int i = 0; i < testTask.threadCount; i++) {
            service.execute(new Runnable() {
                @Override
                public void run() {
                    ThreadTask task = testTask.create();
                    countDownLatch.countDown();
                    try {
                        countDownLatch.await();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (task != null) {
                        task.prepare();
                        long begin = System.currentTimeMillis();
                        while (System.currentTimeMillis() - begin < testTask.executeTime) {
                            try {
                                task.execute();
                            } catch (Throwable e) {
                                t[0] = e;
                                break;
                            }
                        }
                        task.done();
                    }
                    testLatch.countDown();
                }
            });
        }
        try {
            testLatch.await();
        } catch (InterruptedException e) {
        }
        if (t[0] != null) {
            throw new RuntimeException(t[0]);
        }
        testTask.allFinish();
    }

    public interface ThreadTask {
        void prepare();

        void execute();

        void done();
    }

    public interface TaskFactory {
        ThreadTask create();

        void allFinish();
    }

    public static class TestTask implements TaskFactory {
        private int threadCount;
        private long executeTime;

        public TestTask(int threadCount, long executeTime) {
            this.threadCount = threadCount;
            this.executeTime = executeTime;
        }

        @Override
        public ThreadTask create() {
            return null;
        }

        @Override
        public void allFinish() {

        }
    }

    public static class SimpleTask extends TestTask {
        private Runnable task;

        public SimpleTask(int threadCount, long executeTime, Runnable task) {
            super(threadCount, executeTime);
            this.task = task;
        }

        @Override
        public ThreadTask create() {
            return new ThreadTask() {
                @Override
                public void prepare() {

                }

                @Override
                public void execute() {
                    task.run();
                }

                @Override
                public void done() {

                }
            };
        }
    }
}
