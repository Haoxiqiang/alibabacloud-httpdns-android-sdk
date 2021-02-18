package com.alibaba.sdk.android.httpdns.test.utils;

/**
 * 测试模块的日志接口
 *
 * @author zonglin.nzl
 * @date 2020/12/18
 */
public class TestLogger {
    public static void log(String msg) {
        System.out.println("[TEST][" + System.currentTimeMillis() % (60 * 1000) + "]" + msg);
    }
}
