package com.alibaba.sdk.android.httpdns.test.server;

import com.alibaba.sdk.android.httpdns.serverip.UpdateServerResponse;
import com.alibaba.sdk.android.httpdns.test.helper.ServerHelper;
import com.alibaba.sdk.android.httpdns.test.server.base.BaseDataServer;

import org.json.JSONException;

import okhttp3.mockwebserver.RecordedRequest;

/**
 * 服务IP更新接口服务
 *
 * @author zonglin.nzl
 * @date 2020/11/9
 */
public class ServerIpsServer extends BaseDataServer<String, UpdateServerResponse> {
    @Override
    public String convert(UpdateServerResponse updateServerResponse) {
        return ServerHelper.toResponseBodyStr(updateServerResponse);
    }

    @Override
    public UpdateServerResponse convert(String body) {
        try {
            return UpdateServerResponse.fromResponse(body);
        } catch (JSONException e) {
            throw new IllegalStateException("解析服务IP数据失败", e);
        }
    }

    @Override
    public UpdateServerResponse randomData(String s) {
        return new UpdateServerResponse(null, null, null, null);
    }

    @Override
    public String getRequestArg(RecordedRequest recordedRequest) {
        return ServerHelper.getRegionIfIsUpdateServerRequest(recordedRequest);
    }

    @Override
    public boolean isMyBusinessRequest(RecordedRequest request) {
        return ServerHelper.getRegionIfIsUpdateServerRequest(request) != null;
    }
}
