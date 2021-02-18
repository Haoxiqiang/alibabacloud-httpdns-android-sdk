package com.alibaba.sdk.android.httpdns.test.server;

import com.alibaba.sdk.android.httpdns.interpret.InterpretHostResponse;
import com.alibaba.sdk.android.httpdns.test.helper.ServerHelper;
import com.alibaba.sdk.android.httpdns.test.server.base.BaseDataServer;

import org.json.JSONException;

import okhttp3.mockwebserver.RecordedRequest;

/**
 * 域名解析服务
 *
 * @author zonglin.nzl
 * @date 2020/11/9
 */
public class InterpretHostServer extends BaseDataServer<String, InterpretHostResponse> {

    private SecretService secretService;

    public InterpretHostServer(SecretService secretService) {
        this.secretService = secretService;
    }

    @Override
    public String convert(InterpretHostResponse interpretHostResponse) {
        return ServerHelper.toResponseBodyStr(interpretHostResponse);
    }

    @Override
    public InterpretHostResponse convert(String body) {
        try {
            return InterpretHostResponse.fromResponse(body);
        } catch (JSONException e) {
            throw new IllegalStateException("解析域名ip数据失败", e);
        }
    }

    @Override
    public InterpretHostResponse randomData(String targetHost) {
        targetHost = ServerHelper.getInterpretHost(targetHost);
        return ServerHelper.randomInterpretHostResponse(targetHost);
    }

    @Override
    public String getRequestArg(RecordedRequest recordedRequest) {
        return ServerHelper.getArgForInterpretHostRequest(recordedRequest);
    }

    @Override
    public boolean isMyBusinessRequest(RecordedRequest request) {
        return ServerHelper.getArgForInterpretHostRequest(request) != null && ServerHelper.checkSign(secretService, request);
    }
}
