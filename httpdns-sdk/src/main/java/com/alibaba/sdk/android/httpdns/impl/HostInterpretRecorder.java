package com.alibaba.sdk.android.httpdns.impl;

import com.alibaba.sdk.android.httpdns.RequestIpType;
import com.alibaba.sdk.android.httpdns.utils.CommonUtil;

import java.util.HashSet;

/**
 * @author zonglin.nzl
 * @date 2020/12/22
 */
public class HostInterpretRecorder {

    private HashSet<String> resolvingHost = new HashSet<>();

    public boolean beginInterpret(String host, RequestIpType type) {
        return beginInterpret(host, type, null);
    }

    public void endInterpret(String host, RequestIpType type) {
        endInterpret(host, type, null);
    }

    public synchronized boolean beginInterpret(String host, RequestIpType type, String cacheKey) {
        String key = CommonUtil.formKeyForAllType(host, type, cacheKey);
        if (type == RequestIpType.both) {
            String v4Key = CommonUtil.formKeyForAllType(host, RequestIpType.v4, cacheKey);
            String v6Key = CommonUtil.formKeyForAllType(host, RequestIpType.v6, cacheKey);
            boolean begin = !resolvingHost.contains(key) && !(resolvingHost.contains(v4Key) && resolvingHost.contains(v6Key));
            if (begin) {
                resolvingHost.add(key);
            }
            return begin;
        } else {
            String bothKey = CommonUtil.formKeyForAllType(host, RequestIpType.both, cacheKey);
            boolean begin = !resolvingHost.contains(key) && !resolvingHost.contains(bothKey);
            if (begin) {
                resolvingHost.add(key);
            }
            return begin;
        }
    }

    public synchronized void endInterpret(String host, RequestIpType type, String cacheKey) {
        String key = CommonUtil.formKeyForAllType(host, type, cacheKey);
        resolvingHost.remove(key);
    }
}
