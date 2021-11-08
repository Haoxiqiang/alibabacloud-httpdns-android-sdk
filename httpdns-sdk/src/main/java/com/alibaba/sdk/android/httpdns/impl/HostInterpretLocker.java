package com.alibaba.sdk.android.httpdns.impl;

import com.alibaba.sdk.android.httpdns.RequestIpType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author zonglin.nzl
 * @date 2020/12/22
 */
public class HostInterpretLocker {

    private static class Recorder {
        private HashSet<String> v4ResolvingHost = new HashSet<>();
        private HashSet<String> v6ResolvingHost = new HashSet<>();
        private HashSet<String> bothResolvingHost = new HashSet<>();
        private HashMap<String, CountDownLatch> v4Latchs = new HashMap<>();
        private HashMap<String, CountDownLatch> v6Latchs = new HashMap<>();
        private HashMap<String, CountDownLatch> bothLatchs = new HashMap<>();
        private Object lock = new Object();

        public boolean beginInterpret(String host, RequestIpType type) {

            if (type == RequestIpType.both) {
                if (bothResolvingHost.contains(host)) {
                    // 正在解析
                    return false;
                } else {
                    synchronized (lock) {
                        if (bothResolvingHost.contains(host)) {
                            // 正在解析
                            return false;
                        } else {
                            bothResolvingHost.add(host);
                            createLatch(host, bothLatchs);
                            return true;
                        }
                    }
                }
            } else if (type == RequestIpType.v4) {
                if (v4ResolvingHost.contains(host)) {
                    return false;
                } else {
                    synchronized (lock) {
                        if (v4ResolvingHost.contains(host)) {
                            return false;
                        } else {
                            v4ResolvingHost.add(host);
                            createLatch(host, v4Latchs);
                            return true;
                        }
                    }
                }
            } else if (type == RequestIpType.v6) {
                if (v6ResolvingHost.contains(host)) {
                    return false;
                } else {
                    synchronized (lock) {
                        if (v6ResolvingHost.contains(host)) {
                            return false;
                        } else {
                            v6ResolvingHost.add(host);
                            createLatch(host, v6Latchs);
                            return true;
                        }
                    }
                }
            }
            return false;
        }

        private void createLatch(String host, HashMap<String, CountDownLatch> latchs) {
            CountDownLatch countDownLatch = new CountDownLatch(1);
            latchs.put(host, countDownLatch);
        }

        private void countDownLatch(String host, HashMap<String, CountDownLatch> latchs) {
            CountDownLatch latch = latchs.get(host);
            if (latch != null) {
                latch.countDown();
            }
        }

        private CountDownLatch getLatch(String host, RequestIpType type) {
            switch (type) {
                case v4:
                    return v4Latchs.get(host);
                case v6:
                    return v6Latchs.get(host);
                case both:
                    return bothLatchs.get(host);
            }
            return null;
        }

        public void endInterpret(String host, RequestIpType type) {
            switch (type) {
                case v4:
                    v4ResolvingHost.remove(host);
                    countDownLatch(host, v4Latchs);
                    break;
                case v6:
                    v6ResolvingHost.remove(host);
                    countDownLatch(host, v6Latchs);
                    break;
                case both:
                    bothResolvingHost.remove(host);
                    countDownLatch(host, bothLatchs);
                    break;
            }
        }

        public boolean await(String host, RequestIpType type, long timeout, TimeUnit unit) throws InterruptedException {
            CountDownLatch countDownLatch = getLatch(host, type);
            if (countDownLatch != null) {
                return countDownLatch.await(timeout, unit);
            } else {
                return true;
            }
        }
    }

    private Object lock = new Object();
    private Recorder defaultRecorder = new Recorder();
    private HashMap<String, Recorder> recorders = new HashMap<>();

    public boolean beginInterpret(String host, RequestIpType type, String cacheKey) {
        Recorder recorder = getRecorder(cacheKey);
        return recorder.beginInterpret(host, type);
    }

    private Recorder getRecorder(String cacheKey) {
        Recorder recorder = null;
        if (cacheKey == null || cacheKey.isEmpty()) {
            recorder = defaultRecorder;
        } else {
            recorder = recorders.get(cacheKey);
            if (recorder == null) {
                synchronized (lock) {
                    recorder = recorders.get(cacheKey);
                    if (recorder == null) {
                        recorder = new Recorder();
                        recorders.put(cacheKey, recorder);
                    }
                }
            }
        }
        return recorder;
    }

    public void endInterpret(String host, RequestIpType type, String cacheKey) {
        Recorder recorder = getRecorder(cacheKey);
        recorder.endInterpret(host, type);
    }

    public boolean await(String host, RequestIpType type, String cacheKey, long timeout, TimeUnit unit) throws InterruptedException {
        Recorder recorder = getRecorder(cacheKey);
        return recorder.await(host, type, timeout, unit);
    }
}
