package com.alibaba.sdk.android.httpdns.net64;

/**
 * Created by tomchen on 2018/8/27
 *
 * @author lianke
 */
public interface Net64Service {


    /**
     * 设置是否开启httpdns-ipv6解析，开启时会解析ipv6地址
     *
     * @param enable enable为true时开启，否则不开启
     */
    void enableIPv6(boolean enable);


    /**
     * 获取ipv6地址
     *
     * @param host host为目标域名
     * @return 返回ipv6地址，当enableIPv6未开启时返回null
     */
    String getIPv6ByHostAsync(String host);
}
