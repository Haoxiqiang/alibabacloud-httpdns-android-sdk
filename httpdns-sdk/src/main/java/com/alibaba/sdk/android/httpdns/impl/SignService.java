package com.alibaba.sdk.android.httpdns.impl;

import com.alibaba.sdk.android.httpdns.utils.CommonUtil;

import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

/**
 * @author zonglin.nzl
 * @date 2020/12/11
 */
public class SignService {
    public static final int EXPIRATION_TIME = 10 * 60;
    private String secret;
    private long offset = 0L;

    public SignService(String secret) {
        this.secret = secret;
    }

    public HashMap<String, String> getSigns(String host) {
        if (secret == null) {
            return null;
        }
        String t = Long.toString(System.currentTimeMillis() / 1000 + EXPIRATION_TIME + offset);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(host);
        stringBuilder.append("-");
        stringBuilder.append(secret);
        stringBuilder.append("-");
        stringBuilder.append(t);
        String s = null;
        try {
            s = CommonUtil.getMD5String(stringBuilder.toString());
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
        HashMap<String, String> params = new HashMap<>();
        params.put("t", t);
        params.put("s", s);
        return params;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public void setCurrentTimestamp(long serverTime) {
        offset = serverTime - System.currentTimeMillis() / 1000;
    }
}
