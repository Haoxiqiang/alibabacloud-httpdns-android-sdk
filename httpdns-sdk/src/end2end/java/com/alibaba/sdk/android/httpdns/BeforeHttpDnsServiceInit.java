package com.alibaba.sdk.android.httpdns;

/**
 * @author zonglin.nzl
 * @date 1/14/22
 */
public interface BeforeHttpDnsServiceInit {

    void beforeInit(HttpDnsService service);
}
