package com.alibaba.sdk.android.httpdns.test.server;

import com.alibaba.sdk.android.httpdns.RequestIpType;
import com.alibaba.sdk.android.httpdns.interpret.ResolveHostResponse;
import com.alibaba.sdk.android.httpdns.test.helper.ServerHelper;
import com.alibaba.sdk.android.httpdns.test.server.base.BaseDataServer;
import com.alibaba.sdk.android.httpdns.test.server.base.RequestRecord;
import com.alibaba.sdk.android.httpdns.test.utils.TestLogger;

import org.json.JSONException;

import java.nio.charset.Charset;

import okhttp3.mockwebserver.RecordedRequest;

/**
 * @author zonglin.nzl
 * @date 2020/12/9
 */
public class ResolveHostServer extends BaseDataServer<String, ResolveHostResponse> {

    private SecretService secretService;

    public ResolveHostServer(SecretService secretService) {
        this.secretService = secretService;
    }

    @Override
    public String getRequestArg(RecordedRequest recordedRequest) {
        return ServerHelper.getArgForResolveHostRequest(recordedRequest);
    }

    @Override
    public String convert(ResolveHostResponse resolveHostResponse) {
        return ServerHelper.toResponseBodyStr(resolveHostResponse);
    }

    @Override
    public ResolveHostResponse convert(String body) {
        try {
            return ResolveHostResponse.fromResponse(body);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public ResolveHostResponse randomData(String arg) {
        return ServerHelper.randomResolveHostResponse(arg);
    }

    @Override
    public boolean isMyBusinessRequest(RecordedRequest request) {
        return ServerHelper.getArgForResolveHostRequest(request) != null && SecretService.checkSign(secretService, request);
    }

    public ResolveHostResponse getReponseForHost(String host, RequestIpType type) {
        synchronized (records) {
            for (RequestRecord record : records) {
                String hosts = record.getRecordedRequest().getRequestUrl().queryParameter("host");
                String query = record.getRecordedRequest().getRequestUrl().queryParameter("query");
                if (hosts.contains(host) && isRequestType(query, type)) {
                    return convert(record.getMockResponse().getBody().readString(Charset.forName("UTF-8")));
                } else {
                    TestLogger.log("getReponseForHost host " + host + " type " + type + " hosts " + hosts + " query " + query);
                }
            }
        }
        TestLogger.log("getReponseForHost host " + host + " type " + type + " return null!!");
        return null;
    }

    private boolean isRequestType(String query, RequestIpType type) {
        return (type == RequestIpType.v4 && (query == null || query.equals("4")))
                || (type == RequestIpType.v6 && (query.equals("6")))
                || (type == RequestIpType.both && (query.equals("4,6") || query.equals("6,4")));
    }
}
