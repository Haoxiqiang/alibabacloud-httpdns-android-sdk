package com.alibaba.sdk.android.httpdns.impl;

import com.alibaba.sdk.android.httpdns.RequestIpType;
import com.alibaba.sdk.android.httpdns.utils.CommonUtil;

import java.util.HashSet;

/**
 * @author zonglin.nzl
 * @date 2020/12/22
 */
public class HostInterpretRecorder {

    private Object lock = new Object();
    private HashSet<String> resolvingHost = new HashSet<>();

    public boolean beginInterpret(String host, RequestIpType type) {
        return beginInterpret(host, type, null);
    }

    public void endInterpret(String host, RequestIpType type) {
        endInterpret(host, type, null);
    }

    public boolean beginInterpret(String host, RequestIpType type, String cacheKey) {
        String key = CommonUtil.formKeyForAllType(host, type, cacheKey);
        if (type == RequestIpType.both) {
            String v4Key = CommonUtil.formKeyForAllType(host, RequestIpType.v4, cacheKey);
            String v6Key = CommonUtil.formKeyForAllType(host, RequestIpType.v6, cacheKey);
            if (resolvingHost.contains(key) || (resolvingHost.contains(v4Key) && resolvingHost.contains(v6Key))) {
                // 正在解析
                return false;
            } else {
                synchronized (lock) {
                    if (resolvingHost.contains(key) || (resolvingHost.contains(v4Key) && resolvingHost.contains(v6Key))) {
                        // 正在解析
                        return false;
                    } else {
                        resolvingHost.add(key);
                        return true;
                    }
                }
            }
        } else {
            String bothKey = CommonUtil.formKeyForAllType(host, RequestIpType.both, cacheKey);
            if (resolvingHost.contains(key) || resolvingHost.contains(bothKey)) {
                return false;
            } else {
                synchronized (lock) {
                    if (resolvingHost.contains(key) || resolvingHost.contains(bothKey)) {
                        return false;
                    } else {
                        resolvingHost.add(key);
                        return true;
                    }
                }
            }
        }
    }

    public void endInterpret(String host, RequestIpType type, String cacheKey) {
        String key = CommonUtil.formKeyForAllType(host, type, cacheKey);
        resolvingHost.remove(key);
    }
}
