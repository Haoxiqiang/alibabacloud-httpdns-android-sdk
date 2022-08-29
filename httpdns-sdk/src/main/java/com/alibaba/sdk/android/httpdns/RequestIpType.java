package com.alibaba.sdk.android.httpdns;

/**
 * 请求的ip类型
 *
 * @author zonglin.nzl
 * @date 2020/12/5
 */
public enum RequestIpType {
    v4,
    v6,
    // 表示 两个都要
    both,
    // 表示根据网络情况自动判断
    auto
}
