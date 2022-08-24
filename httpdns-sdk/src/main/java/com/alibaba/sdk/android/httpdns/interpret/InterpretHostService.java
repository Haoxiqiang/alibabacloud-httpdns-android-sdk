package com.alibaba.sdk.android.httpdns.interpret;

import com.alibaba.sdk.android.httpdns.HTTPDNSResult;
import com.alibaba.sdk.android.httpdns.RequestIpType;
import com.alibaba.sdk.android.httpdns.impl.HostInterpretLocker;
import com.alibaba.sdk.android.httpdns.impl.HostInterpretRecorder;
import com.alibaba.sdk.android.httpdns.impl.HttpDnsConfig;
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

    private HttpDnsConfig config;
    private InterpretHostResultRepo repo;
    private InterpretHostRequestHandler requestHandler;
    private ProbeService ipProbeService;
    private HostFilter filter;
    private boolean enableExpiredIp = true;
    private HostInterpretRecorder recorder;
    private HostInterpretLocker locker;

    public InterpretHostService(HttpDnsConfig config, ProbeService ipProbeService, InterpretHostRequestHandler requestHandler, InterpretHostResultRepo repo, HostFilter filter, HostInterpretRecorder recorder) {
        this.config = config;
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
        if ((result == null || result.isExpired())) {
            if (type == RequestIpType.both) {
                // 过滤掉 未过期的请求
                HTTPDNSResult resultV4 = repo.getIps(host, RequestIpType.v4, cacheKey);
                HTTPDNSResult resultV6 = repo.getIps(host, RequestIpType.v6, cacheKey);
                boolean v4Invalid = resultV4 == null || resultV4.isExpired();
                boolean v6Invalid = resultV6 == null || resultV6.isExpired();
                if(v4Invalid && v6Invalid) {
                    // 都过期，不过滤
                    interpretHostInner(host, type, extras, cacheKey);
                } else if(v4Invalid) {
                    // 仅v4过期
                    interpretHostInner(host, RequestIpType.v4, extras, cacheKey);
                } else if(v6Invalid) {
                    // 仅v6过期
                    interpretHostInner(host, RequestIpType.v6, extras, cacheKey);
                }
            } else {
                interpretHostInner(host, type, extras, cacheKey);
            }
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

    private void interpretHostInner(final String host, final RequestIpType type, Map<String, String> extras, final String cacheKey) {
        if (recorder.beginInterpret(host, type, cacheKey)) {
            final String region = config.getRegion();
            requestHandler.requestInterpretHost(host, type, extras, cacheKey, new RequestCallback<InterpretHostResponse>() {
                @Override
                public void onSuccess(final InterpretHostResponse interpretHostResponse) {
                    if (HttpDnsLog.isPrint()) {
                        HttpDnsLog.i("ip request for " + host + " " + type + " return " + interpretHostResponse.toString());
                    }
                    repo.save(region, host, type, interpretHostResponse.getExtras(), cacheKey, interpretHostResponse);
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

            if (type == RequestIpType.both) {
                // 过滤掉 未过期的请求
                HTTPDNSResult resultV4 = repo.getIps(host, RequestIpType.v4, cacheKey);
                HTTPDNSResult resultV6 = repo.getIps(host, RequestIpType.v6, cacheKey);
                boolean v4Invalid = resultV4 == null || resultV4.isExpired();
                boolean v6Invalid = resultV6 == null || resultV6.isExpired();
                if(v4Invalid && v6Invalid) {
                    // 都过期，不过滤
                    syncInterpretHostInner(host, type, extras, cacheKey, result);
                } else if(v4Invalid) {
                    // 仅v4过期
                    syncInterpretHostInner(host, RequestIpType.v4, extras, cacheKey, result);
                } else if(v6Invalid) {
                    // 仅v6过期
                    syncInterpretHostInner(host, RequestIpType.v6, extras, cacheKey, result);
                }
            } else {
                syncInterpretHostInner(host, type, extras, cacheKey, result);
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

    private void syncInterpretHostInner(final String host, final RequestIpType type, Map<String, String> extras, final String cacheKey, HTTPDNSResult result) {
        if (locker.beginInterpret(host, type, cacheKey)) {
            final String region = config.getRegion();
            // 没有正在进行的解析，发起新的解析
            requestHandler.requestInterpretHost(host, type, extras, cacheKey, new RequestCallback<InterpretHostResponse>() {
                @Override
                public void onSuccess(final InterpretHostResponse interpretHostResponse) {
                    if (HttpDnsLog.isPrint()) {
                        HttpDnsLog.i("ip request for " + host + " " + type + " return " + interpretHostResponse.toString());
                    }
                    repo.save(region, host, type, interpretHostResponse.getExtras(), cacheKey, interpretHostResponse);
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
            // 不管什么情况释放锁
            locker.endInterpret(host, type, cacheKey);
        }
    }

    public void setEnableExpiredIp(boolean enableExpiredIp) {
        this.enableExpiredIp = enableExpiredIp;
    }

}
