package com.alibaba.sdk.android.httpdns;

/**
 * @author zonglin.nzl
 * @date 2020/12/21
 */
public interface SyncService {
    /**
     * 同步解析接口，必须在子线程中执行，否则没有效果
     *
     * @param host
     * @return
     */
    HTTPDNSResult getByHost(String host, RequestIpType type);
}
