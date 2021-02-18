package com.alibaba.sdk.android.httpdns.test.helper;

import com.alibaba.sdk.android.httpdns.RequestIpType;
import com.alibaba.sdk.android.httpdns.interpret.InterpretHostResponse;
import com.alibaba.sdk.android.httpdns.interpret.ResolveHostResponse;
import com.alibaba.sdk.android.httpdns.serverip.UpdateServerResponse;
import com.alibaba.sdk.android.httpdns.test.server.SecretService;
import com.alibaba.sdk.android.httpdns.test.utils.RandomValue;
import com.alibaba.sdk.android.httpdns.test.utils.TestLogger;
import com.alibaba.sdk.android.httpdns.utils.CommonUtil;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import okhttp3.mockwebserver.RecordedRequest;

/**
 * 服务判断逻辑辅助类
 *
 * @author zonglin.nzl
 * @date 2020/10/16
 */
public class ServerHelper {

    public static boolean checkSign(SecretService secretService, RecordedRequest recordedRequest) {
        List<String> pathSegments = recordedRequest.getRequestUrl().pathSegments();
        if (pathSegments.size() == 2 && (pathSegments.contains("sign_resolve") || pathSegments.contains("sign_d"))) {
            String account = pathSegments.get(0);
            String host = recordedRequest.getRequestUrl().queryParameter("host");
            String timestamp = recordedRequest.getRequestUrl().queryParameter("t");
            String secret = secretService.get(account);
            String sign = recordedRequest.getRequestUrl().queryParameter("s");
            long time = Long.parseLong(timestamp);
            long currentTime = System.currentTimeMillis() / 1000;
            boolean timeValid = time > currentTime && time <= currentTime + 600;
            try {
                boolean signValid = sign.equals(CommonUtil.getMD5String(host + "-" + secret + "-" + timestamp));
                if (!timeValid || !signValid) {
                    TestLogger.log("check sign fail time : " + timeValid + " sign : " + signValid);
                }
                return timeValid && signValid;
            } catch (NoSuchAlgorithmException e) {
                TestLogger.log("check sign fail");
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    public static String getArgForInterpretHostRequest(RecordedRequest recordedRequest) {
        List<String> pathSegments = recordedRequest.getRequestUrl().pathSegments();
        if (pathSegments.size() == 2 && (pathSegments.contains("d") || pathSegments.contains("sign_d"))) {
            String host = recordedRequest.getRequestUrl().queryParameter("host");
            RequestIpType type = getQueryType(recordedRequest);
            HashMap<String, String> extras = getExtras(recordedRequest);
            return formInterpretHostArg(host, type, extras);
        }
        return null;
    }

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

    private static HashMap<String, String> getExtras(RecordedRequest recordedRequest) {
        Set<String> queryKeys = recordedRequest.getRequestUrl().queryParameterNames();
        ArrayList<String> sdnsKeys = new ArrayList<>();
        for (String qKey : queryKeys) {
            if (qKey.startsWith("sdns-")) {
                sdnsKeys.add(qKey);
            }
        }
        HashMap<String, String> extras = new HashMap<>();
        for (String sKey : sdnsKeys) {
            String value = recordedRequest.getRequestUrl().queryParameter(sKey);
            extras.put(sKey.replace("sdns-", ""), value);
        }
        return extras;
    }

    public static String formInterpretHostArg(String host, RequestIpType type, HashMap<String, String> extras) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(host);
        switch (type) {
            case v6:
                stringBuilder.append("&v6");
                break;
            case both:
                stringBuilder.append("&v4v6");
                break;
        }
        if (extras != null && extras.size() > 0) {
            ArrayList<String> keys = new ArrayList<>(extras.keySet());
            Collections.sort(keys);
            for (String key : keys) {
                stringBuilder.append("&" + key + "=" + extras.get(key));
            }
        }
        return stringBuilder.toString();
    }

    public static String getInterpretHost(String arg) {
        return arg.split("&")[0];
    }

    public static String formInterpretHostArg(String host, RequestIpType type) {
        return formInterpretHostArg(host, type, null);
    }

    public static StringBuilder constructInterpretHostResultBody(String targetHost, String[] resultIps, String[] resultIpv6s, int ttl, String extra) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("{\"host\":\"");
        stringBuilder.append(targetHost);
        stringBuilder.append("\",\"ips\":");
        if (resultIps.length == 0) {
            stringBuilder.append("[]");
        } else {
            for (int i = 0; i < resultIps.length; i++) {
                if (i == 0) {
                    stringBuilder.append("[\"");
                }
                stringBuilder.append(resultIps[i]);
                if (i == resultIps.length - 1) {
                    stringBuilder.append("\"]");
                } else {
                    stringBuilder.append("\",\"");
                }
            }
        }
        if (resultIpv6s != null && resultIpv6s.length != 0) {
            stringBuilder.append(",\"ipsv6\":");
            for (int i = 0; i < resultIpv6s.length; i++) {
                if (i == 0) {
                    stringBuilder.append("[\"");
                }
                stringBuilder.append(resultIpv6s[i]);
                if (i == resultIpv6s.length - 1) {
                    stringBuilder.append("\"]");
                } else {
                    stringBuilder.append("\",\"");
                }
            }
        }
        stringBuilder.append(",\"ttl\":");
        stringBuilder.append(ttl);
        if (extra != null) {
            stringBuilder.append(",\"extra\":\"" + extra.replace("\"", "\\\"") + "\"");
        }
        stringBuilder.append(",\"origin_ttl\":60,\"client_ip\":\"106.11.41.215\"}");
        return stringBuilder;
    }

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

    public static boolean isDebugRequest(RecordedRequest request) {
        List<String> pathSegments = request.getRequestUrl().pathSegments();
        return pathSegments.size() == 1 && pathSegments.contains("debug");
    }

    public static InterpretHostResponse randomInterpretHostResponse(String host) {
        return new InterpretHostResponse(host, RandomValue.randomIpv4s(), RandomValue.randomIpv6s(), RandomValue.randomInt(60), RandomValue.randomJsonMap());
    }

    public static String toResponseBodyStr(InterpretHostResponse response) {
        return constructInterpretHostResultBody(response.getHostName(), response.getIps(), response.getIpsv6(), (int) response.getTtl(), response.getExtras()).toString();
    }

    public static String createUpdateServerResponse(String[] serverIps, String[] serverIpv6s, int[] ports, int[] v6ports) {
        return constructUpdateServerResultBody(true, serverIps, serverIpv6s, ports, v6ports).toString();
    }

    public static String toResponseBodyStr(UpdateServerResponse updateServerResponse) {
        return constructUpdateServerResultBody(updateServerResponse.isEnable(), updateServerResponse.getServerIps(), updateServerResponse.getServerIpv6s(), updateServerResponse.getServerPorts(), updateServerResponse.getServerIpv6Ports()).toString();
    }

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

    public static InterpretHostResponse randomInterpretHostResponse(String host, int ttl) {
        return new InterpretHostResponse(host, RandomValue.randomIpv4s(), RandomValue.randomIpv6s(), ttl, RandomValue.randomJsonMap());
    }

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
