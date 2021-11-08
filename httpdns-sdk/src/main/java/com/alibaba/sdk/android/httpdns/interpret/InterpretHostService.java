package com.alibaba.sdk.android.httpdns.interpret;

import com.alibaba.sdk.android.httpdns.HTTPDNSResult;
import com.alibaba.sdk.android.httpdns.RequestIpType;
import com.alibaba.sdk.android.httpdns.impl.HostInterpretLocker;
import com.alibaba.sdk.android.httpdns.impl.HostInterpretRecorder;
import com.alibaba.sdk.android.httpdns.log.HttpDnsLog;
import com.alibaba.sdk.android.httpdns.probe.ProbeCallback;
import com.alibaba.sdk.android.httpdns.probe.ProbeService;
import com.alibaba.sdk.android.httpdns.request.RequestCallback;
import com.alibaba.sdk.android.httpdns.utils.CommonUtil;
import com.alibaba.sdk.android.httpdns.utils.Constants;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 域名解析服务
 *
 * @author zonglin.nzl
 * @date 2020/12/3
 */
public class InterpretHostService {

    private InterpretHostResultRepo repo;
    private InterpretHostRequestHandler requestHandler;
    private ProbeService ipProbeService;
    private HostFilter filter;
    private boolean enableExpiredIp = true;
    private HostInterpretRecorder recorder;
    private HostInterpretLocker locker;

    public InterpretHostService(ProbeService ipProbeService, InterpretHostRequestHandler requestHandler, InterpretHostResultRepo repo, HostFilter filter, HostInterpretRecorder recorder) {
        this.ipProbeService = ipProbeService;
        this.requestHandler = requestHandler;
        this.repo = repo;
        this.filter = filter;
        this.recorder = recorder;
        this.locker = new HostInterpretLocker();
    }

    /**
     * 解析域名
     *
     * @param host
     * @return
     */
    public HTTPDNSResult interpretHostAsync(final String host, final RequestIpType type, final Map<String, String> extras, final String cacheKey) {
        if (filter.isFiltered(host)) {
            if (HttpDnsLog.isPrint()) {
                HttpDnsLog.d("request host " + host + ", which is filtered");
            }
            return Constants.EMPTY;
        }
        if (HttpDnsLog.isPrint()) {
            HttpDnsLog.d("request host " + host + " with type " + type + " extras : " + CommonUtil.toString(extras) + " cacheKey " + cacheKey);
        }
        HTTPDNSResult result = repo.getIps(host, type, cacheKey);
        if (HttpDnsLog.isPrint()) {
            HttpDnsLog.d("host " + host + " result is " + CommonUtil.toString(result));
        }
        if ((result == null || result.isExpired()) && recorder.beginInterpret(host, type, cacheKey)) {
            requestHandler.requestInterpretHost(host, type, extras, cacheKey, new RequestCallback<InterpretHostResponse>() {
                @Override
                public void onSuccess(final InterpretHostResponse interpretHostResponse) {
                    if (HttpDnsLog.isPrint()) {
                        HttpDnsLog.i("ip request for " + host + " " + type + " return " + interpretHostResponse.toString());
                    }
                    repo.save(host, type, interpretHostResponse.getExtras(), cacheKey, interpretHostResponse);
                    if (type == RequestIpType.v4 || type == RequestIpType.both) {
                        ipProbeService.probleIpv4(host, interpretHostResponse.getIps(), new ProbeCallback() {
                            @Override
                            public void onResult(String host, String[] sortedIps) {
                                if (HttpDnsLog.isPrint()) {
                                    HttpDnsLog.i("ip probe for " + host + " " + type + " return " + CommonUtil.translateStringArray(sortedIps));
                                }
                                repo.update(host, RequestIpType.v4, cacheKey, sortedIps);
                            }
                        });
                    }
                    recorder.endInterpret(host, type, cacheKey);
                }

                @Override
                public void onFail(Throwable throwable) {
                    HttpDnsLog.w("ip request for " + host + " fail", throwable);
                    recorder.endInterpret(host, type, cacheKey);
                }
            });
        }
        if (result != null && (!result.isExpired() || enableExpiredIp || result.isFromDB())) {
            if (HttpDnsLog.isPrint()) {
                HttpDnsLog.i("request host " + host + " for " + type + " and return " + result.toString() + " immediately");
            }
            return result;
        } else {
            if (HttpDnsLog.isPrint()) {
                HttpDnsLog.i("request host " + host + " and return empty immediately");
            }
            return Constants.EMPTY;
        }
    }

    /**
     * 解析域名
     *
     * @param host
     * @return
     */
    public HTTPDNSResult interpretHost(final String host, final RequestIpType type, final Map<String, String> extras, final String cacheKey) {
        if (filter.isFiltered(host)) {
            HttpDnsLog.d("request host " + host + ", which is filtered");
            return Constants.EMPTY;
        }
        if (HttpDnsLog.isPrint()) {
            HttpDnsLog.d("request host " + host + " sync with type " + type + " extras : " + CommonUtil.toString(extras) + " cacheKey " + cacheKey);
        }
        HTTPDNSResult result = repo.getIps(host, type, cacheKey);
        if (HttpDnsLog.isPrint()) {
            HttpDnsLog.d("host " + host + " result is " + CommonUtil.toString(result));
        }
        if ((result == null || result.isExpired())) {
            // 没有缓存，或者缓存过期，需要解析
            if (locker.beginInterpret(host, type, cacheKey)) {
                // 没有正在进行的解析，发起新的解析
                requestHandler.requestInterpretHost(host, type, extras, cacheKey, new RequestCallback<InterpretHostResponse>() {
                    @Override
                    public void onSuccess(final InterpretHostResponse interpretHostResponse) {
                        if (HttpDnsLog.isPrint()) {
                            HttpDnsLog.i("ip request for " + host + " " + type + " return " + interpretHostResponse.toString());
                        }
                        repo.save(host, type, interpretHostResponse.getExtras(), cacheKey, interpretHostResponse);
                        if (type == RequestIpType.v4 || type == RequestIpType.both) {
                            ipProbeService.probleIpv4(host, interpretHostResponse.getIps(), new ProbeCallback() {
                                @Override
                                public void onResult(String host, String[] sortedIps) {
                                    if (HttpDnsLog.isPrint()) {
                                        HttpDnsLog.i("ip probe for " + host + " " + type + " return " + CommonUtil.translateStringArray(sortedIps));
                                    }
                                    repo.update(host, RequestIpType.v4, cacheKey, sortedIps);
                                }
                            });
                        }
                        locker.endInterpret(host, type, cacheKey);
                    }

                    @Override
                    public void onFail(Throwable throwable) {
                        HttpDnsLog.w("ip request for " + host + " fail", throwable);
                        locker.endInterpret(host, type, cacheKey);
                    }
                });
            }

            if (result == null || !enableExpiredIp) {
                // 有结果，但是过期了，不允许返回过期结果，等请求结束
                if (HttpDnsLog.isPrint()) {
                    HttpDnsLog.d("wait for request finish");
                }
                try {
                    locker.await(host, type, cacheKey, 15, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        } else {
            if (HttpDnsLog.isPrint()) {
                HttpDnsLog.i("request host " + host + " for " + type + " and return " + result.toString() + " immediately");
            }
            return result;
        }

        result = repo.getIps(host, type, cacheKey);
        if (result != null && (!result.isExpired() || enableExpiredIp || result.isFromDB())) {
            if (HttpDnsLog.isPrint()) {
                HttpDnsLog.i("request host " + host + " for " + type + " and return " + result.toString() + " after request");
            }
            return result;
        } else {
            if (HttpDnsLog.isPrint()) {
                HttpDnsLog.i("request host " + host + " and return empty after request");
            }
            return Constants.EMPTY;
        }
    }

    public void setEnableExpiredIp(boolean enableExpiredIp) {
        this.enableExpiredIp = enableExpiredIp;
    }

}
