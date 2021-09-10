package com.alibaba.sdk.android.httpdns.utils;

import android.content.Context;
import android.text.Html;
import android.util.Pair;

import com.alibaba.sdk.android.httpdns.RequestIpType;
import com.alibaba.sdk.android.httpdns.log.HttpDnsLog;

import org.json.JSONObject;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 辅助类
 *
 * @author zonglin.nzl
 * @date 2020/10/20
 */
public class CommonUtil {

    public static boolean regionEquals(String region, String regionThat) {
        if (region == null) {
            region = "";
        }
        if (regionThat == null) {
            regionThat = "";
        }
        return equals(region, regionThat);
    }

    public static boolean equals(Object a, Object b) {
        return (a == b) || (a != null && a.equals(b));
    }

    public static String fixRegion(String region) {
        if (region == null) {
            return "";
        }
        return region;
    }

    public static String[] sortIpsWithSpeeds(String[] ips, int[] speeds) {
        ArrayList<Pair<String, Integer>> ipSpeedPairList = new ArrayList<>();
        for (int i = 0; i < ips.length; i++) {
            ipSpeedPairList.add(new Pair<String, Integer>(ips[i], speeds[i]));
        }
        Collections.sort(ipSpeedPairList, new Comparator<Pair<String, Integer>>() {
            @Override
            public int compare(Pair<String, Integer> o1, Pair<String, Integer> o2) {
                return o1.second - o2.second;
            }
        });
        String[] result = new String[ipSpeedPairList.size()];
        for (int i = 0; i < ipSpeedPairList.size(); i++) {
            result[i] = ipSpeedPairList.get(i).first;
        }
        return result;
    }

    public static final String SEP = ",&#";

    public static String translateStringArray(String[] ips) {
        if (ips == null) {
            return null;
        }
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < ips.length; i++) {
            if (i != 0) {
                stringBuilder.append(SEP);
            }
            stringBuilder.append(ips[i]);
        }
        return stringBuilder.toString();
    }

    public static String[] parseStringArray(String ips) {
        if (ips == null) {
            return null;
        }
        if (ips.equals("")) {
            return new String[0];
        }
        return ips.split(SEP);
    }

    public static String translateIntArray(int[] ports) {
        if (ports == null) {
            return null;
        }
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < ports.length; i++) {
            if (i != 0) {
                stringBuilder.append(SEP);
            }
            stringBuilder.append(ports[i]);
        }
        return stringBuilder.toString();
    }

    public static int[] parseIntArray(String str) {
        if (str == null) {
            return null;
        }
        if (str.equals("")) {
            return new int[0];
        }
        String[] tmp = str.split(SEP);
        int[] ports = new int[tmp.length];
        try {
            for (int i = 0; i < tmp.length; i++) {
                ports[i] = Integer.parseInt(tmp[i]);
            }
        } catch (Throwable throwable) {
            return null;
        }
        return ports;
    }

    public static String getMD5String(final String s) throws NoSuchAlgorithmException {
        final String MD5 = "MD5";
        // Create MD5 Hash
        MessageDigest digest = MessageDigest.getInstance(MD5);
        digest.update(s.getBytes());
        byte messageDigest[] = digest.digest();

        // Create Hex String
        StringBuilder hexString = new StringBuilder();
        for (byte aMessageDigest : messageDigest) {
            String h = Integer.toHexString(0xFF & aMessageDigest);
            while (h.length() < 2)
                h = "0" + h;
            hexString.append(h);
        }
        return hexString.toString();
    }

    public static String toString(Map<String, String> keyValues) {
        if (keyValues == null) {
            return "null";
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[");
        for (Map.Entry<String, String> entry : keyValues.entrySet()) {
            stringBuilder.append(entry.getKey()).append("=").append(entry.getValue()).append(",");
        }
        stringBuilder.append("]");
        return stringBuilder.toString();
    }

    public static String toString(Object result) {
        if (result == null) {
            return "null";
        }
        return result.toString();
    }


    public static String getAccountId(Context context) {
        return getStringStr(context, "ams_accountId");
    }

    public static String getSecretKey(Context context) {
        return getStringStr(context, "ams_httpdns_secretKey");
    }

    private static int getResourceString(Context context, String resourceName) {
        return context.getResources().getIdentifier(resourceName, "string", context.getPackageName());
    }

    public static String getStringStr(Context context, String resourceName) {
        try {
            return context.getResources().getString(getResourceString(context, resourceName));
        } catch (Exception var3) {
            HttpDnsLog.w("AMSConfigUtils " + resourceName + " is NULL");
            return null;
        }
    }


    public static boolean isAHost(String host) {
        try {
            if (host != null) {
                char[] bytes = host.toCharArray();
                if (bytes.length <= 0 || bytes.length > 255) {
                    return false;
                }
                for (char aByte : bytes) {
                    if (!((aByte >= 'A' && aByte <= 'Z') || (aByte >= 'a' && aByte <= 'z')
                            || (aByte >= '0' && aByte <= '9') || aByte == '.' || aByte == '-')) {
                        return false;
                    }
                }
                return true;
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private static final String rex = "^(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)\\."
            + "(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)\\." + "(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)\\."
            + "(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)$";
    private static Pattern pattern = Pattern.compile(rex);


    public static boolean isAnIP(String ip) {
        return !(ip == null || ip.length() < 7 || ip.length() > 15 || ip.equals("")) && pattern.matcher(ip).matches();
    }

    public static String formKey(String host, RequestIpType type, String cacheKey) {
        if (type == RequestIpType.both) {
            throw new IllegalArgumentException("type can not be both");
        }
        return formKeyForAllType(host, type, cacheKey);
    }

    public static String formKeyForAllType(String host, RequestIpType type, String cacheKey) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(host);
        switch (type) {
            case v4:
                stringBuilder.append(":").append("v4");
                break;
            case v6:
                stringBuilder.append(":").append("v6");
                break;
            default:
                stringBuilder.append(":").append("both");
                break;
        }
        if (cacheKey != null) {
            stringBuilder.append(":").append(cacheKey);
        }
        return stringBuilder.toString();
    }

    public static String parseHost(String key) {
        if (key != null && !key.isEmpty()) {
            String[] tmp = key.split(":");
            return tmp[0];
        } else {
            return null;
        }
    }

    // 从旧代码中获取，逻辑待确定
    public static Map<String, String> toMap(String extra) {
        Map<String, String> extras = new HashMap<>();
        if (extra != null) {
            try {
                // 测试验证 服务返回的extra字段进行了url encode处理，所以此处 通过Html转化一下。
                JSONObject jsonObjectExtra = new JSONObject((Html.fromHtml((Html.fromHtml(extra)).toString())).toString());
                Iterator var2 = jsonObjectExtra.keys();
                while (var2.hasNext()) {
                    String var3 = (String) var2.next();
                    extras.put(var3, jsonObjectExtra.get(var3) == null ? null : jsonObjectExtra.get(var3).toString());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return extras;
    }
}
