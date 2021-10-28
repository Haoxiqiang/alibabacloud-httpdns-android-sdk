package com.aliyun.ams.httpdns.demo;

import com.alibaba.sdk.android.httpdns.HTTPDNSResult;
import com.alibaba.sdk.android.httpdns.HttpDnsService;
import com.alibaba.sdk.android.httpdns.RequestIpType;
import com.alibaba.sdk.android.httpdns.SyncService;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import okhttp3.Dns;

/**
 * 示意代码
 *
 * @author zonglin.nzl
 * @date 10/28/21
 */
public class OkHttpDns implements Dns {
    private HttpDnsService service = MainActivity.httpdns;

    @Override
    public List<InetAddress> lookup(String s) throws UnknownHostException {
        SyncService syncService = (SyncService) service;

        // 请求的ip类型根据 网络环境判断
        HTTPDNSResult result = syncService.getByHost(s, RequestIpType.v4);
        // 如果请求的 v4 使用 result.getIps()，如果请求的v6 使用 result.getIpv6s()
        if (result != null && result.getIps() != null && result.getIps().length > 0) {
            ArrayList<InetAddress> list = new ArrayList<>();
            for (int i = 0; i < result.getIps().length; i++) {
                list.addAll(Arrays.asList(InetAddress.getAllByName(result.getIps()[i])));
            }
            return list;
        }
        return Dns.SYSTEM.lookup(s);
    }
}
