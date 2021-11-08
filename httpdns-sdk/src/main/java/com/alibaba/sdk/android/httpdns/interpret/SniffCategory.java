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
import com.alibaba.sdk.android.httpdns.serverip.ScheduleService;
import com.alibaba.sdk.android.httpdns.utils.Constants;

/**
 * 嗅探模式
 *
 * @author zonglin.nzl
 * @date 2020/12/3
 */
public class SniffCategory implements InterpretHostCategory {

    private ScheduleService scheduleService;
    private StatusControl statusControl;
    private int timeInterval = 30 * 1000;
    private long lastRequestTime = 0L;

    public SniffCategory(ScheduleService scheduleService, StatusControl statusControl) {
        this.scheduleService = scheduleService;
        this.statusControl = statusControl;
    }

    @Override
    public void interpret(HttpDnsConfig config, HttpRequestConfig requestConfig, RequestCallback<InterpretHostResponse> callback) {
        long currentTimeMillis = System.currentTimeMillis();
        // 请求间隔 不小于timeInterval
        if (currentTimeMillis - lastRequestTime < timeInterval) {
            callback.onFail(Constants.sniff_too_often);
            return;
        }
        lastRequestTime = currentTimeMillis;

        HttpRequest<InterpretHostResponse> request = new HttpRequest<InterpretHostResponse>(requestConfig, new InterpretHostResponseTranslator());
        request = new HttpRequestWatcher<>(request, new HttpRequestFailWatcher(ReportManager.getReportManagerByAccount(config.getAccountId())));
        // 兼容ipv6only 环境
        request = new HttpRequestWatcher<>(request, new Ipv6onlyWatcher(config));
        // 切换服务IP，更新服务IP
        request = new HttpRequestWatcher<>(request, new ShiftServerWatcher(config, scheduleService, statusControl));
        try {
            config.getWorker().execute(new HttpRequestTask<InterpretHostResponse>(request, callback));
        } catch (Throwable tr) {
            callback.onFail(tr);
        }
    }

    public void setInterval(int timeMs) {
        this.timeInterval = timeMs;
    }

    public void reset() {
        lastRequestTime = 0L;
    }
}
