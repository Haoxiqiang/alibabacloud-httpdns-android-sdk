package com.alibaba.sdk.android.httpdns.net;

import com.alibaba.sdk.android.httpdns.HttpDnsSettings;
import com.alibaba.sdk.android.httpdns.NetType;
import com.aliyun.ams.ipdetector.Inet64Util;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * @author zonglin.nzl
 * @date 8/26/22
 */
public class HttpDnsNetworkDetector implements HttpDnsSettings.NetworkDetector {

    private static class Holder {
        private static final HttpDnsNetworkDetector instance = new HttpDnsNetworkDetector();
    }

    public static HttpDnsNetworkDetector getInstance() {
        return Holder.instance;
    }

    private HttpDnsNetworkDetector() {
    }


    @Override
    public NetType getNetType() {
        try {
            Class.forName("com.aliyun.ams.ipdetector.Inet64Util");
            int type = Inet64Util.getStackType();
            if (type == Inet64Util.IP_DUAL_STACK) {
                return NetType.both;
            } else if (type == Inet64Util.IPV4_ONLY) {
                return NetType.v4;
            } else if (type == Inet64Util.IPV6_ONLY) {
                return NetType.v6;
            } else {
                // 没有网络？
                return NetType.none;
            }
        } catch (Throwable e) {
            // 没有引入网络判断库时，使用local dns解析简单判断下
            int type = getIpStackByHost("www.taobao.com");
            if (type == IP_DUAL_STACK) {
                return NetType.both;
            } else if (type == IPV4_ONLY) {
                return NetType.v4;
            } else if (type == IPV6_ONLY) {
                return NetType.v6;
            } else {
                // 没有网络？
                return NetType.none;
            }
        }
    }


    private final static int IP_STACK_UNKNOWN = 0;
    private final static int IPV4_ONLY = 1;
    private final static int IPV6_ONLY = 2;
    private final static int IP_DUAL_STACK = 3;

    private static int getIpStackByHost(String host) {
        try {
            InetAddress[] addrs = InetAddress.getAllByName(host);
            int stack = IP_STACK_UNKNOWN;
            for (int i = 0; i < addrs.length; i++) {
                if (addrs[i] instanceof Inet4Address) {
                    stack |= IPV4_ONLY;
                } else if (addrs[i] instanceof Inet6Address) {
                    stack |= IPV6_ONLY;
                }
            }
            return stack;
        } catch (UnknownHostException e) {
        }
        return IP_STACK_UNKNOWN;
    }
}
