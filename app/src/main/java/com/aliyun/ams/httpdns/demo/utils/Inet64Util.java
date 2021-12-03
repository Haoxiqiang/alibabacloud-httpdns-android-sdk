package com.aliyun.ams.httpdns.demo.utils;

import android.util.Log;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 网络环境判断
 * Created by yangyupeng on 2018/7/2.
 */
public class Inet64Util {
    final static String TAG = "Inet64Util";

    public final static int IP_STACK_UNKNOWN = 0;
    public final static int IPV4_ONLY = 1;
    public final static int IPV6_ONLY = 2;
    public final static int IP_DUAL_STACK = 3;

    static volatile String networkId = "";
    static ConcurrentHashMap<String, Integer> ipStackMap = new ConcurrentHashMap<String, Integer>();
    private static INetworkHelper helper;
    private static ScheduledExecutorService threadPool;

    private static int stackByHost = IP_STACK_UNKNOWN;

    public static void init(INetworkHelper helper) {
        if (Inet64Util.helper != null) {
            return;
        }
        Inet64Util.helper = helper;
        threadPool = Executors.newScheduledThreadPool(2);

        networkId = helper.generateCurrentNetworkId();
        startIpStackDetect();
        startIpStackDetectByHost();
    }

    /**
     * 是否是ipv6only的网络
     *
     * @return
     */
    public static boolean isIPv6OnlyNetwork() {
        Integer status = ipStackMap.get(networkId);
        return status != null && status == IPV6_ONLY;
    }

    /**
     * 是否是ipv4only的网络
     *
     * @return
     */
    public static boolean isIPv4OnlyNetwork() {
        Integer status = ipStackMap.get(networkId);
        return status != null && status == IPV4_ONLY;
    }

    /**
     * 获取网络类型
     *
     * @return
     */
    public static int getStackType() {
        Integer status = ipStackMap.get(networkId);
        if (status == null) {
            return IP_STACK_UNKNOWN;
        }

        return status;
    }

    /**
     * 通过遍历本地网口的方式进行协议栈判断（存在手工过滤不准确问题）
     *
     * @return
     * @throws SocketException
     */
    private static int getIpStackByInterfaces() throws SocketException {
        TreeMap<String, Integer> map = new TreeMap<String, Integer>();

        Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
        for (NetworkInterface networkInterface : Collections.list(networkInterfaces)) {

            List<InterfaceAddress> addressList = networkInterface.getInterfaceAddresses();
            if (addressList.isEmpty()) {
                continue;
            }
            String displayName = networkInterface.getDisplayName();
            Log.d(TAG, " find NetworkInterface:" + displayName);

            int flag = IP_STACK_UNKNOWN;
            for (InterfaceAddress address : networkInterface.getInterfaceAddresses()) {
                InetAddress addr = address.getAddress();
                if (addr instanceof Inet6Address) {
                    // found IPv6 address
                    // do any other validation of address you may need here
                    Inet6Address addr_v6 = (Inet6Address) addr;
                    if (!filterAddress(addr_v6)) {
                        Log.d(TAG, " Found IPv6 address:" + addr_v6.toString());
                        flag |= IPV6_ONLY;
                    }
                } else if (addr instanceof Inet4Address) {
                    Inet4Address addr_v4 = (Inet4Address) addr;
                    if (!filterAddress(addr_v4)
                            && !addr_v4.getHostAddress().startsWith("192.168.43.")) { //过滤掉分享wifi热点这种case
                        Log.d(TAG, " Found IPv4 address:" + addr_v4.toString());
                        flag |= IPV4_ONLY;
                    }
                }
            }
            if (flag != IP_STACK_UNKNOWN) {
                map.put(displayName.toLowerCase(), flag);
            }
        }

        if (map.isEmpty()) {
            return IP_STACK_UNKNOWN;
        } else if (map.size() == 1) {
            return map.firstEntry().getValue();
        } else {
            //如果有多个networkInterface有ip地址，选择当前手机网络对应的那个
            String searchStr = null;
            if (helper.isWifi()) {
                searchStr = "wlan";
            } else if (helper.isMobile()) {
                searchStr = "rmnet";
            }

            int flag = IP_STACK_UNKNOWN;
            if (searchStr != null) {
                for (Map.Entry<String, Integer> entry : map.entrySet()) {
                    if (entry.getKey().startsWith(searchStr)) {
                        flag |= entry.getValue();
                    }
                }
            }

            if (flag == IPV6_ONLY && map.containsKey("v4-wlan0")) {
                flag |= map.remove("v4-wlan0"); //如果是IPv6_Only网络且存在系统启动的v4->v6转换虚拟网卡，则认为还是双栈网络
            }

            return flag;
        }
    }

    public static void startIpStackDetectByHost() {
        threadPool.execute(new Runnable() {
            @Override
            public void run() {
                detectIpStackByHost();
            }
        });
    }

    /**
     * 开始协议栈探测
     */
    public static void startIpStackDetect() {
        networkId = helper.generateCurrentNetworkId();

        if (ipStackMap.putIfAbsent(networkId, IP_STACK_UNKNOWN) != null) {
            return;
        }

        int status = detectIpStack();
        ipStackMap.put(networkId, status);

        final int lastDetectIpStack = status;

        final String finalNetworkId = networkId;

        if (status == IPV6_ONLY || status == IP_DUAL_STACK) {
            //如果判断是IPv6_Only或者Dual_Stack，延迟2秒double check一次
            threadPool.schedule(new Runnable() {
                @Override
                public void run() {
                    try {
                        String tmpNetworkId = helper.generateCurrentNetworkId();
                        if (!finalNetworkId.equals(tmpNetworkId)) {
                            return;
                        }
                        Log.d(TAG, " startIpStackDetect double check");
                        int status = detectIpStack();
                        if (lastDetectIpStack != status) {
                            ipStackMap.put(finalNetworkId, status);
                        }
                    } catch (Exception e) {
                    }
                }
            }, 1500, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * 获取协议栈
     *
     * @return
     */
    private static int detectIpStack() {
        int status = IP_STACK_UNKNOWN;
        try {
            status = getIpStackByInterfaces();
        } catch (Throwable e) {
            Log.e(TAG, "[detectIpStack]error.");
        }
        Log.d(TAG, "startIpStackDetect ip stack " + status);
        return status;
    }

    private static boolean filterAddress(InetAddress address) {
        return address.isLoopbackAddress() || address.isLinkLocalAddress() || address.isAnyLocalAddress();
    }

    private static void detectIpStackByHost() {
        // 任何支持双栈的域名都可以
        stackByHost = getIpStackByHost("www.taobao.com");
    }

    public static boolean isIPv6OnlyNetworkByHost() {
        return stackByHost == IPV6_ONLY;
    }

    public static int getIpStackByHost(String host) {
        try {
            InetAddress[] addrs = InetAddress.getAllByName(host);
            int stack = IP_STACK_UNKNOWN;
            for (int i = 0; i < addrs.length; i++) {
                Log.d(TAG, host + " has " + addrs[i].getHostAddress());
                if (addrs[i] instanceof Inet4Address) {
                    stack |= IPV4_ONLY;
                } else if (addrs[i] instanceof Inet6Address) {
                    stack |= IPV6_ONLY;
                }
            }
            Log.d(TAG, "By host stack is " + stack);
            return stack;
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "By host stack is unknown");
        return IP_STACK_UNKNOWN;
    }
}