package com.alibaba.sdk.android.httpdns.test.server;

import com.alibaba.sdk.android.httpdns.test.utils.RandomValue;

import java.util.HashMap;

/**
 * @author zonglin.nzl
 * @date 2020/12/11
 */
public class SecretService {
    HashMap<String, String> secrets = new HashMap<>();

    public String get(String account) {
        String secret = secrets.get(account);
        if (secret == null) {
            secret = RandomValue.randomStringWithFixedLength(16);
            secrets.put(account, secret);
        }
        return secret;
    }
}
