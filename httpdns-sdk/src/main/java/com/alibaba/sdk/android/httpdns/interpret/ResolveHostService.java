package com.alibaba.sdk.android.httpdns.interpret;

import com.alibaba.sdk.android.httpdns.HTTPDNSResult;
import com.alibaba.sdk.android.httpdns.RequestIpType;
import com.alibaba.sdk.android.httpdns.impl.HostInterpretRecorder;
import com.alibaba.sdk.android.httpdns.impl.HttpDnsConfig;
import com.alibaba.sdk.android.httpdns.log.HttpDnsLog;
import com.alibaba.sdk.android.httpdns.probe.ProbeCallback;
import com.alibaba.sdk.android.httpdns.probe.ProbeService;
import com.alibaba.sdk.android.httpdns.request.RequestCallback;
import com.alibaba.sdk.android.httpdns.utils.CommonUtil;

import java.util.ArrayList;

/**
 * 批量解析域名
 *
 * @author zonglin.nzl
 * @date 2020/12/11
 */
public class ResolveHostService {
    private HttpDnsConfig config;
    private InterpretHostResultRepo repo;
    private InterpretHostRequestHandler requestHandler;
    private ProbeService ipProbeService;
    private HostFilter filter;
    private HostInterpretRecorder recorder;

    public ResolveHostService(HttpDnsConfig config, InterpretHostResultRepo repo, InterpretHostRequestHandler requestHandler, ProbeService ipProbeService, HostFilter filter, HostInterpretRecorder recorder) {
        this.config = config;
        this.repo = repo;
        this.requestHandler = requestHandler;
        this.ipProbeService = ipProbeService;
        this.filter = filter;
        this.recorder = recorder;
    }

    /**
     * 批量解析域名
     *
     * @param hostList
     * @param type
     */
    public void resolveHostAsync(final ArrayList<String> hostList, final RequestIpType type) {
        if (HttpDnsLog.isPrint()) {
            HttpDnsLog.d("resolve host " + hostList.toString() + " " + type);
        }
        ArrayList<String> allHosts = new ArrayList<>(hostList);
        int count = hostList.size() / 5 + 1;
        for (int i = 0; i < count; i++) {
            final ArrayList<String> targetHost = new ArrayList<>();
            while (targetHost.size() < 5 && allHosts.size() > 0) {
                String host = allHosts.remove(0);
                HTTPDNSResult result = repo.getIps(host, type, null);
                if (CommonUtil.isAHost(host)
                        && !CommonUtil.isAnIP(host)
                        && !this.filter.isFiltered(host)
                        && (result == null || result.isExpired())
                        && recorder.beginInterpret(host, type)) {
                    targetHost.add(host);
                } else {
                    if (HttpDnsLog.isPrint()) {
                        HttpDnsLog.d("resolve ignore host " + host);
                    }
                }
            }
            if (targetHost.size() <= 0) {
                continue;
            }
            if (HttpDnsLog.isPrint()) {
                HttpDnsLog.i("resolve host " + targetHost.toString() + " " + type);
            }
            final String region = config.getRegion();
            requestHandler.requestResolveHost(targetHost, type, new RequestCallback<ResolveHostResponse>() {
                @Override
                public void onSuccess(final ResolveHostResponse resolveHostResponse) {
                    if (HttpDnsLog.isPrint()) {
                        HttpDnsLog.d("resolve hosts for " + targetHost.toString() + " " + type + " return " + resolveHostResponse.toString());
                    }
                    repo.save(region, type, resolveHostResponse);
                    if (type == RequestIpType.v4 || type == RequestIpType.both) {
                        for(final ResolveHostResponse.HostItem item : resolveHostResponse.getItems()) {
                            if(item.getType() == RequestIpType.v4) {
                                ipProbeService.probleIpv4(item.getHost(), item.getIps(), new ProbeCallback() {
                                    @Override
                                    public void onResult(String host, String[] sortedIps) {
                                        repo.update(item.getHost(), item.getType(), null, sortedIps);
                                    }
                                });
                            }
                        }
                    }
                    for (String host : targetHost) {
                        recorder.endInterpret(host, type);
                    }
                }

                @Override
                public void onFail(Throwable throwable) {
                    HttpDnsLog.w("resolve hosts for " + targetHost.toString() + " fail", throwable);
                    for (String host : targetHost) {
                        recorder.endInterpret(host, type);
                    }
                }
            });
        }
    }
}
