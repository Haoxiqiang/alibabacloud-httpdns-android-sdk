package com.alibaba.sdk.android.httpdns.probe;

/**
 * Created by liyazhou on 2017/12/14.
 */

public interface IPListUpdateCallback {
    /**
     * IP不可用
     *
     * @param hostName
     * @param ips
     */
    void onIPListUpdate(String hostName, String[] ips);
}
