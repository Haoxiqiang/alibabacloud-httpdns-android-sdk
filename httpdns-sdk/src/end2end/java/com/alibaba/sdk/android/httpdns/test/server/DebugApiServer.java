package com.alibaba.sdk.android.httpdns.test.server;

import com.alibaba.sdk.android.httpdns.test.helper.ServerHelper;
import com.alibaba.sdk.android.httpdns.test.server.base.BaseDataServer;

import okhttp3.mockwebserver.RecordedRequest;

/**
 * 测试用接口
 *
 * @author zonglin.nzl
 * @date 2020/11/9
 */
public class DebugApiServer extends BaseDataServer<Void, String> {
    @Override
    public String convert(String body) {
        return body;
    }

    @Override
    public String randomData(Void aVoid) {
        return "hello";
    }

    @Override
    public Void getRequestArg(RecordedRequest recordedRequest) {
        return null;
    }

    @Override
    public boolean isMyBusinessRequest(RecordedRequest request) {
        return ServerHelper.isDebugRequest(request);
    }
}
