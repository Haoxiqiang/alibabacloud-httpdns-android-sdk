package com.alibaba.sdk.android.httpdns.test.app;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.alibaba.sdk.android.httpdns.ApiForTest;
import com.alibaba.sdk.android.httpdns.DegradationFilter;
import com.alibaba.sdk.android.httpdns.HTTPDNSResult;
import com.alibaba.sdk.android.httpdns.HttpDns;
import com.alibaba.sdk.android.httpdns.HttpDnsService;
import com.alibaba.sdk.android.httpdns.ILogger;
import com.alibaba.sdk.android.httpdns.RequestIpType;
import com.alibaba.sdk.android.httpdns.SyncService;
import com.alibaba.sdk.android.httpdns.log.HttpDnsLog;
import com.alibaba.sdk.android.httpdns.probe.IPProbeItem;
import com.alibaba.sdk.android.httpdns.test.server.HttpDnsServer;
import com.alibaba.sdk.android.httpdns.test.server.MockSpeedTestServer;
import com.alibaba.sdk.android.httpdns.test.utils.RandomValue;
import com.alibaba.sdk.android.httpdns.test.utils.ShadowNetworkInfo;
import com.alibaba.sdk.android.httpdns.test.utils.TestExecutorService;
import com.alibaba.sdk.android.httpdns.test.utils.TestLogger;

import org.mockito.ArgumentCaptor;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowConnectivityManager;
import org.robolectric.shadows.ShadowLog;
import org.robolectric.shadows.ShadowLooper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * 模拟业务app
 *
 * @author zonglin.nzl
 * @date 2020/10/15
 */
public class BusinessApp {
    private String accountId;
    private String secret;
    private String businessHost = RandomValue.randomHost();
    private HttpDnsService httpDnsService;
    private ILogger mockLogger;

    private TestExecutorService testExecutorService;

    public BusinessApp(String accountId) {
        this.accountId = accountId;
    }

    public BusinessApp(String account, String secret) {
        this.accountId = account;
        this.secret = secret;
    }

    /**
     * 应用启动
     */
    public void start(HttpDnsServer[] servers, MockSpeedTestServer speedTestServer) {

        // 获取httpdns
        if (secret == null) {
            httpDnsService = HttpDns.getService(RuntimeEnvironment.application, accountId);
        } else {
            httpDnsService = HttpDns.getService(RuntimeEnvironment.application, accountId, secret);
        }
        assertThat("HttpDns.getService should not return null", httpDnsService, notNullValue());

        // 设置针对测试的辅助接口
        if (httpDnsService instanceof ApiForTest) {
            String[] ips = new String[servers.length];
            int[] ports = new int[servers.length];
            for (int i = 0; i < servers.length; i++) {
                ips[i] = servers[i].getServerIp();
                ports[i] = servers[i].getPort();
            }
            // 设置初始IP
            ((ApiForTest) httpDnsService).setInitServer(ips, ports);
            testExecutorService = new TestExecutorService(((ApiForTest) httpDnsService).getWorker());
            ((ApiForTest) httpDnsService).setThread(testExecutorService);
            ((ApiForTest) httpDnsService).setSocketFactory(speedTestServer);
        }

        for (int i = 0; i < servers.length; i++) {
            TestLogger.log(this.toString() + " start with " + servers[i].toString());
        }
    }

    /**
     * 应用退出
     */
    public void stop() {
        testExecutorService.shutdownNow();
        httpDnsService = null;
    }

    /**
     * 获取业务使用host
     *
     * @return
     */
    public String getRequestHost() {
        return businessHost;
    }

    /**
     * 等待app的线程执行完毕
     */
    public void waitForAppThread() {
        try {
            testExecutorService.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 解析域名
     *
     * @return
     */
    public String[] requestInterpretHost() {
        return httpDnsService.getIpsByHostAsync(businessHost);
    }

    /**
     * 解析ipv6的域名
     *
     * @return
     */
    public String[] requestInterpretHostForIpv6() {
        return httpDnsService.getIPv6sByHostAsync(businessHost);
    }

    /**
     * 解析域名
     *
     * @param host
     * @return
     */
    public String[] requestInterpretHost(String host) {
        return httpDnsService.getIpsByHostAsync(host);
    }

    /**
     * 解析ipv6的域名
     *
     * @param requestHost
     * @return
     */
    public String[] requestInterpretHostForIpv6(String requestHost) {
        return httpDnsService.getIPv6sByHostAsync(requestHost);
    }

    /**
     * 设置日志接口
     */
    public void setLogger() {
        mockLogger = mock(ILogger.class);
        HttpDnsLog.setLogger(mockLogger);
        httpDnsService.setLogger(mockLogger);
    }


    public void removeLogger() {
        HttpDnsLog.removeLogger(mockLogger);
    }


    /**
     * check 收到或者没有收到日志
     *
     * @param received
     */
    public void hasReceiveLogInLogcat(boolean received) {
        List<ShadowLog.LogItem> list = ShadowLog.getLogs();
        if (received) {
            assertThat(list.size(), greaterThan(1));
        } else {
            assertThat(list.size(), is(0));
        }
        ShadowLog.clear();
    }

    /**
     * check logger收到日志
     */
    public void hasReceiveLogInLogger() {
        ArgumentCaptor<String> logArgument = ArgumentCaptor.forClass(String.class);
        verify(mockLogger, atLeastOnce()).log(logArgument.capture());
        assertThat(logArgument.getAllValues().size(), greaterThan(1));
    }

    /**
     * 切换region
     *
     * @param region
     */
    public void changeRegionTo(String region) {
        httpDnsService.setRegion(region);
    }

    /**
     * 设置ip probe
     */
    public void enableIpProbe() {
        ArrayList<IPProbeItem> list = new ArrayList<>();
        list.add(new IPProbeItem(businessHost, 6666));
        httpDnsService.setIPProbeList(list);
    }

    /**
     * 设置请求超时
     *
     * @param ms
     */
    public void setTimeout(int ms) {
        httpDnsService.setTimeoutInterval(ms);
    }

    /**
     * 设置更新服务IP的最小间隔
     *
     * @param timeInterval
     */
    public void setUpdateServerTimeInterval(int timeInterval) {
        if (httpDnsService instanceof ApiForTest) {
            ((ApiForTest) httpDnsService).setUpdateServerTimeInterval(timeInterval);
        }
    }

    /**
     * 设置嗅探模式的最小间隔
     *
     * @param timeInterval
     */
    public void setSniffTimeInterval(int timeInterval) {
        if (httpDnsService instanceof ApiForTest) {
            ((ApiForTest) httpDnsService).setSniffTimeInterval(timeInterval);
        }
    }

    /**
     * check 相同的account是否返回相同的实例
     */
    public void checkSameInstanceForSameAcount() {
        assertThat("一个accountId对应一个实例", httpDnsService == HttpDns.getService(RuntimeEnvironment.application, accountId));
    }

    public HTTPDNSResult requestSDNSInterpretHost(HashMap<String, String> extras, String cacheKey) {
        return httpDnsService.getIpsByHostAsync(businessHost, extras, cacheKey);
    }

    public void setGlobalParams(HashMap<String, String> globalParams) {
        httpDnsService.setSdnsGlobalParams(globalParams);
    }

    public void cleanGlobalParams() {
        httpDnsService.clearSdnsGlobalParams();
    }

    public HTTPDNSResult requestSDNSInterpretHostForIpv6(HashMap<String, String> extras, String cacheKey) {
        return httpDnsService.getIpsByHostAsync(businessHost, RequestIpType.v6, extras, cacheKey);
    }

    public void preInterpreHost(ArrayList<String> hostList, RequestIpType type) {
        httpDnsService.setPreResolveHosts(hostList, type);
    }

    public void enableExpiredIp() {
        httpDnsService.setExpiredIPEnabled(true);
    }

    public void enableCache(boolean clean) {
        httpDnsService.setCachedIPEnabled(true, clean);
        waitForAppThread();
    }

    public void setFilter() {
        httpDnsService.setDegradationFilter(new DegradationFilter() {
            @Override
            public boolean shouldDegradeHttpDNS(String hostName) {
                return hostName.equals(businessHost);
            }
        });
    }

    public void setTime(long time) {
        httpDnsService.setAuthCurrentTime(time);
    }

    public void enableResolveAfterNetworkChange(boolean enable) {
        httpDnsService.setPreResolveAfterNetworkChanged(enable);
    }

    public void changeToNetwork(int netType) {
        ShadowConnectivityManager shadowConnectivityManager = Shadows.shadowOf((ConnectivityManager) RuntimeEnvironment.application.getSystemService(Context.CONNECTIVITY_SERVICE));
        NetworkInfo networkInfo;
        if (netType == -1) {
            networkInfo = ShadowNetworkInfo.newInstance(NetworkInfo.DetailedState.DISCONNECTED, netType, 0, true, false);
        } else {
            networkInfo = ShadowNetworkInfo.newInstance(NetworkInfo.DetailedState.CONNECTED, netType, 0, true, true);
        }
        shadowConnectivityManager.setActiveNetworkInfo(networkInfo);
        RuntimeEnvironment.application.sendBroadcast(new Intent(ConnectivityManager.CONNECTIVITY_ACTION));
        ShadowLooper.runUiThreadTasks();
        waitForAppThread();
        TestLogger.log("change network to " + netType);
    }

    @SuppressLint("ApplySharedPref")
    public void changeServerIpUpdateTimeTo(long time) {
        SharedPreferences.Editor editor = RuntimeEnvironment.application.getSharedPreferences("httpdns_config_" + accountId, Context.MODE_PRIVATE).edit();
        editor.putLong("servers_last_updated_time", time);
        editor.commit();
    }

    public void enableHttps(boolean enableHttps) {
        httpDnsService.setHTTPSRequestEnabled(enableHttps);
    }

    public String[] requestInterpretHostSync(String host) {
        if (httpDnsService instanceof SyncService) {
            return ((SyncService) httpDnsService).getByHost(host, RequestIpType.v4).getIps();
        }
        return new String[0];
    }

    public void checkThreadCount(boolean check) {
        if (httpDnsService instanceof ApiForTest) {
            if (((ApiForTest) httpDnsService).getWorker() instanceof TestExecutorService) {
                ((TestExecutorService) ((ApiForTest) httpDnsService).getWorker()).enableThreadCountCheck(check);
            }
        }
    }
}
