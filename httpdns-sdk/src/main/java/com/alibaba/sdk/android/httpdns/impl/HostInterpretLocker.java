package com.alibaba.sdk.android.httpdns.impl;

import com.alibaba.sdk.android.httpdns.RequestIpType;
import com.alibaba.sdk.android.httpdns.utils.CommonUtil;

import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author zonglin.nzl
 * @date 2020/12/22
 */
public class HostInterpretLocker {

    private HashSet<String> resolvingHost = new HashSet<>();
    private HashMap<String, CountDownLatch> latches = new HashMap<>();

    public synchronized boolean beginInterpret(String host, RequestIpType type, String cacheKey) {
        String key = CommonUtil.formKeyForAllType(host, type, cacheKey);
        boolean begin = !resolvingHost.contains(key);
        if (begin) {
            resolvingHost.add(key);
            latches.put(key, new CountDownLatch(1));
        }
        return begin;
    }


    public synchronized void endInterpret(String host, RequestIpType type, String cacheKey) {
        String key = CommonUtil.formKeyForAllType(host, type, cacheKey);
        resolvingHost.remove(key);
        CountDownLatch countDownLatch = latches.remove(key);
        if (countDownLatch != null) {
            countDownLatch.countDown();
        }
    }

    public boolean await(String host, RequestIpType type, String cacheKey, long timeout, TimeUnit unit) throws InterruptedException {
        String key = CommonUtil.formKeyForAllType(host, type, cacheKey);
        CountDownLatch countDownLatch = latches.get(key);
        if (countDownLatch != null) {
            return countDownLatch.await(timeout, unit);
        } else {
            return true;
        }
    }
}
