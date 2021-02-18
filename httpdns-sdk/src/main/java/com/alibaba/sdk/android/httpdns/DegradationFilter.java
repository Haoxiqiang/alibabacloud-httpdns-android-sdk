package com.alibaba.sdk.android.httpdns;

/**
 * 降级判断开关接口
 */
public interface DegradationFilter {
    /**
     * 是否应该不使用httpdns
     *
     * @param hostName
     * @return
     */
    boolean shouldDegradeHttpDNS(String hostName);
}
