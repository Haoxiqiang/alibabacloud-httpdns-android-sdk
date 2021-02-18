package com.alibaba.sdk.android.httpdns.impl;

import android.content.Context;

import com.alibaba.sdk.android.httpdns.HttpDnsService;
import com.alibaba.sdk.android.httpdns.log.HttpDnsLog;

import java.util.HashMap;

/**
 * HttpDnsService 实例持有者
 *
 * @author zonglin.nzl
 * @date 2020/12/4
 */
public class HttpDnsInstanceHolder {

    private HttpDnsCreator creator;
    private HashMap<String, HttpDnsService> instances;
    private ErrorImpl error = new ErrorImpl();


    public HttpDnsInstanceHolder(HttpDnsCreator creator) {
        this.creator = creator;
        instances = new HashMap<>();
    }

    public HttpDnsService get(Context context, String account, String secretKey) {
        if (context == null) {
            HttpDnsLog.e("init httpdns with null context!!");
            return error;
        }
        if (account == null || account.equals("")) {
            HttpDnsLog.e("init httpdns with emtpy account!!");
            return error;
        }
        HttpDnsService service = instances.get(account);
        if (service == null) {

            service = creator.create(context, account, secretKey);
            instances.put(account, service);
        } else {
            if (service instanceof HttpDnsServiceImpl) {
                ((HttpDnsServiceImpl) service).setSecret(secretKey);
            }
        }
        return service;
    }
}
