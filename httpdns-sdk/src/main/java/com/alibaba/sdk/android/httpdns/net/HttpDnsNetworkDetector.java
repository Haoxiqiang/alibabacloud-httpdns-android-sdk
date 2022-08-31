package com.alibaba.sdk.android.httpdns.net;

import android.content.Context;

import com.alibaba.sdk.android.httpdns.HttpDnsSettings;
import com.alibaba.sdk.android.httpdns.NetType;
import com.alibaba.sdk.android.httpdns.log.HttpDnsLog;
import com.alibaba.sdk.android.httpdns.utils.ThreadUtil;

import java.lang.reflect.Method;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;

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

    private ExecutorService worker = ThreadUtil.createSingleThreadService("NetType");
    private boolean checkInterface = true;
    private String hostToCheckNetType = "www.taobao.com";
    private NetType cache = NetType.none;
    private boolean disableCache = false;
    private Context context;

    /**
     * 网络变化时，清除缓存
     */
    public void cleanCache(final boolean connected) {
        if (disableCache) {
            return;
        }
        cache = NetType.none;
        if (connected && this.context != null) {
            worker.execute(new Runnable() {
                @Override
                public void run() {
                    // 异步探测一下
                    if (context != null) {
                        cache = detectNetType(context);
                    }
                }
            });
        }
    }

    /**
     * 是否禁用缓存，默认不禁用
     * 不确定是否存在网络链接不变的情况下，网络情况会发生变化的情况，所以提供了此开关
     *
     * @param disable
     */
    public void disableCache(boolean disable) {
        this.disableCache = disable;
    }

    /**
     * 如果不能检查本地网关ip,可以调用此接口关闭
     *
     * @param checkInterface
     */
    public void setCheckInterface(boolean checkInterface) {
        this.checkInterface = checkInterface;
    }

    /**
     * 有些场景需要通过本地解析来确认网络类型，默认使用 www.taobao.com
     *
     * @param hostToCheckNetType
     */
    public void setHostToCheckNetType(String hostToCheckNetType) {
        this.hostToCheckNetType = hostToCheckNetType;
    }

    @Override
    public NetType getNetType(Context context) {
        if (disableCache) {
            NetType tmp = detectNetType(context);
            if (HttpDnsLog.isPrint()) {
                HttpDnsLog.d("ipdetector type is " + tmp.name());
            }
            return tmp;
        }
        if (cache != NetType.none) {
            return cache;
        }
        cache = detectNetType(context);
        if (HttpDnsLog.isPrint()) {
            HttpDnsLog.d("ipdetector type is " + cache.name());
        }
        return cache;
    }

    private NetType detectNetType(Context context) {
        this.context = context.getApplicationContext();
        try {
            Class clz = Class.forName("com.aliyun.ams.ipdetector.Inet64Util");
            if (checkInterface) {
                Method getStackType = clz.getMethod("getIpStack", Context.class);
                int type = (int) getStackType.invoke(null, context);

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
            } else {
                Method getStackType = clz.getMethod("getIpStackCheckLocal", Context.class);
                int type = (int) getStackType.invoke(null, context);
                if (HttpDnsLog.isPrint()) {
                    HttpDnsLog.d("ipdetector type is " + type);
                }
                if (type == IP_DUAL_STACK) {
                    // 不检查本地IP的情况下，无法过滤ipv6只有本地ip的情况，需要通过其它方式检测下。
                    NetType tmp = getNetTypeByHost();
                    if (tmp == NetType.v4) {
                        return tmp;
                    }
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

        } catch (Throwable e) {
            if (HttpDnsLog.isPrint()) {
                HttpDnsLog.i("ipdetector not exist.");
            }
            // 没有引入网络判断库时，使用local dns解析简单判断下
            return getNetTypeByHost();
        }
    }

    private NetType getNetTypeByHost() {
        int type = getIpStackByHost(hostToCheckNetType);
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
