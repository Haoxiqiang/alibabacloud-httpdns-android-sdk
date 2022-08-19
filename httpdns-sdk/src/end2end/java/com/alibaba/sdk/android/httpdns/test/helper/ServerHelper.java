package com.alibaba.sdk.android.httpdns.test.helper;

import com.alibaba.sdk.android.httpdns.RequestIpType;
import com.alibaba.sdk.android.httpdns.interpret.InterpretHostResponse;
import com.alibaba.sdk.android.httpdns.interpret.ResolveHostResponse;
import com.alibaba.sdk.android.httpdns.serverip.UpdateServerResponse;
import com.alibaba.sdk.android.httpdns.test.server.InterpretHostServer;
import com.alibaba.sdk.android.httpdns.test.utils.RandomValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import okhttp3.mockwebserver.RecordedRequest;

/**
 * 服务判断逻辑辅助类
 *
 * @author zonglin.nzl
 * @date 2020/10/16
 */
public class ServerHelper {

    private static RequestIpType getQueryType(RecordedRequest recordedRequest) {
        String query = recordedRequest.getRequestUrl().queryParameter("query");
        RequestIpType type = RequestIpType.v4;
        if (query != null && query.contains("6") && query.contains("4")) {
            type = RequestIpType.both;
        } else if (query != null && query.contains("6")) {
            type = RequestIpType.v6;
        }
        return type;
    }

    /**
     * 构建 调度接口结果字符串
     * @param enable
     * @param serverIps
     * @param serverIpv6s
     * @param ports
     * @param v6ports
     * @return
     */
    public static StringBuilder constructUpdateServerResultBody(boolean enable, String[] serverIps, String[] serverIpv6s, int[] ports, int[] v6ports) {
        StringBuilder stringBuilder = new StringBuilder();
        // "{\"service_ip\":[\""+serverIp+"\"],\"service_ipv6\":[\"2401:b180:2000:20::10\"]}"
        boolean hasValue = false;
        stringBuilder.append("{");
        if (!enable) {
            if (hasValue) {
                stringBuilder.append(",");
            }
            hasValue = true;
            stringBuilder.append("\"service_status\":\"disable\"");
        }
        if (serverIps != null) {
            if (hasValue) {
                stringBuilder.append(",");
            }
            hasValue = true;
            stringBuilder.append("\"service_ip\":[");
            for (int i = 0; i < serverIps.length; i++) {
                if (i != 0) {
                    stringBuilder.append(",");
                }
                stringBuilder.append("\"").append(serverIps[i]).append("\"");
            }
            stringBuilder.append("]");
        }
        if (serverIpv6s != null) {
            if (hasValue) {
                stringBuilder.append(",");
            }
            hasValue = true;
            stringBuilder.append("\"service_ipv6\":[");
            for (int i = 0; i < serverIpv6s.length; i++) {
                if (i != 0) {
                    stringBuilder.append(",");
                }
                stringBuilder.append("\"").append(serverIpv6s[i]).append("\"");
            }
            stringBuilder.append("]");
        }
        if (ports != null) {
            if (hasValue) {
                stringBuilder.append(",");
            }
            hasValue = true;
            stringBuilder.append("\"service_ip_port\":[");
            for (int i = 0; i < ports.length; i++) {
                if (i != 0) {
                    stringBuilder.append(",");
                }
                stringBuilder.append(ports[i]);
            }
            stringBuilder.append("]");
        }
        if (v6ports != null) {
            if (hasValue) {
                stringBuilder.append(",");
            }
            hasValue = true;
            stringBuilder.append("\"service_ipv6_port\":[");
            for (int i = 0; i < v6ports.length; i++) {
                if (i != 0) {
                    stringBuilder.append(",");
                }
                stringBuilder.append(v6ports[i]);
            }
            stringBuilder.append("]");
        }
        stringBuilder.append("}");
        return stringBuilder;
    }

    /**
     * 服务侧 从调度请求中获取 region 参数
     * @param request
     * @return
     */
    public static String getRegionIfIsUpdateServerRequest(RecordedRequest request) {
        List<String> pathSegments = request.getRequestUrl().pathSegments();
        if (pathSegments.size() == 2 && pathSegments.contains("ss")) {
            String region = request.getRequestUrl().queryParameter("region");
            if (region == null) {
                return "";
            } else {
                return region;
            }
        }
        return null;
    }

    /**
     * 服务侧 判断是否是 测试请求
     * @param request
     * @return
     */
    public static boolean isDebugRequest(RecordedRequest request) {
        List<String> pathSegments = request.getRequestUrl().pathSegments();
        return pathSegments.size() == 1 && pathSegments.contains("debug");
    }

    /**
     * 构建 调度结果 下行数据
     * @param serverIps
     * @param serverIpv6s
     * @param ports
     * @param v6ports
     * @return
     */
    public static String createUpdateServerResponse(String[] serverIps, String[] serverIpv6s, int[] ports, int[] v6ports) {
        return constructUpdateServerResultBody(true, serverIps, serverIpv6s, ports, v6ports).toString();
    }

    /**
     * 将调度结果 改为下行body字符串
     * @param updateServerResponse
     * @return
     */
    public static String toResponseBodyStr(UpdateServerResponse updateServerResponse) {
        return constructUpdateServerResultBody(updateServerResponse.isEnable(), updateServerResponse.getServerIps(), updateServerResponse.getServerIpv6s(), updateServerResponse.getServerPorts(), updateServerResponse.getServerIpv6Ports()).toString();
    }

    /**
     * 根据 自定义请求参数 构建批量解析结果数据，数据随机
     * @param resolveServerArg
     * @return
     */
    public static ResolveHostResponse randomResolveHostResponse(String resolveServerArg) {
        String[] args = resolveServerArg.split("&");
        String hosts = args[0];
        RequestIpType type = RequestIpType.v4;
        if (args.length > 1) {
            String types = args[1];
            if (types.contains("4") && types.contains("6")) {
                type = RequestIpType.both;
            } else if (types.contains("6")) {
                type = RequestIpType.v6;
            }
        }
        return randomResolveHostResponse(Arrays.asList(hosts.split(",")), type);
    }

    /**
     * 根据 域名列表和解析类型 构建批量解析结果数据，数据随机
     * @param hostList
     * @param type
     * @return
     */
    public static ResolveHostResponse randomResolveHostResponse(List<String> hostList, RequestIpType type) {
        ArrayList<ResolveHostResponse.HostItem> hostItems = new ArrayList<>();
        for (String host : hostList) {
            switch (type) {
                case v4:
                    hostItems.add(new ResolveHostResponse.HostItem(host, RandomValue.randomIpv4s(), null, RandomValue.randomInt(300)));
                    break;
                case v6:
                    hostItems.add(new ResolveHostResponse.HostItem(host, null, RandomValue.randomIpv6s(), RandomValue.randomInt(300)));
                    break;
                default:
                    hostItems.add(new ResolveHostResponse.HostItem(host, RandomValue.randomIpv4s(), RandomValue.randomIpv6s(), RandomValue.randomInt(300)));
                    break;
            }
        }
        return new ResolveHostResponse(hostItems);
    }

    /**
     * 根据 域名列表和解析类型 构建批量解析结果数据，数据随机, ttl指定
     * @param hostList
     * @param type
     * @param ttl
     * @return
     */
    public static ResolveHostResponse randomResolveHostResponse(List<String> hostList, RequestIpType type, int ttl) {
        ArrayList<ResolveHostResponse.HostItem> hostItems = new ArrayList<>();
        for (String host : hostList) {
            switch (type) {
                case v4:
                    hostItems.add(new ResolveHostResponse.HostItem(host, RandomValue.randomIpv4s(), null, ttl));
                    break;
                case v6:
                    hostItems.add(new ResolveHostResponse.HostItem(host, null, RandomValue.randomIpv6s(), ttl));
                    break;
                default:
                    hostItems.add(new ResolveHostResponse.HostItem(host, RandomValue.randomIpv4s(), RandomValue.randomIpv6s(), ttl));
                    break;
            }
        }
        return new ResolveHostResponse(hostItems);
    }

    /**
     * 将批量解析结果 转化为下行字符串
     * @param response
     * @return
     */
    public static String toResponseBodyStr(ResolveHostResponse response) {
        ArrayList<ResolveItem> items = new ArrayList<>();
        for (String host : response.getHosts()) {
            ResolveHostResponse.HostItem item = response.getItem(host);
            if (item.getIps() != null) {
                items.add(new ResolveItem(host, 1, item.getTtl(), item.getIps()));
            }
            if (item.getIpv6s() != null) {
                items.add(new ResolveItem(host, 28, item.getTtl(), item.getIpv6s()));
            }
        }
        return constructResolveHostResultBody(items);
    }

    private static String constructResolveHostResultBody(ArrayList<ResolveItem> items) {
        // {"dns":[{"host":"www.taobao.com","ips":["124.239.239.235","124.239.159.105"],"type":1,"ttl":31},{"host":"www.taobao.com","ips":["240e:b1:9801:400:3:0:0:3fa","240e:b1:a820:0:3:0:0:3f6"],"type":28,"ttl":60}]}
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("{\"dns\":[");
        for (int i = 0; i < items.size(); i++) {
            ResolveItem item = items.get(i);
            if (i != 0) {
                stringBuilder.append(",");
            }
            stringBuilder.append("{");
            stringBuilder.append("\"host\":\"").append(item.host).append("\"");
            if (item.ips != null) {
                stringBuilder.append(",\"ips\":[");
                for (int j = 0; j < item.ips.length; j++) {
                    if (j != 0) {
                        stringBuilder.append(",");
                    }
                    stringBuilder.append("\"").append(item.ips[j]).append("\"");
                }
                stringBuilder.append("]");
            }
            stringBuilder.append(",\"type\":").append(item.type);
            stringBuilder.append(",\"ttl\":").append(item.ttl);
            stringBuilder.append("}");
        }
        stringBuilder.append("]}");
        return stringBuilder.toString();
    }

    /**
     * 服务侧 构建 批量解析的 自定义参数字符串
     * @param recordedRequest
     * @return
     */
    public static String getArgForResolveHostRequest(RecordedRequest recordedRequest) {
        List<String> pathSegments = recordedRequest.getRequestUrl().pathSegments();
        if (pathSegments.size() == 2 && (pathSegments.contains("resolve") || pathSegments.contains("sign_resolve"))) {
            String hosts = recordedRequest.getRequestUrl().queryParameter("host");
            List<String> hostList = Arrays.asList(hosts.split(","));
            RequestIpType type = getQueryType(recordedRequest);
            return formResolveHostArg(hostList, type);
        }
        return null;
    }

    /**
     * 创建含义为禁止服务的 调度解析结果
     * @return
     */
    public static String createUpdateServerDisableResponse() {
        return constructUpdateServerResultBody(false, null, null, null, null).toString();
    }

    private static class ResolveItem {
        public String host;
        public int type;
        public int ttl;
        public String[] ips;

        public ResolveItem(String host, int type, int ttl, String[] ips) {
            this.host = host;
            this.type = type;
            this.ttl = ttl;
            this.ips = ips;
        }
    }


    /**
     * 构建 批量解析的 自定义参数字符串
     * @param hostList
     * @param type
     * @return
     */
    public static String formResolveHostArg(List<String> hostList, RequestIpType type) {
        ArrayList<String> hosts = new ArrayList<>();
        hosts.addAll(hostList);
        Collections.sort(hosts);
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < hosts.size(); i++) {
            if (i != 0) {
                stringBuilder.append(",");
            }
            stringBuilder.append(hosts.get(i));
        }
        switch (type) {
            case v6:
                stringBuilder.append("&v6");
                break;
            case both:
                stringBuilder.append("&v4v6");
                break;
        }
        return stringBuilder.toString();
    }
}
