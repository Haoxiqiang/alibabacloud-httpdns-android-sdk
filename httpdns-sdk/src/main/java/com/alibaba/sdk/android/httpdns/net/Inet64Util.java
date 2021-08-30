package com.alibaba.sdk.android.httpdns.net;

import com.alibaba.sdk.android.httpdns.log.HttpDnsLog;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.InvalidParameterException;
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
 * ipv4地址转换
 * Created by yangyupeng on 2018/7/2.
 */
public class Inet64Util {
    final static String TAG = "Inet64Util";
    final static String IPV4ONLY_HOST = "ipv4only.arpa";
    final static byte[][] IPV4ONLY_IP = {new byte[]{(byte) 0xC0, 0x0, 0x0, (byte) 0xAA}//192.0.0.170
            , new byte[]{(byte) 0xC0, 0x0, 0x0, (byte) 0xAB}                            //192.0.0.171
    };

    public final static int IP_STACK_UNKNOWN = 0;
    public final static int IPV4_ONLY = 1;
    public final static int IPV6_ONLY = 2;
    public final static int IP_DUAL_STACK = 3;

    static volatile String networkId = null;
    static Nat64Prefix defaultNatPrefix = null;
    static ConcurrentHashMap<String, Nat64Prefix> nat64PrefixMap = new ConcurrentHashMap<String, Nat64Prefix>();
    static ConcurrentHashMap<String, Integer> ipStackMap = new ConcurrentHashMap<String, Integer>();
    private static INetworkHelper helper;
    private static ScheduledExecutorService threadPool;

    public static void init(INetworkHelper helper) {
        if (Inet64Util.helper != null) {
            return;
        }
        Inet64Util.helper = helper;
        threadPool = Executors.newScheduledThreadPool(2);
        try {
            defaultNatPrefix = new Nat64Prefix((Inet6Address) InetAddress.getAllByName("64:ff9b::")[0], 96);
        } catch (UnknownHostException e) {
        }
        networkId = helper.generateCurrentNetworkId();
        startIpStackDetect();
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
     * 获取当前网络的ipv6前缀
     *
     * @return
     */
    public static Nat64Prefix getNat64Prefix() {
        Nat64Prefix nat64Prefix = nat64PrefixMap.get(networkId);
        if (nat64Prefix == null) {
            nat64Prefix = defaultNatPrefix;
        }
        return nat64Prefix;
    }

    /**
     * v4 转 v6 地址
     *
     * @param inet4Address
     * @return
     * @throws Exception
     */
    public static String convertToIPv6(Inet4Address inet4Address) throws Exception {
        if (inet4Address == null) {
            throw new InvalidParameterException("address in null");
        }

        Nat64Prefix prefix = getNat64Prefix();
        if (prefix == null) {
            throw new Exception("cannot get nat64 prefix");
        }

        //拆分IP字节
        byte[] v4_bytes = inet4Address.getAddress();
        byte[] v6_bytes = prefix.mPrefix.getAddress();

        //按字节位或v4地址,并跳过第8字节.
        int indexStart = prefix.mPrefixLength / 8, i = 0;
        for (int counter = 0; i + indexStart <= 15 && counter < 4; i++) {
            if ((indexStart + i) == 8)
                continue;
            v6_bytes[indexStart + i] |= v4_bytes[counter++];
        }

        return (InetAddress.getByAddress(v6_bytes)).getHostAddress();
    }

    /**
     * v4 转 v6
     *
     * @param iPv4
     * @return
     */
    public static String convertToIPv6(String iPv4) {
        try {
            Inet4Address v4Address = (Inet4Address) Inet4Address.getByName(iPv4);
            return convertToIPv6(v4Address);
        } catch (Exception e) {
        }
        return null;
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
            HttpDnsLog.d(TAG + " find NetworkInterface:" + displayName);

            int flag = IP_STACK_UNKNOWN;
            for (InterfaceAddress address : networkInterface.getInterfaceAddresses()) {
                InetAddress addr = address.getAddress();
                if (addr instanceof Inet6Address) {
                    // found IPv6 address
                    // do any other validation of address you may need here
                    Inet6Address addr_v6 = (Inet6Address) addr;
                    if (!filterAddress(addr_v6)) {
                        HttpDnsLog.d(TAG + " Found IPv6 address:" + addr_v6.toString());
                        flag |= IPV6_ONLY;
                    }
                } else if (addr instanceof Inet4Address) {
                    Inet4Address addr_v4 = (Inet4Address) addr;
                    if (!filterAddress(addr_v4)
                            && !addr_v4.getHostAddress().startsWith("192.168.43.")) { //过滤掉分享wifi热点这种case
                        HttpDnsLog.d(TAG + " Found IPv4 address:" + addr_v4.toString());
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
                        flag = entry.getValue();
                        break;
                    }
                }
            }

            if (flag == IPV6_ONLY && map.containsKey("v4-wlan0")) {
                flag |= map.remove("v4-wlan0"); //如果是IPv6_Only网络且存在系统启动的v4->v6转换虚拟网卡，则认为还是双栈网络
            }

            return flag;
        }
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
                        HttpDnsLog.d(TAG + " startIpStackDetect double check");
                        int status = detectIpStack();
                        if (lastDetectIpStack != status) {
                            ipStackMap.put(finalNetworkId, status);
                        }

                        if (status == IPV6_ONLY || status == IP_DUAL_STACK) {
                            Nat64Prefix nat64Prefix = detectNat64Prefix();
                            if (nat64Prefix != null) {
                                nat64PrefixMap.put(finalNetworkId, nat64Prefix);
                            }
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
            HttpDnsLog.e(TAG + "[detectIpStack]error.");
        }
        HttpDnsLog.d(TAG + "startIpStackDetect ip stack " + status);
        return status;
    }

    private static boolean filterAddress(InetAddress address) {
        return address.isLoopbackAddress() || address.isLinkLocalAddress() || address.isAnyLocalAddress();
    }


    private static Nat64Prefix detectNat64Prefix() throws UnknownHostException {

        InetAddress inetAddress = null;
        try {
            //根据简化流程的RFC7050, 对ipv4only.arpa进行DNS查询, 如果返回AAAA记录则表明存在DNS64/NAT64, 可以进行Prefix抽取.
            inetAddress = InetAddress.getByName(IPV4ONLY_HOST);
        } catch (Exception e) {
            HttpDnsLog.e(TAG + " detectNat64Prefix " + e.getMessage(), e);
        }

        if (inetAddress instanceof Inet6Address) {
            HttpDnsLog.d(TAG + " Resolved AAAA: " + inetAddress.toString());
            byte[] addrBytes = inetAddress.getAddress();

            if (addrBytes.length != 16) {
                return null;
            }

            //从尾部查找匹配的V4地址.取前端Prefix和长度.
            int index = 12;
            boolean inet4Matched = false;
            while (index >= 0) {
                //First byte matches 0xcc
                if ((addrBytes[index] & IPV4ONLY_IP[0][0]) != 0) {
                    //Middle two bytes matches 0x00
                    if (addrBytes[index + 1] == 0 && addrBytes[index + 2] == 0) {
                        //Last byte matches 0xaa or 0xab
                        if (addrBytes[index + 3] == IPV4ONLY_IP[0][3] || addrBytes[index + 3] == IPV4ONLY_IP[1][3]) {
                            inet4Matched = true;
                            break;
                        }
                    }
                }
                index--;
            }

            if (inet4Matched) {
                //Mask inet4 bits
                addrBytes[index] = addrBytes[index + 1] = addrBytes[index + 2] = addrBytes[index + 3] = 0x0;
                Inet6Address maskedAddr = Inet6Address.getByAddress(IPV4ONLY_HOST, addrBytes, 0);
                Nat64Prefix resolvedPrefix = new Nat64Prefix(maskedAddr, index * 8 /*in bits*/);
                return resolvedPrefix;
            }
        } else if (inetAddress instanceof Inet4Address) {
            HttpDnsLog.d(TAG + "Resolved A: " + inetAddress.toString());
        }
        return null;
    }
}