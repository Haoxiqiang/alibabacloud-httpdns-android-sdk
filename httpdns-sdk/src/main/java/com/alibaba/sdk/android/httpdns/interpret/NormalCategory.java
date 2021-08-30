package com.alibaba.sdk.android.httpdns.interpret;

import com.alibaba.sdk.android.httpdns.impl.HttpDnsConfig;
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

/**
 * 域名解析的一般策略
 *
 * @author zonglin.nzl
 * @date 2020/12/3
 */
public class NormalCategory implements InterpretHostCategory {

    private StatusControl statusControl;
    private ScheduleService scheduleService;

    public NormalCategory(ScheduleService scheduleService, StatusControl statusControl) {
        this.scheduleService = scheduleService;
        this.statusControl = statusControl;
    }

    @Override
    public void interpret(HttpDnsConfig config, HttpRequestConfig requestConfig, RequestCallback<InterpretHostResponse> callback) {
        HttpRequest<InterpretHostResponse> request = new HttpRequest<InterpretHostResponse>(requestConfig, new InterpretHostResponseTranslator());
        request = new HttpRequestWatcher<>(request, new HttpRequestFailWatcher(ReportManager.getReportManagerByAccount(config.getAccountId())));
        // 兼容ipv6only 环境
        request = new HttpRequestWatcher<>(request, new Ipv6onlyWatcher(config));
        // 切换服务IP，更新服务IP
        request = new HttpRequestWatcher<>(request, new ShiftServerWatcher(config, scheduleService, statusControl));
        // 重试一次
        request = new RetryHttpRequest<>(request, 1);
        config.getWorker().execute(new HttpRequestTask<InterpretHostResponse>(request, callback));
    }

}
