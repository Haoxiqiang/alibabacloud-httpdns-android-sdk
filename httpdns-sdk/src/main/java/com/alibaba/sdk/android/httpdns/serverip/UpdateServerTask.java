package com.alibaba.sdk.android.httpdns.serverip;

import android.text.TextUtils;

import com.alibaba.sdk.android.httpdns.BuildConfig;
import com.alibaba.sdk.android.httpdns.impl.HttpDnsConfig;
import com.alibaba.sdk.android.httpdns.report.ReportManager;
import com.alibaba.sdk.android.httpdns.request.HttpRequest;
import com.alibaba.sdk.android.httpdns.request.HttpRequestConfig;
import com.alibaba.sdk.android.httpdns.request.HttpRequestFailWatcher;
import com.alibaba.sdk.android.httpdns.request.HttpRequestTask;
import com.alibaba.sdk.android.httpdns.request.HttpRequestWatcher;
import com.alibaba.sdk.android.httpdns.request.RequestCallback;
import com.alibaba.sdk.android.httpdns.request.ResponseTranslator;
import com.alibaba.sdk.android.httpdns.request.RetryHttpRequest;

import static com.alibaba.sdk.android.httpdns.interpret.InterpretHostHelper.getBssid;
import static com.alibaba.sdk.android.httpdns.interpret.InterpretHostHelper.getNetType;
import static com.alibaba.sdk.android.httpdns.interpret.InterpretHostHelper.getSid;

/**
 * @author zonglin.nzl
 * @date 2020/10/19
 */
public class UpdateServerTask {

    public static void updateServer(HttpDnsConfig config, String region, RequestCallback<UpdateServerResponse> callback) {
        String path = "/" + config.getAccountId() + "/ss?"
                + "platform=android&sdk_version=" + BuildConfig.VERSION_NAME
                + (TextUtils.isEmpty(region) ? "" : ("&region=" + region)
                + getSid()
                + getNetType()
                + getBssid());
        HttpRequestConfig requestConfig = new HttpRequestConfig(config.getSchema(), config.getServerIp(), config.getPort(), path, config.getTimeout());
        HttpRequest<UpdateServerResponse> httpRequest = new HttpRequest<>(requestConfig, new ResponseTranslator<UpdateServerResponse>() {
            @Override
            public UpdateServerResponse translate(String response) throws Throwable {
                return UpdateServerResponse.fromResponse(response);
            }
        });
        httpRequest = new HttpRequestWatcher<>(httpRequest, new HttpRequestFailWatcher(ReportManager.getReportManagerByAccount(config.getAccountId())));
        // 增加切换ip，回到初始Ip的逻辑
        httpRequest = new HttpRequestWatcher<>(httpRequest, new ShiftServerWatcher(config));
        // 重试，当前服务Ip和初始服务ip个数
        httpRequest = new RetryHttpRequest<>(httpRequest, config.getServerIps().length + config.getInitServerSize() - 1);
        config.getWorker().execute(new HttpRequestTask<>(httpRequest, callback));
    }
}
