package com.alibaba.sdk.android.httpdns.probe;

/**
 * IP优选的结果回调
 *
 * @author zonglin.nzl
 * @date 2020/10/23
 */
public interface ProbeCallback {
    void onResult(String host, String[] sortedIps);
}
