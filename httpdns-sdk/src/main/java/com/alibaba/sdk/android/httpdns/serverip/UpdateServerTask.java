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
import com.alibaba.sdk.android.httpdns.request.Ipv6onlyWatcher;
import com.alibaba.sdk.android.httpdns.request.RequestCallback;
import com.alibaba.sdk.android.httpdns.request.ResponseTranslator;
import com.alibaba.sdk.android.httpdns.request.RetryHttpRequest;
import com.alibaba.sdk.android.httpdns.utils.Constants;

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
                + getSid());

        Server[] servers = getAllServers(config.getCurrentServer().getServerIps(), config.getCurrentServer().getPorts(), config.getInitServer().getServerIps(), config.getInitServer().getPorts());

        HttpRequestConfig requestConfig = new HttpRequestConfig(config.getSchema(), servers[0].getServerIp(), servers[0].getPort(config.getSchema()), path, config.getTimeout());
        HttpRequest<UpdateServerResponse> httpRequest = new HttpRequest<>(requestConfig, new ResponseTranslator<UpdateServerResponse>() {
            @Override
            public UpdateServerResponse translate(String response) throws Throwable {
                return UpdateServerResponse.fromResponse(response);
            }
        });
        httpRequest = new HttpRequestWatcher<>(httpRequest, new HttpRequestFailWatcher(ReportManager.getReportManagerByAccount(config.getAccountId())));
        // 兼容ipv6only 环境
        httpRequest = new HttpRequestWatcher<>(httpRequest, new Ipv6onlyWatcher(config.getIpv6ServerIps()));
        // 增加切换ip，回到初始Ip的逻辑
        httpRequest = new HttpRequestWatcher<>(httpRequest, new ShiftServerWatcher(servers));
        // 重试，当前服务Ip和初始服务ip个数
        httpRequest = new RetryHttpRequest<>(httpRequest, servers.length - 1);

        try {
            config.getWorker().execute(new HttpRequestTask<>(httpRequest, callback));
        } catch (Throwable e) {
            callback.onFail(e);
        }
    }

    private static Server[] getAllServers(String[] currentServerIps, int[] ports, String[] initServerIps, int[] initServerPorts) {
        Server[] servers = new Server[currentServerIps.length + initServerIps.length];
        for (int i = 0; i < currentServerIps.length; i++) {
            servers[i] = new Server(currentServerIps[i], ports != null && ports.length > i ? ports[i] : Constants.NO_PORT);
        }
        final int tmpSize = currentServerIps.length;
        for (int i = 0; i < initServerIps.length; i++) {
            servers[tmpSize + i] = new Server(initServerIps[i], initServerPorts != null && initServerPorts.length > i ? initServerPorts[i] : Constants.NO_PORT);
        }
        return servers;
    }
}
