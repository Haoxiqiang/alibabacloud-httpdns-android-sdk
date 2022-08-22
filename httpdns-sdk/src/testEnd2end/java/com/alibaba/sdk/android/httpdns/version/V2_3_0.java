package com.alibaba.sdk.android.httpdns.version;

import android.Manifest;
import android.net.ConnectivityManager;

import com.alibaba.sdk.android.httpdns.CacheTtlChanger;
import com.alibaba.sdk.android.httpdns.HTTPDNSResult;
import com.alibaba.sdk.android.httpdns.HttpDns;
import com.alibaba.sdk.android.httpdns.ILogger;
import com.alibaba.sdk.android.httpdns.InitConfig;
import com.alibaba.sdk.android.httpdns.RequestIpType;
import com.alibaba.sdk.android.httpdns.interpret.InterpretHostResponse;
import com.alibaba.sdk.android.httpdns.interpret.ResolveHostResponse;
import com.alibaba.sdk.android.httpdns.log.HttpDnsLog;
import com.alibaba.sdk.android.httpdns.probe.IPProbeItem;
import com.alibaba.sdk.android.httpdns.test.app.BusinessApp;
import com.alibaba.sdk.android.httpdns.test.helper.ServerHelper;
import com.alibaba.sdk.android.httpdns.test.helper.ServerStatusHelper;
import com.alibaba.sdk.android.httpdns.test.server.HttpDnsServer;
import com.alibaba.sdk.android.httpdns.test.server.InterpretHostServer;
import com.alibaba.sdk.android.httpdns.test.server.MockSpeedTestServer;
import com.alibaba.sdk.android.httpdns.test.utils.RandomValue;
import com.alibaba.sdk.android.httpdns.test.utils.ShadowNetworkInfo;
import com.alibaba.sdk.android.httpdns.test.utils.UnitTestUtil;
import com.alibaba.sdk.android.httpdns.utils.Constants;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowLog;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 * HTTPDNS 2.3.0 版本需求
 * 1. 缓存使用的ttl改为可配置
 *
 * @author zonglin.nzl
 * @date 2020/10/15
 */
@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner.class)
public class V2_3_0 {

    private BusinessApp app = new BusinessApp(RandomValue.randomStringWithFixedLength(20));

    private HttpDnsServer server = new HttpDnsServer();
    private HttpDnsServer server1 = new HttpDnsServer();
    private HttpDnsServer server2 = new HttpDnsServer();

    private MockSpeedTestServer speedTestServer = new MockSpeedTestServer();
    private ILogger logger;

    @Before
    public void setUp() {
        // 设置日志接口
        HttpDnsLog.enable(true);
        logger = new ILogger() {
            private SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");

            @Override
            public void log(String msg) {
                System.out.println("[" + format.format(new Date()) + "][Httpdns][" + System.currentTimeMillis() % (60 * 1000) + "]" + msg);
            }
        };
        HttpDnsLog.setLogger(logger);
        // 重置实例
        HttpDns.resetInstance();
        // 重置配置
        InitConfig.removeConfig(null);
        // 这里我们启动3个 服务节点用于测试
        server.start();
        server1.start();
        server2.start();
        ShadowApplication application = Shadows.shadowOf(RuntimeEnvironment.application);
        application.grantPermissions(Manifest.permission.ACCESS_NETWORK_STATE);
        app.start(new HttpDnsServer[]{server, server1, server2}, speedTestServer, true);
    }

    @After
    public void tearDown() {
        HttpDnsLog.removeLogger(logger);
        app.waitForAppThread();
        app.stop();
        server.stop();
        server1.stop();
        server2.stop();
        speedTestServer.stop();
    }


    /**
     * 测试 自定义ttl 能力
     *
     * @throws InterruptedException
     */
    @Test
    public void testTtlChanger() throws InterruptedException {

        String hostWithShorterTtl = RandomValue.randomHost();
        String hostWithChangerTtl = RandomValue.randomHost();

        CacheTtlChanger changer = Mockito.mock(CacheTtlChanger.class);
        Mockito.when(changer.changeCacheTtl(hostWithShorterTtl, RequestIpType.v4, 2)).thenReturn(1);
        Mockito.when(changer.changeCacheTtl(hostWithChangerTtl, RequestIpType.v4, 1)).thenReturn(2);

        // 重置，然后重新初始化httpdns
        HttpDns.resetInstance();
        new InitConfig.Builder().configCacheTtlChanger(changer).setEnableExpiredIp(false).buildFor(app.getAccountId());
        app.start(new HttpDnsServer[]{server, server1, server2}, speedTestServer, true);

        InterpretHostResponse response = InterpretHostServer.randomInterpretHostResponse(hostWithShorterTtl, 2);
        server.getInterpretHostServer().preSetRequestResponse(hostWithShorterTtl, response, -1);
        // 请求域名解析，并返回空结果，因为是接口是异步的，所以第一次请求一个域名返回是空
        String[] ips = app.requestInterpretHost(hostWithShorterTtl);
        UnitTestUtil.assertIpsEmpty("第一次请求，没有缓存，应该返回空", ips);
        // 验证服务器收到了请求
        ServerStatusHelper.hasReceiveAppInterpretHostRequestWithResult("当没有缓存时，会异步请求服务器", app, InterpretHostServer.InterpretHostArg.create(hostWithShorterTtl), server, response, 1, true);
        // 再次请求，获取服务器返回的结果
        ips = app.requestInterpretHost(hostWithShorterTtl);
        ServerStatusHelper.hasNotReceiveAppInterpretHostRequest("当有缓存时，不会请求服务器", app, server);
        // 结果和服务器返回一致
        UnitTestUtil.assertIpsEqual("解析域名返回服务器结果", ips, response.getIps());

        Thread.sleep(1000);
        // 由于修改了ttl, 过期了，请求ip
        ips = app.requestInterpretHost(hostWithShorterTtl);
        UnitTestUtil.assertIpsEmpty("ip过期后，返回空", ips);
        ServerStatusHelper.hasReceiveAppInterpretHostRequestWithResult("ttl过期后，再次请求会触发网络请求", app, InterpretHostServer.InterpretHostArg.create(hostWithShorterTtl), server, response, 1, true);
        // 再次请求，获取再次请求服务器返回的结果
        ips = app.requestInterpretHost(hostWithShorterTtl);
        // 结果和服务器返回一致
        UnitTestUtil.assertIpsEqual("解析域名返回服务器结果", ips, response.getIps());


        InterpretHostResponse response1 = InterpretHostServer.randomInterpretHostResponse(hostWithChangerTtl, 1);
        server.getInterpretHostServer().preSetRequestResponse(hostWithChangerTtl, response1, -1);
        // 请求域名解析，并返回空结果，因为是接口是异步的，所以第一次请求一个域名返回是空
        String[] ips1 = app.requestInterpretHost(hostWithChangerTtl);
        UnitTestUtil.assertIpsEmpty("第一次请求，没有缓存，应该返回空", ips1);
        // 验证服务器收到了请求
        ServerStatusHelper.hasReceiveAppInterpretHostRequestWithResult("当没有缓存时，会异步请求服务器", app, InterpretHostServer.InterpretHostArg.create(hostWithChangerTtl), server, response1, 1, true);
        // 再次请求，获取服务器返回的结果
        ips1 = app.requestInterpretHost(hostWithChangerTtl);
        ServerStatusHelper.hasNotReceiveAppInterpretHostRequest("当有缓存时，不会请求服务器", app, server);
        // 结果和服务器返回一致
        UnitTestUtil.assertIpsEqual("解析域名返回服务器结果", ips1, response1.getIps());

        Thread.sleep(1000);
        // 由于修改了ttl, 没有过期，请求ip 返回缓存结果
        ips1 = app.requestInterpretHost(hostWithChangerTtl);
        // 服务没有收到请求
        ServerStatusHelper.hasNotReceiveAppInterpretHostRequest("当有缓存时，不会请求服务器", app, server);
        UnitTestUtil.assertIpsEqual("解析域名返回缓存结果", ips1, response1.getIps());
    }

//
//    /**
//     * 解析结果为空会使用本地存储
//     */
//    @Test
//    public void testEmptyIpWillUseDiskCache() {
//
//        // 模拟 请求 解析结果为空
//        server.getInterpretHostServer().preSetRequestResponse(app.getRequestHost(), ServerHelper.randomInterpretHostResponse(app.getRequestHost()), -1);
//
//        // 重新初始化实例
//
//        // 再次请求，使用的是缓存
//
//    }
//
//    /**
//     * 当解析结果为空时，说明此域名的解析是无效的，我们期望根据ttl来存储，保证ttl时间内，不在发起此请求，即使应用杀死重启
//     * 服务会基于此特性，下发较大的ttl，来降低无效请求
//     */
//    @Test
//    public void testEmptyIpWillLastBetweenAppLifeCycle() {
//
//    }
//
//    /**
//     * 当网络变化时，空IP不会被清除
//     */
//    @Test
//    public void testEmptyIpWillNotBeCleanWhenNetworkChange() {
//
//    }
//
//    /**
//     * 预解析过滤掉已经有缓存的域名，仅解析没有结果或者结果过期的域名和ip
//     */
//    @Test
//    public void testResolveRequestWillFilterCache() {
//
//    }
}
