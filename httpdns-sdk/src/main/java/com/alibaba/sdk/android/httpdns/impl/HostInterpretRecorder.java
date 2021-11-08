package com.alibaba.sdk.android.httpdns.impl;

import com.alibaba.sdk.android.httpdns.RequestIpType;

import java.util.HashMap;
import java.util.HashSet;

/**
 * @author zonglin.nzl
 * @date 2020/12/22
 */
public class HostInterpretRecorder {

    private static class Recorder {
        private HashSet<String> v4ResolvingHost = new HashSet<>();
        private HashSet<String> v6ResolvingHost = new HashSet<>();
        private HashSet<String> bothResolvingHost = new HashSet<>();
        private Object lock = new Object();

        public boolean beginInterpret(String host, RequestIpType type) {

            if (type == RequestIpType.both) {
                if (bothResolvingHost.contains(host) || (v4ResolvingHost.contains(host) && v6ResolvingHost.contains(host))) {
                    // 正在解析
                    return false;
                } else {
                    synchronized (lock) {
                        if (bothResolvingHost.contains(host) || (v4ResolvingHost.contains(host) && v6ResolvingHost.contains(host))) {
                            // 正在解析
                            return false;
                        } else {
                            bothResolvingHost.add(host);
                            return true;
                        }
                    }
                }
            } else if (type == RequestIpType.v4) {
                if (v4ResolvingHost.contains(host) || bothResolvingHost.contains(host)) {
                    return false;
                } else {
                    synchronized (lock) {
                        if (v4ResolvingHost.contains(host) || bothResolvingHost.contains(host)) {
                            return false;
                        } else {
                            v4ResolvingHost.add(host);
                            return true;
                        }
                    }
                }
            } else if (type == RequestIpType.v6) {
                if (v6ResolvingHost.contains(host) || bothResolvingHost.contains(host)) {
                    return false;
                } else {
                    synchronized (lock) {
                        if (v6ResolvingHost.contains(host) || bothResolvingHost.contains(host)) {
                            return false;
                        } else {
                            v6ResolvingHost.add(host);
                            return true;
                        }
                    }
                }
            }
            return false;
        }

        public void endInterpret(String host, RequestIpType type) {
            switch (type) {
                case v4:
                    v4ResolvingHost.remove(host);
                    break;
                case v6:
                    v6ResolvingHost.remove(host);
                    break;
                case both:
                    bothResolvingHost.remove(host);
                    break;
            }
        }
    }

    private Object lock = new Object();
    private Recorder defaultRecorder = new Recorder();
    private HashMap<String, Recorder> recorders = new HashMap<>();

    public boolean beginInterpret(String host, RequestIpType type) {
        return beginInterpret(host, type, null);
    }

    public void endInterpret(String host, RequestIpType type) {
        endInterpret(host, type, null);
    }

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
}
