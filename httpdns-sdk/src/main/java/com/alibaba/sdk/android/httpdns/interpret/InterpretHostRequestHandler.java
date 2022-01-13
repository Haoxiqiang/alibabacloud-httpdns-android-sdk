package com.alibaba.sdk.android.httpdns.interpret;

import com.alibaba.sdk.android.httpdns.RequestIpType;
import com.alibaba.sdk.android.httpdns.impl.HttpDnsConfig;
import com.alibaba.sdk.android.httpdns.impl.SignService;
import com.alibaba.sdk.android.httpdns.log.HttpDnsLog;
import com.alibaba.sdk.android.httpdns.report.ReportManager;
import com.alibaba.sdk.android.httpdns.request.HttpRequest;
import com.alibaba.sdk.android.httpdns.request.HttpRequestConfig;
import com.alibaba.sdk.android.httpdns.request.HttpRequestFailWatcher;
import com.alibaba.sdk.android.httpdns.request.HttpRequestTask;
import com.alibaba.sdk.android.httpdns.request.HttpRequestWatcher;
import com.alibaba.sdk.android.httpdns.request.Ipv6onlyWatcher;
import com.alibaba.sdk.android.httpdns.request.RequestCallback;
import com.alibaba.sdk.android.httpdns.request.RetryHttpRequest;
import com.alibaba.sdk.android.httpdns.serverip.ScheduleService;
import com.alibaba.sdk.android.httpdns.utils.Constants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * 发起域名解析请求
 *
 * @author zonglin.nzl
 * @date 2020/12/11
 */
public class InterpretHostRequestHandler {

    private HttpDnsConfig config;
    private ScheduleService scheduleService;
    private CategoryController categoryController;
    private HashMap<String, String> globalParams;
    private SignService signService;

    public InterpretHostRequestHandler(HttpDnsConfig config, ScheduleService scheduleService, SignService signService) {
        this.config = config;
        this.scheduleService = scheduleService;
        this.categoryController = new CategoryController(scheduleService);
        this.globalParams = new HashMap<>();
        this.signService = signService;
    }

    public void requestInterpretHost(final String host, final RequestIpType type, Map<String, String> extras, final String cacheKey, RequestCallback<InterpretHostResponse> callback) {
        if (!config.isCurrentRegionMatch()) {
            if (HttpDnsLog.isPrint()) {
                HttpDnsLog.w("requestInterpretHost region miss match [" + config.getRegion() + "] [" + config.getCurrentServer().getRegion() + "]");
            }
            callback.onFail(Constants.region_not_match);
            return;
        }
        HttpRequestConfig requestConfig = InterpretHostHelper.getConfig(config, host, type, extras, cacheKey, globalParams, signService);
        if (HttpDnsLog.isPrint()) {
            HttpDnsLog.d("start async ip request for " + host + " " + type);
        }
        categoryController.getCategory().interpret(config, requestConfig, callback);
    }


    public void requestResolveHost(final ArrayList<String> hostList, final RequestIpType type, RequestCallback<ResolveHostResponse> callback) {
        if (!config.isCurrentRegionMatch()) {
            if (HttpDnsLog.isPrint()) {
                HttpDnsLog.w("requestResolveHost region miss match [" + config.getRegion() + "] [" + config.getCurrentServer().getRegion() + "]");
            }
            callback.onFail(Constants.region_not_match);
            return;
        }
        HttpRequestConfig requestConfig = InterpretHostHelper.getConfig(config, hostList, type, signService);
        if (HttpDnsLog.isPrint()) {
            HttpDnsLog.d("start resolve hosts async for " + hostList.toString() + " " + type);
        }

        HttpRequest<ResolveHostResponse> request = new HttpRequest<ResolveHostResponse>(requestConfig, new ResolveHostResponseTranslator());
        request = new HttpRequestWatcher<>(request, new HttpRequestFailWatcher(ReportManager.getReportManagerByAccount(config.getAccountId())));
        // 兼容ipv6only 环境
        request = new HttpRequestWatcher<>(request, new Ipv6onlyWatcher(config.getIpv6ServerIps()));
        // 切换服务IP，更新服务IP
        request = new HttpRequestWatcher<>(request, new ShiftServerWatcher(config, scheduleService, categoryController));
        // 重试一次
        request = new RetryHttpRequest<>(request, 1);
        try {
            config.getWorker().execute(new HttpRequestTask<ResolveHostResponse>(request, callback));
        } catch (Throwable e) {
            callback.onFail(e);
        }
    }


    /**
     * 重置状态
     */
    public void resetStatus() {
        categoryController.reset();
    }

    /**
     * 设置嗅探模式的请求时间间隔
     *
     * @param timeInterval
     */
    public void setSniffTimeInterval(int timeInterval) {
        categoryController.setSniffTimeInterval(timeInterval);
    }

    /**
     * 设置sdns的全局参数
     *
     * @param params
     */
    public void setSdnsGlobalParams(Map<String, String> params) {
        this.globalParams.clear();
        if (params != null) {
            this.globalParams.putAll(params);
        }
    }

    /**
     * 清除sdns的全局参数
     */
    public void clearSdnsGlobalParams() {
        this.globalParams.clear();
    }

}
