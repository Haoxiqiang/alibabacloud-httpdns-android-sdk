package com.aliyun.ams.httpdns.demo;

import android.app.Application;
import android.util.Log;

import com.alibaba.sdk.android.httpdns.HttpDnsService;
import com.alibaba.sdk.android.httpdns.ILogger;
import com.alibaba.sdk.android.httpdns.log.HttpDnsLog;

/**
 * @author zonglin.nzl
 * @date 8/30/22
 */
public class MyApp extends Application {

    public static final String TAG = "HTTPDNS DEMO";
    private static MyApp instance;

    public static MyApp getInstance() {
        return instance;
    }

    private HttpDnsHolder holderA = new HttpDnsHolder("请替换为测试用A实例的accountId", "请替换为测试用A实例的secret");
    private HttpDnsHolder holderB = new HttpDnsHolder("请替换为测试用B实例的accountId");

    private HttpDnsHolder current = holderA;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        // 开启logcat 日志 默认关闭, 开发测试过程中可以开启
        HttpDnsLog.enable(true);
        // 注入日志接口，接受httpdns的日志，开发测试过程中可以开启, 基础日志需要先enable才生效，一些错误日志不需要
        HttpDnsLog.setLogger(new ILogger() {
            @Override
            public void log(String msg) {
                Log.d("HttpDnsLogger", msg);
            }
        });

        // 初始化httpdns的配置
        holderA.init(this);
        holderB.init(this);
    }

    public HttpDnsHolder getCurrentHolder() {
        return current;
    }

    public HttpDnsHolder changeHolder() {
        if (current == holderA) {
            current = holderB;
        } else {
            current = holderA;
        }
        return current;
    }

    public HttpDnsService getService() {
        return current.getService();
    }

}
