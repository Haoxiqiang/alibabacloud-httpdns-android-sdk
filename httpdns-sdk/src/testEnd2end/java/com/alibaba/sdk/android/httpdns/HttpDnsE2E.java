package com.alibaba.sdk.android.httpdns;

import android.Manifest;
import android.net.ConnectivityManager;

import com.alibaba.sdk.android.httpdns.interpret.InterpretHostResponse;
import com.alibaba.sdk.android.httpdns.interpret.ResolveHostResponse;
import com.alibaba.sdk.android.httpdns.log.HttpDnsLog;
import com.alibaba.sdk.android.httpdns.test.app.BusinessApp;
import com.alibaba.sdk.android.httpdns.test.helper.ServerHelper;
import com.alibaba.sdk.android.httpdns.test.helper.ServerStatusHelper;
import com.alibaba.sdk.android.httpdns.test.server.HttpDnsServer;
import com.alibaba.sdk.android.httpdns.test.server.MockSpeedTestServer;
import com.alibaba.sdk.android.httpdns.test.utils.RandomValue;
import com.alibaba.sdk.android.httpdns.test.utils.ShadowNetworkInfo;
import com.alibaba.sdk.android.httpdns.test.utils.UnitTestUtil;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowLog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author zonglin.nzl
 * @date 2020/10/15
 */
@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner.class)
public class HttpDnsE2E {

    private BusinessApp app = new BusinessApp(RandomValue.randomStringWithFixedLength(20));
    private BusinessApp app1 = new BusinessApp(RandomValue.randomStringWithFixedLength(20));

    private HttpDnsServer server = new HttpDnsServer();
    private HttpDnsServer server1 = new HttpDnsServer();
    private HttpDnsServer server2 = new HttpDnsServer();

    private HttpDnsServer server3 = new HttpDnsServer();
    private HttpDnsServer server4 = new HttpDnsServer();
    private HttpDnsServer server5 = new HttpDnsServer();
    private MockSpeedTestServer speedTestServer = new MockSpeedTestServer();
    private ILogger logger;

    @Before
    public void setUp() {
        HttpDnsLog.enable(true);
        logger = new ILogger() {
            @Override
            public void log(String msg) {
                System.out.println("[Httpdns][" + System.currentTimeMillis() % (60 * 1000) + "]" + msg);
            }
        };
        HttpDnsLog.setLogger(logger);
        HttpDns.resetInstance();
        server.start();
        server1.start();
        server2.start();
        server3.start();
        server4.start();
        server5.start();
        ShadowApplication application = Shadows.shadowOf(RuntimeEnvironment.application);
        application.grantPermissions(Manifest.permission.ACCESS_NETWORK_STATE);
        app.start(new HttpDnsServer[]{server, server1, server2}, speedTestServer);
        app1.start(new HttpDnsServer[]{server, server1, server2}, speedTestServer);
    }

    @After
    public void tearDown() {
        HttpDnsLog.removeLogger(logger);
        app.waitForAppThread();
        app1.waitForAppThread();
        app.stop();
        app1.stop();
        server.stop();
        server1.stop();
        server2.stop();
        server3.stop();
        server4.stop();
        server5.stop();
        speedTestServer.stop();
        HttpDns.resetInstance();
    }

    /**
     * 解析域名获取ipv4结果
     */
    @Test
    public void interpretHostToIpv4s() {
        // 请求域名解析，并返回空结果，因为是接口是异步的，所以第一次请求一个域名返回是空
        String[] ips = app.requestInterpretHost();
        UnitTestUtil.assertIpsEmpty("第一次请求，没有缓存，应该返回空", ips);
        // 验证服务器收到了请求
        ServerStatusHelper.hasReceiveAppInterpretHostRequest("当没有缓存时，会异步请求服务器", app, server, 1);
        String[] serverResponseIps = ServerStatusHelper.getServerResponseIps(app, server);
        // 再次请求，获取服务器返回的结果
        ips = app.requestInterpretHost();
        ServerStatusHelper.hasNotReceiveAppInterpretHostRequest("当有缓存时，不会请求服务器", app, server);
        // 结果和服务器返回一致
        UnitTestUtil.assertIpsEqual("解析域名返回服务器结果", serverResponseIps, ips);
    }

    /**
     * 启动日志，会在logcat输出日志
     */
    @Test
    public void enableLogWillPrintLogInLogcat() {
        ShadowLog.clear();
        HttpDnsLog.enable(true);
        doSomethingTriggerLog();
        app.hasReceiveLogInLogcat(true);
    }

    /**
     * 停止日志，logcat无法获取日志
     */
    @Test
    public void disableLogWillNotPrintLogInLogcat() {
        ShadowLog.clear();
        HttpDnsLog.enable(false);
        doSomethingTriggerLog();
        app.hasReceiveLogInLogcat(false);
    }

    /**
     * 设置logger，会在logger中接收到日志
     */
    @Test
    public void setLoggerWillPrintLogToLogger() {
        app.setLogger();
        doSomethingTriggerLog();
        app.hasReceiveLogInLogger();
        app.removeLogger();
    }

    /**
     * 进行一些操作，触发日志
     */
    private void doSomethingTriggerLog() {
        interpretHostToIpv4s();
    }

    /**
     * 修改region，触发更新服务IP，获取新的服务
     */
    @Test
    public void changeRegionWillUpdateServerIp() {
        final String defaultRegion = "";
        final String hkRegion = "hk";

        // 设置不同region对应的服务信息
        prepareUpdateServerResponse(defaultRegion, hkRegion);

        // 修改region
        app.changeRegionTo(hkRegion);
        ServerStatusHelper.hasReceiveRegionChange("修改region会触发更新服务IP请求", app, server, hkRegion);
        // 请求域名解析
        app.requestInterpretHost();
        // 确认是另一个服务接受到请求
        ServerStatusHelper.hasReceiveAppInterpretHostRequest("切换region后，新的服务收到域名解析请求", app, server3, 1);

        // 再把region切换回来
        app.changeRegionTo(defaultRegion);
        ServerStatusHelper.hasReceiveRegionChange("修改region会触发更新服务IP请求", app, server3, defaultRegion);
        app.requestInterpretHost();
        ServerStatusHelper.hasReceiveAppInterpretHostRequest("切回region后，原来的服务收到域名解析请求", app, server, 1);

    }

    private void prepareUpdateServerResponse(String defaultRegion, String anotherRegion) {
        prepareUpdateServerResponseForGroup1(anotherRegion);
        prepareUpdateServerResponseForGroup2(defaultRegion);
    }

    private void prepareUpdateServerResponseForGroup2(String defaultRegion) {
        String anotherUpdateServerResponse = ServerHelper.createUpdateServerResponse(new String[]{server.getServerIp(), server1.getServerIp(), server2.getServerIp()}, RandomValue.randomIpv6s(), new int[]{server.getPort(), server1.getPort(), server2.getPort()}, RandomValue.randomPorts());
        server3.getServerIpsServer().preSetRequestResponse(defaultRegion, 200, anotherUpdateServerResponse, -1);
        server4.getServerIpsServer().preSetRequestResponse(defaultRegion, 200, anotherUpdateServerResponse, -1);
        server5.getServerIpsServer().preSetRequestResponse(defaultRegion, 200, anotherUpdateServerResponse, -1);
    }

    private void prepareUpdateServerResponseForGroup1(String anotherRegion) {
        String updateServerResponse = ServerHelper.createUpdateServerResponse(new String[]{server3.getServerIp(), server4.getServerIp(), server5.getServerIp()}, RandomValue.randomIpv6s(), new int[]{server3.getPort(), server4.getPort(), server5.getPort()}, RandomValue.randomPorts());
        server.getServerIpsServer().preSetRequestResponse(anotherRegion, 200, updateServerResponse, -1);
        server1.getServerIpsServer().preSetRequestResponse(anotherRegion, 200, updateServerResponse, -1);
        server2.getServerIpsServer().preSetRequestResponse(anotherRegion, 200, updateServerResponse, -1);
    }


    /**
     * 测试ip probe功能，
     * 解析域名之后，对返回的ip进行测试，安装速度快慢排序
     */
    @Test
    public void sortIpArrayWithSpeedAfterInterpretHost() {

        speedTestServer.watch(server);

        // 配置IP优先
        app.enableIpProbe();

        // 请求数据触发IP优选
        app.requestInterpretHost();
        ServerStatusHelper.hasReceiveAppInterpretHostRequest("服务接受到域名解析请求", app, server, 1);

        // 判断返回的结果是优选的结果
        String[] ips = app.requestInterpretHost();
        String[] sortedIps = speedTestServer.getSortedIpsFor(app.getRequestHost());

        UnitTestUtil.assertIpsEqual("设置ip优选后，返回的ip是优选之后的结果", ips, sortedIps);
    }

    /**
     * 正常模式下，域名解析失败，会自动重试一次
     */
    @Test
    public void interpretHostRequestWillRetryOnceWhenFailed() {
        // 预置第一次失败，第二次成功
        server.getInterpretHostServer().preSetRequestResponse(app.getRequestHost(), 400, "whatever", 1);
        InterpretHostResponse response = ServerHelper.randomInterpretHostResponse(app.getRequestHost());
        server.getInterpretHostServer().preSetRequestResponse(app.getRequestHost(), response, 1);

        // 请求
        app.requestInterpretHost();
        // 判断服务器是否收到两次请求
        ServerStatusHelper.hasReceiveAppInterpretHostRequestWithResult(app, server, 400, "whatever");
        ServerStatusHelper.hasReceiveAppInterpretHostRequestWithResult("正常模式域名解析失败会重试一次", app, server, response);

        String[] ips = app.requestInterpretHost();
        UnitTestUtil.assertIpsEqual("重试如果请求成功，可以正常获取到解析结果", ips, response.getIps());
    }

    /**
     * 设置超时
     */
    @Test
    public void configReqeustTimeout() {
        // 预设请求超时
        server.getInterpretHostServer().preSetRequestTimeout(app.getRequestHost(), -1);

        // 设置超时时间
        int timeout = 1000;
        app.setTimeout(timeout);

        // 请求 并计时
        long start = System.currentTimeMillis();
        app.requestInterpretHost();
        // 确实是否接受到请求，并超时
        ServerStatusHelper.hasReceiveAppInterpretHostRequestButTimeout(app, server);
        long costTime = System.currentTimeMillis() - start;

        // 3.05 是个经验数据，可以考虑调整。影响因素主要有重试次数和线程切换
        assertThat("requst timeout " + costTime, costTime < timeout * 3.05);
    }

    /**
     * 解析域名时，如果服务不可用，会切换服务IP
     */
    @Test
    public void interpretHostRequestWillshiftServerIpWhenCurrentServerNotAvailable() {
        // 预设服务0超时
        server.getInterpretHostServer().preSetRequestTimeout(app.getRequestHost(), -1);
        // 预设服务1正常返回
        InterpretHostResponse response = ServerHelper.randomInterpretHostResponse(app.getRequestHost());
        server1.getInterpretHostServer().preSetRequestResponse(app.getRequestHost(), response, 1);

        // 设置超时，并请求
        app.setTimeout(1000);
        app.requestInterpretHost();
        // 确认服务0 接受到请求，超时
        ServerStatusHelper.hasReceiveAppInterpretHostRequestButTimeout(app, server);
        // 确认服务1 正常返回
        ServerStatusHelper.hasReceiveAppInterpretHostRequestWithResult("服务不可用时，会切换服务IP", app, server1, response);

        String[] ips = app.requestInterpretHost();
        UnitTestUtil.assertIpsEqual("切换服务如果请求成功，可以正常获取到解析结果", ips, response.getIps());

        // 确认后续请求都是实用切换后的服务
        ServerStatusHelper.requestInterpretAnotherHost("切换服务IP后，后续请求都使用此服务", app, server1);
    }

    /**
     * 解析域名时，如果服务降级，会切换服务IP
     */
    @Test
    public void interpretHostRequestWillshiftServerIpWhenCurrentServerDegrade() {
        // 预设服务0降级
        ServerStatusHelper.degradeServer(server, app.getRequestHost(), 1);
        // 预设服务1正常返回
        InterpretHostResponse response = ServerHelper.randomInterpretHostResponse(app.getRequestHost());
        server1.getInterpretHostServer().preSetRequestResponse(app.getRequestHost(), response, 1);

        // 设置超时，并请求
        app.setTimeout(1000);
        app.requestInterpretHost();
        // 确认服务0 接受到请求，降级
        ServerStatusHelper.hasReceiveAppInterpretHostRequestWithDegrade(app, server);
        // 确认服务1 正常返回
        ServerStatusHelper.hasReceiveAppInterpretHostRequestWithResult("服务降级时会切换服务IP", app, server1, response);

        String[] ips = app.requestInterpretHost();
        UnitTestUtil.assertIpsEqual("切换服务如果请求成功，可以正常获取到解析结果", ips, response.getIps());

        // 确认后续请求都是实用切换后的服务
        ServerStatusHelper.requestInterpretAnotherHost("切换服务IP后，后续请求都使用此服务", app, server1);
    }

    /**
     * 当现有的服务IP都失败时，更新服务IP
     */
    @Test
    public void updateServerIpWhenAllServerIpFail() {
        // 设置更新服务IP数据
        prepareUpdateServerResponse("", "");
        // 前三个server设置为不可用
        ServerStatusHelper.degradeServer(server, app.getRequestHost(), -1);
        ServerStatusHelper.degradeServer(server1, app.getRequestHost(), -1);
        ServerStatusHelper.degradeServer(server2, app.getRequestHost(), -1);

        // 请求 切换服务IP，每次请求 重试1次，请求两次，服务IP 换一轮，触发更新服务IP
        app.requestInterpretHost(app.getRequestHost());
        app.waitForAppThread();
        app.requestInterpretHost(app.getRequestHost());
        app.waitForAppThread();

        // 检查服务IP是否已经更新
        ServerStatusHelper.requestInterpretAnotherHost("更新服务IP后，使用新服务解析域名", app, server3);
    }

    /**
     * 当切换两个服务IP解析域名都是超时或者降级时，进入嗅探模式
     */
    @Test
    public void whenServerIpsFailTwiceEnterSniffMode() {
        // 前两个server设置为不可用
        ServerStatusHelper.degradeServer(server, app.getRequestHost(), -1);
        ServerStatusHelper.degradeServer(server1, app.getRequestHost(), -1);
        // 设置server2 一次请求失败，用于嗅探模式请求
        ServerStatusHelper.setError(server2, app.getRequestHost(), 400, "whatever", 1);

        // 请求 切换服务IP，重试1次，一共失败两次，触发sniff模式
        app.requestInterpretHost();
        app.waitForAppThread();
        // 嗅探模式下请求一次
        app.requestInterpretHost();
        // 服务接受到一次请求，返回失败
        ServerStatusHelper.hasReceiveAppInterpretHostRequestWithResult(app, server2, 400, "whatever", 1, true);
        // 没有接收到第二次请求，即没有重试
        ServerStatusHelper.hasNotReceiveAppInterpretHostRequest("嗅探模式没有重试", app, server2);
        // 再次请求
        app.requestInterpretHost(app.getRequestHost());
        // 没有接收到第三次请求，即30s内不能重复请求
        ServerStatusHelper.hasNotReceiveAppInterpretHostRequest("嗅探模式30s内不能再次请求", app, server2);
    }

    /**
     * 嗅探模式下，请求成功，会退出嗅探模式
     */
    @Test
    public void whenInterpretHostSuccessExitSniffMode() {
        // 前两个server设置为不可用
        ServerStatusHelper.degradeServer(server, app.getRequestHost(), -1);
        ServerStatusHelper.degradeServer(server1, app.getRequestHost(), -1);
        // 设置server2 用于嗅探模式请求

        // 请求 切换服务IP，重试1次，一共失败两次，触发sniff模式
        app.requestInterpretHost(app.getRequestHost());
        app.waitForAppThread();
        // 嗅探模式下请求一次，恢复正常模式
        ServerStatusHelper.requestInterpretAnotherHost("嗅探模式下，正常请求", app, server2);

        // 设置一次失败
        server2.getInterpretHostServer().preSetRequestResponse(app.getRequestHost(), 400, "whatever", 1);
        app.requestInterpretHost(app.getRequestHost());
        // 一次失败，一次正常 两次
        ServerStatusHelper.hasReceiveAppInterpretHostRequest("恢复到正常模式后，请求失败会重试一次。", app, server2, 2);
    }

    /**
     * 更新服务IP有时间间隔限制
     */
    @Test
    public void setRegionHasTimeInterval() {
        String defaultRegion = "";
        String hkRegion = "hk";

        prepareUpdateServerResponse(defaultRegion, hkRegion);

        // 切换到hk
        app.changeRegionTo(hkRegion);
        ServerStatusHelper.hasReceiveRegionChange("修改region会触发更新服务IP请求", app, server, hkRegion, true);
        // 切回default
        app.changeRegionTo(defaultRegion);
        ServerStatusHelper.hasReceiveRegionChange("修改region会触发更新服务IP请求", app, server3, defaultRegion, true);

        // 再切换到hk，因为请求时间间隔太小，不会触发请求
        app.changeRegionTo(hkRegion);
        ServerStatusHelper.hasNotReceiveRegionChange("更新服务IP请求没有进行时间间隔限制", app, server, hkRegion);

        // 再切换回default，因为请求时间间隔太小，不会触发请求
        app.changeRegionTo(defaultRegion);
        ServerStatusHelper.hasNotReceiveRegionChange("更新服务IP请求没有进行时间间隔限制", app, server, defaultRegion);

        // 缩短请求间隔
        app.setUpdateServerTimeInterval(1000);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // 切换到hk
        app.changeRegionTo(hkRegion);
        ServerStatusHelper.hasReceiveRegionChange("更新服务IP请求超过时间间隔才允许请求", app, server, hkRegion);
        // 切回default
        app.changeRegionTo(defaultRegion);
        ServerStatusHelper.hasReceiveRegionChange("更新服务IP请求超过时间间隔才允许请求", app, server3, defaultRegion);
    }

    /**
     * 更新服务IP有时间间隔
     *
     * @throws InterruptedException
     */
    @Test
    public void updateServerIpsHasTimeInterval() throws InterruptedException {
        // 设置更新服务IP数据
        prepareUpdateServerResponse("", "");
        //设置所有的服务为不可用
        ServerStatusHelper.degradeServer(server, app.getRequestHost(), -1);
        ServerStatusHelper.degradeServer(server1, app.getRequestHost(), -1);
        ServerStatusHelper.degradeServer(server2, app.getRequestHost(), -1);
        ServerStatusHelper.degradeServer(server3, app.getRequestHost(), -1);
        ServerStatusHelper.degradeServer(server4, app.getRequestHost(), -1);
        ServerStatusHelper.degradeServer(server5, app.getRequestHost(), -1);

        // 请求 切换服务IP，每次请求 重试1次，一共请求两次，进入嗅探模式
        app.requestInterpretHost(app.getRequestHost());
        app.waitForAppThread();
        // 请求一次， 切换服务IP，触发服务IP更新
        app.requestInterpretHost(app.getRequestHost());
        // 检查更新服务IP请求是否触发
        ServerStatusHelper.hasReceiveRegionChange("服务IP切换一遍后，触发服务IP更新", app, server, "", true);
        // 更新之后，退出嗅探模式， 再来一次
        // 请求 切换服务IP，每次请求 重试1次，一共请求两次，进入嗅探模式
        app.requestInterpretHost(app.getRequestHost());
        app.waitForAppThread();
        // 请求一次， 切换服务IP，触发服务IP更新
        app.requestInterpretHost(app.getRequestHost());
        // 因为间隔过小，不会请求服务器
        ServerStatusHelper.hasNotReceiveRegionChange("更新服务IP没有设置时间间隔", app, server, "");
        ServerStatusHelper.hasNotReceiveRegionChange("更新服务IP没有设置时间间隔", app, server3, "");

        // 缩短时间间隔
        app.setUpdateServerTimeInterval(1000);
        // 缩短嗅探的时间间隔
        app.setSniffTimeInterval(500);
        // 嗅探模式下，连续请求三次，触发服务IP更新
        Thread.sleep(500);
        app.requestInterpretHost(app.getRequestHost());
        Thread.sleep(500);
        app.requestInterpretHost(app.getRequestHost());
        Thread.sleep(500);
        app.requestInterpretHost(app.getRequestHost());
        // 确认服务IP请求触发
        ServerStatusHelper.hasReceiveRegionChange("更新服务IP超过时间间隔才能请求,如果在嗅探模式下，也需要缩短嗅探的时间间隔", app, server3, "");
    }

    /**
     * 更新服务IP请求失败，会切换服务尝试
     */
    @Test
    public void updateServerFailWhenRetryAnotherServer() {
        String hkRegion = "hk";
        String updateServerResponse = ServerHelper.createUpdateServerResponse(new String[]{server3.getServerIp(), server4.getServerIp(), server5.getServerIp()}, RandomValue.randomIpv6s(), new int[]{server3.getPort(), server4.getPort(), server5.getPort()}, RandomValue.randomPorts());
        // 第一个服务失败
        server.getServerIpsServer().preSetRequestResponse(hkRegion, 400, "whatever", 1);
        server1.getServerIpsServer().preSetRequestResponse(hkRegion, 200, updateServerResponse, -1);
        server2.getServerIpsServer().preSetRequestResponse(hkRegion, 200, updateServerResponse, -1);

        // 触发服务IP更新，第一次失败，使用第二个服务
        app.changeRegionTo(hkRegion);
        ServerStatusHelper.hasReceiveRegionChange("服务收到更新请求", app, server, hkRegion);
        ServerStatusHelper.hasReceiveRegionChange("更新服务IP时，失败了但是没有遍历尝试其它服务", app, server1, hkRegion);
        ServerStatusHelper.hasNotReceiveRegionChange("更新服务IP时，一旦有一个成功，就不会尝试其它服务了", app, server2, hkRegion);
    }

    /**
     * 当前的服务更新服务IP都失败了，就实用初始服务IP尝试
     */
    @Test
    public void updateServerAllFailWhenRetryInitServer() {
        String hkRegion = "hk";
        String defaultRegion = "";
        prepareUpdateServerResponseForGroup1(hkRegion);

        // 设置服务不可用
        server3.getServerIpsServer().preSetRequestResponse(defaultRegion, 400, "whatever", 1);
        server4.getServerIpsServer().preSetRequestResponse(defaultRegion, 400, "whatever", 1);
        server5.getServerIpsServer().preSetRequestResponse(defaultRegion, 400, "whatever", 1);

        // 先更新一次服务IP
        app.changeRegionTo(hkRegion);
        ServerStatusHelper.hasReceiveRegionChange("服务收到更新服务IP请求", app, server, hkRegion);

        // 再触发一次服务IP更新，此时全部失败，会切换会初始服务请求
        app.changeRegionTo(defaultRegion);
        ServerStatusHelper.hasReceiveRegionChange("更新服务IP都失败时，没有切换回初始化服务IP尝试", app, server, defaultRegion);
    }

    /**
     * 不同的account使用不同的实例，相互之间不影响
     */
    @Test
    public void differentAcountUseDifferentInstance() {
        // 确认一般的域名解析请求都正常
        ServerStatusHelper.requestInterpretAnotherHost("应用0域名解析服务正常", app, server);
        ServerStatusHelper.requestInterpretAnotherHost("应用1域名解析服务正常", app1, server);

        String defaultRegion = "";
        String hkRegion = "hk";
        prepareUpdateServerResponse(defaultRegion, hkRegion);

        // 应用0切换到 hk
        app.changeRegionTo(hkRegion);
        ServerStatusHelper.hasReceiveRegionChange("应用0切换到hk", app, server, hkRegion);
        // 应用1正常使用
        ServerStatusHelper.requestInterpretAnotherHost("应用1不受应用0切region影响", app1, server);

        ServerStatusHelper.degradeServer(server, app1.getRequestHost(), -1);
        ServerStatusHelper.degradeServer(server1, app1.getRequestHost(), -1);
        ServerStatusHelper.setError(server2, app1.getRequestHost(), 400, "whatever", -1);
        // 请求，服务降级，进入嗅探模式
        app1.requestInterpretHost(app1.getRequestHost());
        app1.waitForAppThread();
        // 请求失败, 短时间不能再次嗅探请求
        app1.requestInterpretHost(app1.getRequestHost());
        ServerStatusHelper.hasReceiveAppInterpretHostRequestWithResult(app1, server2, 400, "whatever", 1, true);
        app1.requestInterpretHost(app1.getRequestHost());
        ServerStatusHelper.hasNotReceiveAppInterpretHostRequest("短时间内无法再次嗅探请求", app1, server2);

        ServerStatusHelper.requestInterpretAnotherHost("应用0不受应用1嗅探状态影响", app, server3);

        app1.changeRegionTo(hkRegion);
        app1.waitForAppThread();

        String sameHost = RandomValue.randomHost();
        InterpretHostResponse response = ServerHelper.randomInterpretHostResponse(sameHost);
        InterpretHostResponse response1 = ServerHelper.randomInterpretHostResponse(sameHost);
        server3.getInterpretHostServer().preSetRequestResponse(sameHost, response, 1);
        server3.getInterpretHostServer().preSetRequestResponse(sameHost, response1, 1);

        app.requestInterpretHost(sameHost);
        app.waitForAppThread();
        assertThat("应用0获取自己的结果", server3.getInterpretHostServer().hasRequestForArgWithResult(sameHost, response, 1, true));
        app1.requestInterpretHost(sameHost);
        app1.waitForAppThread();
        assertThat("应用1获取自己的结果", server3.getInterpretHostServer().hasRequestForArgWithResult(sameHost, response1, 1, true));
        assertThat("应用0、应用1结果不干扰", app.requestInterpretHost(sameHost), Matchers.not(Matchers.arrayContaining(app1.requestInterpretHost(sameHost))));
    }

    /**
     * 相同的账号实用相同的实例
     */
    @Test
    public void sameAccountUseSameInstance() {
        app.checkSameInstanceForSameAcount();
    }


    /**
     * ipv6支持测试
     */
    @Test
    public void supportIpv6Interpret() {
        InterpretHostResponse response = ServerHelper.randomInterpretHostResponse(app.getRequestHost());
        server.getInterpretHostServer().preSetRequestResponse(ServerHelper.formInterpretHostArg(app.getRequestHost(), RequestIpType.v6), response, -1);

        // 请求域名解析，并返回空结果，因为是接口是异步的，所以第一次请求一个域名返回是空
        String[] ipv6s = app.requestInterpretHostForIpv6();
        UnitTestUtil.assertIpsEmpty("解析域名，没有缓存时，返回空", ipv6s);
        // 验证服务器收到了请求
        ServerStatusHelper.hasReceiveAppInterpretHostRequestWithResult("服务应该收到ipv6域名解析请求", app, ServerHelper.formInterpretHostArg(app.getRequestHost(), RequestIpType.v6), server, response, 1, true);
        // 再次请求，获取服务器返回的结果
        ipv6s = app.requestInterpretHostForIpv6();
        ServerStatusHelper.hasNotReceiveAppInterpretHostRequest("有缓存，不会请求服务器", app, ServerHelper.formInterpretHostArg(app.getRequestHost(), RequestIpType.v6), server);
        UnitTestUtil.assertIpsEqual("解析结果是服务返回的值", ipv6s, response.getIpsv6());
    }

    @Test
    public void testSDNS() {
        HashMap<String, String> extras = new HashMap<>();
        extras.put("key1", "value1");
        extras.put("key2", "value3");
        String cacheKey = "sdns1";

        InterpretHostResponse normalResponse = ServerHelper.randomInterpretHostResponse(app.getRequestHost());
        server.getInterpretHostServer().preSetRequestResponse(app.getRequestHost(), normalResponse, -1);
        InterpretHostResponse sdnsResponse = ServerHelper.randomInterpretHostResponse(app.getRequestHost());
        server.getInterpretHostServer().preSetRequestResponse(ServerHelper.formInterpretHostArg(app.getRequestHost(), RequestIpType.v4, extras), sdnsResponse, -1);

        HTTPDNSResult result = app.requestSDNSInterpretHost(extras, cacheKey);
        UnitTestUtil.assertIpsEmpty("和其它域名解析一样，sdns解析第一次没有缓存时，返回空", result.getIps());
        ServerStatusHelper.hasReceiveAppInterpretHostRequestWithResult("服务器应该接收到sdns请求", app, ServerHelper.formInterpretHostArg(app.getRequestHost(), RequestIpType.v4, extras), server, sdnsResponse, 1, true);
        result = app.requestSDNSInterpretHost(extras, cacheKey);
        UnitTestUtil.assertIpsEqual("sdns解析结果和预期值一致", result.getIps(), sdnsResponse.getIps());

        String[] ips = app.requestInterpretHost();
        UnitTestUtil.assertIpsEmpty("解析时没有缓存，返回空", ips);
        ServerStatusHelper.hasReceiveAppInterpretHostRequestWithResult("一般解析和sdns解析互不干扰", app, server, normalResponse);
        ips = app.requestInterpretHost();
        UnitTestUtil.assertIpsEqual("一般解析结果和预期一致", ips, normalResponse.getIps());
    }

    @Test
    public void testGlobalParamsSDNS() {

        HashMap<String, String> globalParams = new HashMap<>();
        globalParams.put("g1", "v1");
        globalParams.put("g2", "v2");
        String globalCacheKey = "gsdns1";

        HashMap<String, String> extras = new HashMap<>();
        extras.put("key1", "value1");
        extras.put("key2", "value3");
        String cacheKey = "sdns";
        String cacheKey1 = "sdns1";
        String cacheKey2 = "sdns2";

        // 一般sdns结果
        InterpretHostResponse sdnsResponse = ServerHelper.randomInterpretHostResponse(app.getRequestHost());
        server.getInterpretHostServer().preSetRequestResponse(ServerHelper.formInterpretHostArg(app.getRequestHost(), RequestIpType.v4, extras), sdnsResponse, -1);

        // 仅global参数
        InterpretHostResponse gsdnsResponse = ServerHelper.randomInterpretHostResponse(app.getRequestHost());
        server.getInterpretHostServer().preSetRequestResponse(ServerHelper.formInterpretHostArg(app.getRequestHost(), RequestIpType.v4, globalParams), gsdnsResponse, -1);

        // global参数 + 一般参数
        HashMap<String, String> all = new HashMap();
        all.putAll(globalParams);
        all.putAll(extras);
        InterpretHostResponse sdnsResponse1 = ServerHelper.randomInterpretHostResponse(app.getRequestHost());
        server.getInterpretHostServer().preSetRequestResponse(ServerHelper.formInterpretHostArg(app.getRequestHost(), RequestIpType.v4, all), sdnsResponse1, -1);

        app.requestSDNSInterpretHost(extras, cacheKey);
        app.waitForAppThread();
        HTTPDNSResult result = app.requestSDNSInterpretHost(extras, cacheKey);
        UnitTestUtil.assertIpsEqual("sdns解析结果应该和预期值一致", result.getIps(), sdnsResponse.getIps());

        app.setGlobalParams(globalParams);
        app.requestSDNSInterpretHost(null, globalCacheKey);
        app.waitForAppThread();
        HTTPDNSResult gResult = app.requestSDNSInterpretHost(null, globalCacheKey);
        UnitTestUtil.assertIpsEqual("仅global参数，sdns解析结果和预期值一致", gResult.getIps(), gsdnsResponse.getIps());

        app.requestSDNSInterpretHost(extras, cacheKey1);
        app.waitForAppThread();
        HTTPDNSResult result1 = app.requestSDNSInterpretHost(extras, cacheKey1);
        UnitTestUtil.assertIpsEqual("global参数+定制参数，sdns解析结果和预期值一致", result1.getIps(), sdnsResponse1.getIps());

        app.cleanGlobalParams();
        app.requestSDNSInterpretHost(extras, cacheKey2);
        app.waitForAppThread();
        HTTPDNSResult result2 = app.requestSDNSInterpretHost(extras, cacheKey2);
        UnitTestUtil.assertIpsEqual("清楚global参数后，sdns解析结果和仅定制参数的解析结果一致", result2.getIps(), sdnsResponse.getIps());
    }


    @Test
    public void testSDNSForIpv6() {
        HashMap<String, String> extras = new HashMap<>();
        extras.put("key1", "value1");
        extras.put("key2", "value3");
        String cacheKey = "sdns1";

        InterpretHostResponse sdnsResponse = ServerHelper.randomInterpretHostResponse(app.getRequestHost());
        server.getInterpretHostServer().preSetRequestResponse(ServerHelper.formInterpretHostArg(app.getRequestHost(), RequestIpType.v6, extras), sdnsResponse, -1);

        HTTPDNSResult result = app.requestSDNSInterpretHostForIpv6(extras, cacheKey);
        UnitTestUtil.assertIpsEmpty("和其它域名解析一样，sdns解析第一次没有缓存时，返回空", result.getIps());
        ServerStatusHelper.hasReceiveAppInterpretHostRequestWithResult("服务器应该接收到sdns请求", app, ServerHelper.formInterpretHostArg(app.getRequestHost(), RequestIpType.v6, extras), server, sdnsResponse, 1, true);
        result = app.requestSDNSInterpretHostForIpv6(extras, cacheKey);
        UnitTestUtil.assertIpsEqual("sdns解析结果和预期值一致", result.getIpv6s(), sdnsResponse.getIpsv6());
    }

    @Test
    public void preInterpretHostForIpv4() {
        String host1 = RandomValue.randomHost();
        String host2 = RandomValue.randomHost();
        String host3 = RandomValue.randomHost();
        ArrayList<String> hostList = new ArrayList<>();
        hostList.add(host1);
        hostList.add(host2);
        hostList.add(host3);

        ResolveHostResponse response = ServerHelper.randomResolveHostResponse(hostList, RequestIpType.v4);

        server.getResolveHostServer().preSetRequestResponse(ServerHelper.formResolveHostArg(hostList, RequestIpType.v4), response, 1);

        app.preInterpreHost(hostList, RequestIpType.v4);
        app.waitForAppThread();
        MatcherAssert.assertThat("预解析会触发批量请求", server.getResolveHostServer().hasRequestForArg(ServerHelper.formResolveHostArg(hostList, RequestIpType.v4), 1, false));

        String[] ips1 = app.requestInterpretHost(host1);
        String[] ips2 = app.requestInterpretHost(host2);
        String[] ips3 = app.requestInterpretHost(host3);
        UnitTestUtil.assertIpsEqual("预解析ipv4之后，可以直接获取解析结果", ips1, response.getItem(host1).getIps());
        UnitTestUtil.assertIpsEqual("预解析ipv4之后，可以直接获取解析结果", ips2, response.getItem(host2).getIps());
        UnitTestUtil.assertIpsEqual("预解析ipv4之后，可以直接获取解析结果", ips3, response.getItem(host3).getIps());
    }

    @Test
    public void preInterpretHostForIpv6() {
        String host1 = RandomValue.randomHost();
        String host2 = RandomValue.randomHost();
        String host3 = RandomValue.randomHost();
        ArrayList<String> hostList = new ArrayList<>();
        hostList.add(host1);
        hostList.add(host2);
        hostList.add(host3);

        ResolveHostResponse response = ServerHelper.randomResolveHostResponse(hostList, RequestIpType.v6);

        server.getResolveHostServer().preSetRequestResponse(ServerHelper.formResolveHostArg(hostList, RequestIpType.v6), response, 1);

        app.preInterpreHost(hostList, RequestIpType.v6);
        app.waitForAppThread();
        MatcherAssert.assertThat("预解析会触发批量请求", server.getResolveHostServer().hasRequestForArg(ServerHelper.formResolveHostArg(hostList, RequestIpType.v6), 1, false));


        String[] ips1 = app.requestInterpretHostForIpv6(host1);
        String[] ips2 = app.requestInterpretHostForIpv6(host2);
        String[] ips3 = app.requestInterpretHostForIpv6(host3);
        UnitTestUtil.assertIpsEqual("预解析ipv6之后，可以直接获取解析结果", ips1, response.getItem(host1).getIpv6s());
        UnitTestUtil.assertIpsEqual("预解析ipv6之后，可以直接获取解析结果", ips2, response.getItem(host2).getIpv6s());
        UnitTestUtil.assertIpsEqual("预解析ipv6之后，可以直接获取解析结果", ips3, response.getItem(host3).getIpv6s());
    }


    @Test
    public void preInterpretHost() {
        String host1 = RandomValue.randomHost();
        String host2 = RandomValue.randomHost();
        String host3 = RandomValue.randomHost();
        ArrayList<String> hostList = new ArrayList<>();
        hostList.add(host1);
        hostList.add(host2);
        hostList.add(host3);

        ResolveHostResponse response = ServerHelper.randomResolveHostResponse(hostList, RequestIpType.both);

        server.getResolveHostServer().preSetRequestResponse(ServerHelper.formResolveHostArg(hostList, RequestIpType.both), response, 1);

        app.preInterpreHost(hostList, RequestIpType.both);
        app.waitForAppThread();
        MatcherAssert.assertThat("预解析会触发批量请求", server.getResolveHostServer().hasRequestForArg(ServerHelper.formResolveHostArg(hostList, RequestIpType.both), 1, false));

        String[] ips1 = app.requestInterpretHost(host1);
        String[] ips2 = app.requestInterpretHost(host2);
        String[] ips3 = app.requestInterpretHost(host3);
        UnitTestUtil.assertIpsEqual("预解析之后，可以直接获取解析结果", ips1, response.getItem(host1).getIps());
        UnitTestUtil.assertIpsEqual("预解析之后，可以直接获取解析结果", ips2, response.getItem(host2).getIps());
        UnitTestUtil.assertIpsEqual("预解析之后，可以直接获取解析结果", ips3, response.getItem(host3).getIps());

        String[] ips4 = app.requestInterpretHostForIpv6(host1);
        String[] ips5 = app.requestInterpretHostForIpv6(host2);
        String[] ips6 = app.requestInterpretHostForIpv6(host3);
        UnitTestUtil.assertIpsEqual("预解析之后，可以直接获取解析结果", ips4, response.getItem(host1).getIpv6s());
        UnitTestUtil.assertIpsEqual("预解析之后，可以直接获取解析结果", ips5, response.getItem(host2).getIpv6s());
        UnitTestUtil.assertIpsEqual("预解析之后，可以直接获取解析结果", ips6, response.getItem(host3).getIpv6s());
    }


    @Test
    public void preInterpretHostMoreThan5() {
        String host1 = RandomValue.randomHost();
        String host2 = RandomValue.randomHost();
        String host3 = RandomValue.randomHost();
        String host4 = RandomValue.randomHost();
        String host5 = RandomValue.randomHost();
        String host6 = RandomValue.randomHost();
        String host7 = RandomValue.randomHost();
        ArrayList<String> hostList1 = new ArrayList<>();
        hostList1.add(host1);
        hostList1.add(host2);
        hostList1.add(host3);
        hostList1.add(host4);
        hostList1.add(host5);

        ArrayList<String> hostList2 = new ArrayList<>();
        hostList2.add(host6);
        hostList2.add(host7);

        ResolveHostResponse response1 = ServerHelper.randomResolveHostResponse(hostList1, RequestIpType.both);
        ResolveHostResponse response2 = ServerHelper.randomResolveHostResponse(hostList2, RequestIpType.both);

        server.getResolveHostServer().preSetRequestResponse(ServerHelper.formResolveHostArg(hostList1, RequestIpType.both), response1, 1);
        server.getResolveHostServer().preSetRequestResponse(ServerHelper.formResolveHostArg(hostList2, RequestIpType.both), response2, 1);

        ArrayList<String> hostList = new ArrayList<>();
        hostList.addAll(hostList1);
        hostList.addAll(hostList2);
        app.preInterpreHost(hostList, RequestIpType.both);
        app.waitForAppThread();
        MatcherAssert.assertThat("预解析会触发批量请求,多于5个，会按5个一组拆分", server.getResolveHostServer().hasRequestForArg(ServerHelper.formResolveHostArg(hostList1, RequestIpType.both), 1, false));
        MatcherAssert.assertThat("预解析会触发批量请求,多于5个，会按5个一组拆分", server.getResolveHostServer().hasRequestForArg(ServerHelper.formResolveHostArg(hostList2, RequestIpType.both), 1, false));

        String[] ips1 = app.requestInterpretHost(host1);
        String[] ips2 = app.requestInterpretHost(host2);
        String[] ips3 = app.requestInterpretHost(host3);
        String[] ips4 = app.requestInterpretHost(host4);
        String[] ips5 = app.requestInterpretHost(host5);
        String[] ips6 = app.requestInterpretHost(host6);
        String[] ips7 = app.requestInterpretHost(host7);
        UnitTestUtil.assertIpsEqual("预解析之后，可以直接获取解析结果", ips1, response1.getItem(host1).getIps());
        UnitTestUtil.assertIpsEqual("预解析之后，可以直接获取解析结果", ips2, response1.getItem(host2).getIps());
        UnitTestUtil.assertIpsEqual("预解析之后，可以直接获取解析结果", ips3, response1.getItem(host3).getIps());
        UnitTestUtil.assertIpsEqual("预解析之后，可以直接获取解析结果", ips4, response1.getItem(host4).getIps());
        UnitTestUtil.assertIpsEqual("预解析之后，可以直接获取解析结果", ips5, response1.getItem(host5).getIps());
        UnitTestUtil.assertIpsEqual("超过5个，预解析之后，可以直接获取解析结果", ips6, response2.getItem(host6).getIps());
        UnitTestUtil.assertIpsEqual("超过5个，预解析之后，可以直接获取解析结果", ips7, response2.getItem(host7).getIps());


        String[] ipv6s1 = app.requestInterpretHostForIpv6(host1);
        String[] ipv6s2 = app.requestInterpretHostForIpv6(host2);
        String[] ipv6s3 = app.requestInterpretHostForIpv6(host3);
        String[] ipv6s4 = app.requestInterpretHostForIpv6(host4);
        String[] ipv6s5 = app.requestInterpretHostForIpv6(host5);
        String[] ipv6s6 = app.requestInterpretHostForIpv6(host6);
        String[] ipv6s7 = app.requestInterpretHostForIpv6(host7);
        UnitTestUtil.assertIpsEqual("预解析之后，可以直接获取解析结果", ipv6s1, response1.getItem(host1).getIpv6s());
        UnitTestUtil.assertIpsEqual("预解析之后，可以直接获取解析结果", ipv6s2, response1.getItem(host2).getIpv6s());
        UnitTestUtil.assertIpsEqual("预解析之后，可以直接获取解析结果", ipv6s3, response1.getItem(host3).getIpv6s());
        UnitTestUtil.assertIpsEqual("预解析之后，可以直接获取解析结果", ipv6s4, response1.getItem(host4).getIpv6s());
        UnitTestUtil.assertIpsEqual("预解析之后，可以直接获取解析结果", ipv6s5, response1.getItem(host5).getIpv6s());
        UnitTestUtil.assertIpsEqual("超过5个，预解析之后，可以直接获取解析结果", ipv6s6, response2.getItem(host6).getIpv6s());
        UnitTestUtil.assertIpsEqual("超过5个，预解析之后，可以直接获取解析结果", ipv6s7, response2.getItem(host7).getIpv6s());
    }

    @Test
    public void testIpTtl() throws InterruptedException {
        app.enableExpiredIp(false);
        InterpretHostResponse response = ServerHelper.randomInterpretHostResponse(app.getRequestHost(), 1);
        server.getInterpretHostServer().preSetRequestResponse(app.getRequestHost(), response, -1);
        // 请求域名解析，并返回空结果，因为是接口是异步的，所以第一次请求一个域名返回是空
        String[] ips = app.requestInterpretHost();
        UnitTestUtil.assertIpsEmpty("第一次请求，没有缓存，应该返回空", ips);
        // 验证服务器收到了请求
        ServerStatusHelper.hasReceiveAppInterpretHostRequestWithResult("当没有缓存时，会异步请求服务器", app, app.getRequestHost(), server, response, 1, true);
        // 再次请求，获取服务器返回的结果
        ips = app.requestInterpretHost();
        ServerStatusHelper.hasNotReceiveAppInterpretHostRequest("当有缓存时，不会请求服务器", app, server);
        // 结果和服务器返回一致
        UnitTestUtil.assertIpsEqual("解析域名返回服务器结果", response.getIps(), ips);

        Thread.sleep(1000);
        // ttl 过期后请求ip
        ips = app.requestInterpretHost();
        UnitTestUtil.assertIpsEmpty("ip过期后，返回空", ips);
        ServerStatusHelper.hasReceiveAppInterpretHostRequestWithResult("ttl过期后，再次请求会触发网络请求", app, app.getRequestHost(), server, response, 1, true);
        // 再次请求，获取再次请求服务器返回的结果
        ips = app.requestInterpretHost();
        // 结果和服务器返回一致
        UnitTestUtil.assertIpsEqual("解析域名返回服务器结果", response.getIps(), ips);
    }

    @Test
    public void testEnableExpiredIp() throws InterruptedException {
        app.enableExpiredIp(true);
        InterpretHostResponse response = ServerHelper.randomInterpretHostResponse(app.getRequestHost(), 1);
        InterpretHostResponse response1 = ServerHelper.randomInterpretHostResponse(app.getRequestHost());
        server.getInterpretHostServer().preSetRequestResponse(app.getRequestHost(), response, 1);
        server.getInterpretHostServer().preSetRequestResponse(app.getRequestHost(), response1, -1);
        // 请求域名解析，并返回空结果，因为是接口是异步的，所以第一次请求一个域名返回是空
        String[] ips = app.requestInterpretHost();
        UnitTestUtil.assertIpsEmpty("第一次请求，没有缓存，应该返回空", ips);
        app.waitForAppThread();
        // 再次请求，获取服务器返回的结果
        app.requestInterpretHost();

        Thread.sleep(1000);
        // ttl 过期后请求ip
        ips = app.requestInterpretHost();
        // 结果和服务器返回一致
        UnitTestUtil.assertIpsEqual("启用过期IP，请求时域名过期，仍会返回过期IP", response.getIps(), ips);
        ServerStatusHelper.hasReceiveAppInterpretHostRequestWithResult("ttl过期后，再次请求会触发网络请求", app, app.getRequestHost(), server, response1, 1, true);
        // 再次请求，获取再次请求服务器返回的结果
        ips = app.requestInterpretHost();
        // 结果和服务器返回一致
        UnitTestUtil.assertIpsEqual("解析域名返回服务器结果", response1.getIps(), ips);
    }

    @Test
    public void testServerCache() {
        // 先通过请求失败，切换一次服务IP
        ServerStatusHelper.degradeServer(server, app.getRequestHost(), 1);
        InterpretHostResponse response = ServerHelper.randomInterpretHostResponse(app.getRequestHost());
        server1.getInterpretHostServer().preSetRequestResponse(app.getRequestHost(), response, 1);
        app.requestInterpretHost();
        ServerStatusHelper.hasReceiveAppInterpretHostRequestWithDegrade(app, server);
        ServerStatusHelper.hasReceiveAppInterpretHostRequestWithResult("服务降级时会切换服务IP", app, server1, response);
        String[] ips = app.requestInterpretHost();
        UnitTestUtil.assertIpsEqual("切换服务如果请求成功，可以正常获取到解析结果", ips, response.getIps());

        // 重置实例，确保下次读取的信息是从本地缓存来的
        HttpDns.resetInstance();

        // 重启应用，获取新的实例
        app.start(new HttpDnsServer[]{server, server1, server2}, speedTestServer);

        // 确认后续请求都是使用切换后的服务
        ServerStatusHelper.requestInterpretAnotherHost("读取缓存应该是直接使用切换后的服务", app, server1);
    }

    @Test
    public void testServerCacheWhenServerIsNotInitServer() {
        //  先通过请求失败，切换服务IP
        prepareUpdateServerResponse("", "");
        // 前三个server设置为不可用
        ServerStatusHelper.degradeServer(server, app.getRequestHost(), -1);
        ServerStatusHelper.degradeServer(server1, app.getRequestHost(), -1);
        ServerStatusHelper.degradeServer(server2, app.getRequestHost(), -1);

        // 请求 切换服务IP，每次请求 重试1次，请求两次，服务IP 换一轮，触发更新服务IP
        app.requestInterpretHost(app.getRequestHost());
        app.waitForAppThread();
        app.requestInterpretHost(app.getRequestHost());
        app.waitForAppThread();

        // 重置实例，确保下次读取的信息是从本地缓存来的
        HttpDns.resetInstance();

        // 重启应用，获取新的实例
        app.start(new HttpDnsServer[]{server, server1, server2}, speedTestServer);

        // 检查服务IP是否已经更新
        ServerStatusHelper.requestInterpretAnotherHost("更新服务IP后，使用新服务解析域名", app, server3);
    }

    @Test
    public void testCacheControll() {
        // 先发起一些请求，因为没有开启缓存，所以不会缓存
        String[] ips = app.requestInterpretHost();
        UnitTestUtil.assertIpsEmpty("第一次请求，没有缓存，应该返回空", ips);
        ServerStatusHelper.hasReceiveAppInterpretHostRequest("当没有缓存时，会异步请求服务器", app, server, 1);
        String[] serverResponseIps = ServerStatusHelper.getServerResponseIps(app, server);
        ips = app.requestInterpretHost();
        ServerStatusHelper.hasNotReceiveAppInterpretHostRequest("当有缓存时，不会请求服务器", app, server);
        UnitTestUtil.assertIpsEqual("解析域名返回服务器结果", serverResponseIps, ips);

        // 重置实例
        HttpDns.resetInstance();

        // 重启应用，获取新的实例
        app.start(new HttpDnsServer[]{server, server1, server2}, speedTestServer);
        app.enableCache(false);

        ips = app.requestInterpretHost();
        UnitTestUtil.assertIpsEmpty("之前没有缓存，应该返回空", ips);
        ServerStatusHelper.hasReceiveAppInterpretHostRequest("当没有缓存时，会异步请求服务器", app, server, 1);
    }


    @Test
    public void testCacheClean() {
        app.enableCache(false);
        // 先发起一些请求，缓存一些Ip结果
        String[] ips = app.requestInterpretHost();
        UnitTestUtil.assertIpsEmpty("第一次请求，没有缓存，应该返回空", ips);
        ServerStatusHelper.hasReceiveAppInterpretHostRequest("当没有缓存时，会异步请求服务器", app, server, 1);
        String[] serverResponseIps = ServerStatusHelper.getServerResponseIps(app, server);
        ips = app.requestInterpretHost();
        ServerStatusHelper.hasNotReceiveAppInterpretHostRequest("当有缓存时，不会请求服务器", app, server);
        UnitTestUtil.assertIpsEqual("解析域名返回服务器结果", serverResponseIps, ips);

        // 重置实例，确保下次读取的信息是从本地缓存来的
        HttpDns.resetInstance();

        // 重启应用，获取新的实例
        app.start(new HttpDnsServer[]{server, server1, server2}, speedTestServer);
        app.enableCache(true);

        ips = app.requestInterpretHost();
        ServerStatusHelper.hasNotReceiveAppInterpretHostRequest("当本地有缓存时，不会请求服务器", app, server);
        UnitTestUtil.assertIpsEqual("解析域名返回服务器结果", serverResponseIps, ips);

        // 重置实例，
        HttpDns.resetInstance();

        // 重启应用，获取新的实例
        app.start(new HttpDnsServer[]{server, server1, server2}, speedTestServer);
        app.enableCache(false);

        ips = app.requestInterpretHost();
        UnitTestUtil.assertIpsEmpty("上次读取缓存时把缓存清除了，应该返回空", ips);
        ServerStatusHelper.hasReceiveAppInterpretHostRequest("当没有缓存时，会异步请求服务器", app, server, 1);
    }

    @Test
    public void testIpCache() {
        app.enableCache(false);
        // 先发起一些请求，缓存一些Ip结果
        String[] ips = app.requestInterpretHost();
        UnitTestUtil.assertIpsEmpty("第一次请求，没有缓存，应该返回空", ips);
        ServerStatusHelper.hasReceiveAppInterpretHostRequest("当没有缓存时，会异步请求服务器", app, server, 1);
        String[] serverResponseIps = ServerStatusHelper.getServerResponseIps(app, server);
        ips = app.requestInterpretHost();
        ServerStatusHelper.hasNotReceiveAppInterpretHostRequest("当有缓存时，不会请求服务器", app, server);
        UnitTestUtil.assertIpsEqual("解析域名返回服务器结果", serverResponseIps, ips);

        // 重置实例，确保下次读取的信息是从本地缓存来的
        HttpDns.resetInstance();

        // 重启应用，获取新的实例
        app.start(new HttpDnsServer[]{server, server1, server2}, speedTestServer);
        app.enableCache(false);

        ips = app.requestInterpretHost();
        ServerStatusHelper.hasNotReceiveAppInterpretHostRequest("当本地有缓存时，不会请求服务器", app, server);
        UnitTestUtil.assertIpsEqual("解析域名返回服务器结果", serverResponseIps, ips);
    }


    @Test
    public void testIpCacheWhenExpired() throws InterruptedException {
        app.enableCache(false);
        app.enableExpiredIp(false);
        // 先发起一些请求，缓存一些Ip结果
        InterpretHostResponse response = ServerHelper.randomInterpretHostResponse(app.getRequestHost(), 1);
        InterpretHostResponse response1 = ServerHelper.randomInterpretHostResponse(app.getRequestHost(), 1);
        server.getInterpretHostServer().preSetRequestResponse(app.getRequestHost(), response, 1);
        server.getInterpretHostServer().preSetRequestResponse(app.getRequestHost(), response1, -1);
        String[] ips = app.requestInterpretHost();
        UnitTestUtil.assertIpsEmpty("第一次请求，没有缓存，应该返回空", ips);
        ServerStatusHelper.hasReceiveAppInterpretHostRequestWithResult("当没有缓存时，会异步请求服务器", app, app.getRequestHost(), server, response, 1, true);
        ips = app.requestInterpretHost();
        ServerStatusHelper.hasNotReceiveAppInterpretHostRequest("当有缓存时，不会请求服务器", app, server);
        UnitTestUtil.assertIpsEqual("解析域名返回服务器结果", response.getIps(), ips);

        // 重置实例，确保下次读取的信息是从本地缓存来的
        HttpDns.resetInstance();

        Thread.sleep(1000);

        // 重启应用，获取新的实例
        app.start(new HttpDnsServer[]{server, server1, server2}, speedTestServer);
        app.enableCache(false);
        app.enableExpiredIp(false);

        ips = app.requestInterpretHost();
        ServerStatusHelper.hasReceiveAppInterpretHostRequestWithResult("本地缓存过期时，会触发网络请求", app, server, response1);
        UnitTestUtil.assertIpsEqual("本地缓存即使过期也会返回ip", ips, response.getIps());

        Thread.sleep(1000);

        ips = app.requestInterpretHost();
        ServerStatusHelper.hasReceiveAppInterpretHostRequestWithResult("ttl过期，会触发网络请求", app, server, response1);
        UnitTestUtil.assertIpsEmpty("后续更新后，不会判定为从本地缓存读取，应该返回空", ips);
    }


    @Test
    public void testHostFilter() {
        app.setFilter();

        app.requestInterpretHost();
        app.waitForAppThread();
        String[] ips = app.requestInterpretHost();
        ServerStatusHelper.hasNotReceiveAppInterpretHostRequest("因为域名过滤了,不会请求服务器", app, server);
        UnitTestUtil.assertIpsEmpty("因为域名过滤了解析一直为空", ips);

        ServerStatusHelper.requestInterpretAnotherHost("其它域名不受过滤影响", app, server);
    }


    @Test
    public void testHostFilterForResolve() {
        app.setFilter();

        String anotherHost = RandomValue.randomHost();
        ArrayList<String> list = new ArrayList<>();
        list.add(app.getRequestHost());
        list.add(anotherHost);
        app.preInterpreHost(list, RequestIpType.v4);
        app.waitForAppThread();
        String[] ips = app.requestInterpretHost();
        ServerStatusHelper.hasNotReceiveAppInterpretHostRequest("因为域名过滤了,不会请求服务器", app, server);
        UnitTestUtil.assertIpsEmpty("因为域名过滤了解析一直为空", ips);

        MatcherAssert.assertThat("因为域名过滤了,不会请求到服务器", !server.getResolveHostServer().hasRequestForArg(ServerHelper.formResolveHostArg(list, RequestIpType.v4), 1, false));
        list.remove(app.getRequestHost());
        MatcherAssert.assertThat("没过滤的域名会请求到服务器", server.getResolveHostServer().hasRequestForArg(ServerHelper.formResolveHostArg(list, RequestIpType.v4), 1, false));
    }

    @Test
    public void testAuthSign() {
        String account = RandomValue.randomStringWithFixedLength(20);
        String secret = server.createSecretFor(account);
        BusinessApp app2 = new BusinessApp(account, secret);
        app2.start(new HttpDnsServer[]{server, server1, server2}, speedTestServer);

        String host = RandomValue.randomHost();
        app2.requestInterpretHost(host);
        app2.waitForAppThread();

        ArrayList<String> params = new ArrayList<>();
        params.add("s");
        params.add("t");
        params.add("host");
        MatcherAssert.assertThat("请求服务器时，发出的是带签名的请求", server.getInterpretHostServer().hasRequestForArgWithParams(ServerHelper.formInterpretHostArg(host, RequestIpType.v4), params, 1, false));
    }


    @Test
    public void testAuthSignValid() {
        String account = RandomValue.randomStringWithFixedLength(20);
        String secret = server.createSecretFor(account);
        BusinessApp app2 = new BusinessApp(account, secret);
        app2.start(new HttpDnsServer[]{server, server1, server2}, speedTestServer);
        app2.setTime(System.currentTimeMillis() / 1000 - 11 * 60);

        String host = RandomValue.randomHost();
        app2.requestInterpretHost(host);
        app2.waitForAppThread();

        ArrayList<String> params = new ArrayList<>();
        params.add("s");
        params.add("t");
        params.add("host");
        MatcherAssert.assertThat("超过有效期，服务不处理", !server.getInterpretHostServer().hasRequestForArgWithParams(ServerHelper.formInterpretHostArg(host, RequestIpType.v4), params, 1, false));
    }

    @Test
    @Config(shadows = {ShadowNetworkInfo.class})
    public void testResolveAfterNetworkChanged() {
        app.changeToNetwork(ConnectivityManager.TYPE_WIFI);
        // 先解析域名，使缓存有数据
        String host1 = RandomValue.randomHost();
        String host2 = RandomValue.randomHost();
        String host3 = RandomValue.randomHost();
        String host4 = RandomValue.randomHost();
        String host5 = RandomValue.randomHost();
        String host6 = RandomValue.randomHost();

        app.requestInterpretHost(host1);
        app.requestInterpretHost(host2);
        app.requestInterpretHost(host3);
        app.requestInterpretHost(host4);
        app.requestInterpretHost(host5);
        app.requestInterpretHost(host6);
        app.waitForAppThread();

        app.enableResolveAfterNetworkChange(true);

        app.changeToNetwork(ConnectivityManager.TYPE_MOBILE);
        app.waitForAppThread();
        UnitTestUtil.assertIpsEqual("网络变化之后，会重新解析已解析的域名", app.requestInterpretHost(host1), server.getResolveHostServer().getReponseForHost(host1, RequestIpType.v4).getItem(host1).getIps());
        UnitTestUtil.assertIpsEqual("网络变化之后，会重新解析已解析的域名", app.requestInterpretHost(host2), server.getResolveHostServer().getReponseForHost(host2, RequestIpType.v4).getItem(host2).getIps());
        UnitTestUtil.assertIpsEqual("网络变化之后，会重新解析已解析的域名", app.requestInterpretHost(host3), server.getResolveHostServer().getReponseForHost(host3, RequestIpType.v4).getItem(host3).getIps());
        UnitTestUtil.assertIpsEqual("网络变化之后，会重新解析已解析的域名", app.requestInterpretHost(host4), server.getResolveHostServer().getReponseForHost(host4, RequestIpType.v4).getItem(host4).getIps());
        UnitTestUtil.assertIpsEqual("网络变化之后，会重新解析已解析的域名", app.requestInterpretHost(host5), server.getResolveHostServer().getReponseForHost(host5, RequestIpType.v4).getItem(host5).getIps());
        UnitTestUtil.assertIpsEqual("网络变化之后，会重新解析已解析的域名", app.requestInterpretHost(host6), server.getResolveHostServer().getReponseForHost(host6, RequestIpType.v4).getItem(host6).getIps());

        app.enableResolveAfterNetworkChange(false);
        app.changeToNetwork(ConnectivityManager.TYPE_WIFI);
        app.waitForAppThread();

        String[] ips = app.requestInterpretHost(host3);
        UnitTestUtil.assertIpsEmpty("没有开启网络变化预解析时，网络变换只会清除现有缓存", ips);
    }

    @Test
    @Config(shadows = {ShadowNetworkInfo.class})
    public void testNotResolveWhenNetworkDisconnect() {

        app.changeToNetwork(ConnectivityManager.TYPE_MOBILE);

        // 先解析域名，使缓存有数据
        String host1 = RandomValue.randomHost();
        String host2 = RandomValue.randomHost();
        String host3 = RandomValue.randomHost();
        String host4 = RandomValue.randomHost();
        String host5 = RandomValue.randomHost();
        String host6 = RandomValue.randomHost();

        app.requestInterpretHost(host1);
        app.requestInterpretHost(host2);
        app.requestInterpretHost(host3);
        app.requestInterpretHost(host4);
        app.requestInterpretHost(host5);
        app.requestInterpretHost(host6);
        app.waitForAppThread();

        app.enableResolveAfterNetworkChange(true);

        app.changeToNetwork(-1);
        app.waitForAppThread();
        app.changeToNetwork(ConnectivityManager.TYPE_MOBILE);
        app.waitForAppThread();

        UnitTestUtil.assertIpsEqual("网络断开再连上相同网络，解析的缓存不变", app.requestInterpretHost(host1), server.getInterpretHostServer().getResponse(host1, 1, false).get(0).getIps());
        UnitTestUtil.assertIpsEqual("网络断开再连上相同网络，解析的缓存不变", app.requestInterpretHost(host2), server.getInterpretHostServer().getResponse(host2, 1, false).get(0).getIps());
        UnitTestUtil.assertIpsEqual("网络断开再连上相同网络，解析的缓存不变", app.requestInterpretHost(host3), server.getInterpretHostServer().getResponse(host3, 1, false).get(0).getIps());
        UnitTestUtil.assertIpsEqual("网络断开再连上相同网络，解析的缓存不变", app.requestInterpretHost(host4), server.getInterpretHostServer().getResponse(host4, 1, false).get(0).getIps());
        UnitTestUtil.assertIpsEqual("网络断开再连上相同网络，解析的缓存不变", app.requestInterpretHost(host5), server.getInterpretHostServer().getResponse(host5, 1, false).get(0).getIps());
        UnitTestUtil.assertIpsEqual("网络断开再连上相同网络，解析的缓存不变", app.requestInterpretHost(host6), server.getInterpretHostServer().getResponse(host6, 1, false).get(0).getIps());
    }

    @Test
    public void serverIpWillUpdateEveryday() {
        String region = "hk";
        String updateServerResponseFor345 = ServerHelper.createUpdateServerResponse(new String[]{server.getServerIp(), server1.getServerIp(), server2.getServerIp()}, RandomValue.randomIpv6s(), new int[]{server.getPort(), server1.getPort(), server2.getPort()}, RandomValue.randomPorts());
        server3.getServerIpsServer().preSetRequestResponse(region, 200, updateServerResponseFor345, -1);
        server4.getServerIpsServer().preSetRequestResponse(region, 200, updateServerResponseFor345, -1);
        server5.getServerIpsServer().preSetRequestResponse(region, 200, updateServerResponseFor345, -1);

        String updateServerResponseFor012 = ServerHelper.createUpdateServerResponse(new String[]{server3.getServerIp(), server4.getServerIp(), server5.getServerIp()}, RandomValue.randomIpv6s(), new int[]{server3.getPort(), server4.getPort(), server5.getPort()}, RandomValue.randomPorts());
        server.getServerIpsServer().preSetRequestResponse(region, 200, updateServerResponseFor012, -1);
        server1.getServerIpsServer().preSetRequestResponse(region, 200, updateServerResponseFor012, -1);
        server2.getServerIpsServer().preSetRequestResponse(region, 200, updateServerResponseFor012, -1);

        // 修改region，更新服务IP 到 server 3 4 5
        app.changeRegionTo(region);
        app.waitForAppThread();

        // 因为我们没法模拟时间经过1天，所以直接修改存储的时间到一天前
        app.changeServerIpUpdateTimeTo(System.currentTimeMillis() - 24 * 60 * 60 * 1000);

        // 重置实例
        HttpDns.resetInstance();

        // 重新初始化应用，自动更新服务IP 到 1 2 3
        app.start(new HttpDnsServer[]{server, server1, server2}, speedTestServer);
        app.waitForAppThread();

        ServerStatusHelper.hasReceiveRegionChange("服务IP超过一天自动更新", app, server3, region, true);

        ServerStatusHelper.requestInterpretAnotherHost("服务IP更新后使用新的服务解析域名", app, server);
    }

    @Test
    public void testDisableService() {
        String region = "hk";
        String disableResponse = ServerHelper.createUpdateServerDisableResponse();
        server.getServerIpsServer().preSetRequestResponse(region, 200, disableResponse, -1);

        // 修改region，触发禁止服务
        app.changeRegionTo(region);
        app.waitForAppThread();

        String host = RandomValue.randomHost();
        String[] ips;
        ips = app.requestInterpretHost(host);
        app.waitForAppThread();
        ips = app.requestInterpretHost(host);
        app.waitForAppThread();
        UnitTestUtil.assertIpsEmpty("服务禁用之后，不会再解析IP", ips);

        app.requestInterpretHostForIpv6(host);
        app.waitForAppThread();
        ips = app.requestInterpretHostForIpv6(host);
        UnitTestUtil.assertIpsEmpty("服务禁用之后，不会再解析IP", ips);
    }

    @Test
    public void testMultiThreadForSameHost() {
        int count = 100;
        while (count > 0) {
            count--;
            app.requestInterpretHost();
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
            }
        }
        ServerStatusHelper.hasReceiveAppInterpretHostRequest("同一个域名同时只会请求一次", app, server, 1);

        String host1 = RandomValue.randomHost();
        String host2 = RandomValue.randomHost();
        String host3 = RandomValue.randomHost();
        ArrayList<String> hostList = new ArrayList<>();
        hostList.add(host1);
        hostList.add(host2);
        hostList.add(host3);
        ResolveHostResponse response = ServerHelper.randomResolveHostResponse(hostList, RequestIpType.both);
        server.getResolveHostServer().preSetRequestResponse(ServerHelper.formResolveHostArg(hostList, RequestIpType.both), response, 1);
        count = 100;
        while (count > 0) {
            count--;
            app.preInterpreHost(hostList, RequestIpType.both);
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
            }
        }
        app.waitForAppThread();
        MatcherAssert.assertThat("预解析相同的域名也只会触发一次请求", server.getResolveHostServer().hasRequestForArg(ServerHelper.formResolveHostArg(hostList, RequestIpType.both), 1, false));
    }

    @Test
    public void testSyncRequest() throws InterruptedException {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        new Thread(new Runnable() {
            @Override
            public void run() {
                String host = RandomValue.randomHost();
                InterpretHostResponse response = ServerHelper.randomInterpretHostResponse(host);
                server.getInterpretHostServer().preSetRequestResponse(host, response, 1);
                String[] ips = app.requestInterpretHostSync(host);
                UnitTestUtil.assertIpsEqual("解析结果和预期相同", ips, response.getIps());
                countDownLatch.countDown();
            }
        }).start();
        countDownLatch.await();
    }

    @Test
    public void testNotCrashWhenCallTwoManyTime() {
        app.checkThreadCount(false);
        prepareUpdateServerResponse("", "hk");
        try {
            for (int i = 0; i < 1000; i++) {
                String host = RandomValue.randomHost();
                switch (i % 7) {
                    case 0:
                        app.requestInterpretHost(host);
                        break;
                    case 1:
                        app.requestInterpretHost();
                        break;
                    case 2:
                        app.requestInterpretHostForIpv6(host);
                        break;
                    case 3:
                        app.requestInterpretHostForIpv6();
                        break;
                    case 4:
                        app.changeRegionTo(RandomValue.randomInt(2) == 0 ? "" : "hk");
                        break;
                    case 5:
                        ArrayList<String> hostList = new ArrayList<>();
                        hostList.add(host);
                        hostList.add(RandomValue.randomHost());
                        hostList.add(RandomValue.randomHost());
                        hostList.add(RandomValue.randomHost());
                        hostList.add(RandomValue.randomHost());
                        hostList.add(RandomValue.randomHost());
                        hostList.add(RandomValue.randomHost());
                        app.preInterpreHost(hostList, RequestIpType.values()[RandomValue.randomInt(3)]);
                        break;
                    case 6:
                        HashMap<String, String> extras = new HashMap<>();
                        extras.put("key", "value");
                        extras.put("key1", "value1");
                        extras.put("key2", "value2");
                        app.requestSDNSInterpretHost(extras, RandomValue.randomStringWithMaxLength(10));
                        break;
                }
            }

            for (int i = 0; i < 3; i++) {
                app.waitForAppThread();
                Thread.sleep(1);
            }
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            MatcherAssert.assertThat("调用次数多，不应该崩溃", false);
        }
    }

    @Test
    public void cleanHostCacheWillRemoveLocalCache() {

        app.requestInterpretHost();
        app.waitForAppThread();
        // 再次请求，获取服务器返回的结果
        String[] ips = app.requestInterpretHost();
        MatcherAssert.assertThat("先解析域名确保有缓存", ips.length > 0 && !ips[0].isEmpty());

        ArrayList<String> hosts = new ArrayList<>();
        hosts.add(app.getRequestHost());
        app.cleanHostCache(hosts);

        ips = app.requestInterpretHost();
        UnitTestUtil.assertIpsEmpty("清除缓存之后，请求会返回空", ips);
    }

    @Test
    public void cleanHostCacheWithoutHostWillRemoveAllHostCache() {
        String host1 = RandomValue.randomHost();
        String host2 = RandomValue.randomHost();
        app.requestInterpretHost(host1);
        app.requestInterpretHost(host2);
        app.waitForAppThread();

        String[] ips1 = app.requestInterpretHost(host1);
        String[] ips2 = app.requestInterpretHost(host2);
        MatcherAssert.assertThat("当前 host1 host2 都有缓存", ips1.length > 0 && !ips1[0].isEmpty() && ips2.length > 0 && !ips2[0].isEmpty());

        app.cleanHostCache(null);

        ips1 = app.requestInterpretHost(host1);
        ips2 = app.requestInterpretHost(host2);
        UnitTestUtil.assertIpsEmpty("清除缓存之后，请求会返回空", ips1);
        UnitTestUtil.assertIpsEmpty("清除缓存之后，请求会返回空", ips2);
    }

    @Test
    public void cleanHostCacheWillCleanCacheInLocalDB() {
        // 启动缓存
        app.enableCache(false);
        // 先发起一些请求，缓存一些Ip结果
        app.requestInterpretHost();
        app.waitForAppThread();
        String[] ips1 = app.requestInterpretHost();

        // 重置实例，确保下次读取的信息是从本地缓存来的
        HttpDns.resetInstance();

        // 重启应用，获取新的实例
        app.start(new HttpDnsServer[]{server, server1, server2}, speedTestServer);
        app.enableCache(false);

        // 读取缓存
        String[] ips2 = app.requestInterpretHost();
        UnitTestUtil.assertIpsEqual("确认缓存生效", ips1, ips2);

        // 重置实例，
        HttpDns.resetInstance();

        // 重启应用，获取新的实例
        app.start(new HttpDnsServer[]{server, server1, server2}, speedTestServer);
        app.enableCache(false);

        String[] ips3 = app.requestInterpretHost();
        UnitTestUtil.assertIpsEqual("确认缓存没有被清除，一直存在", ips3, ips2);

        ArrayList<String> hosts = new ArrayList<>();
        hosts.add(app.getRequestHost());
        app.cleanHostCache(hosts);

        // 重置实例，
        HttpDns.resetInstance();

        // 重启应用，获取新的实例
        app.start(new HttpDnsServer[]{server, server1, server2}, speedTestServer);
        app.enableCache(false);

        String[] ips4 = app.requestInterpretHost();
        UnitTestUtil.assertIpsEmpty("清除缓存会把数据库缓存也清除", ips4);
    }

    /**
     * 这个应该手动执行，耗时太长
     */
    @Test
    @Ignore("耗时太长，需要手动执行")
    public void multiThreadTest() {
        HttpDnsLog.removeLogger(logger);
        app.setTimeout(10 * 1000);
        HttpDnsLog.enable(false);

        final int hostCount = 10;
        final int timeoutCount = 3;
        final String timeoutPrefix = "TIMEOUT";
        final ArrayList<String> hosts = new ArrayList<>(hostCount);
        for (int i = 0; i < hostCount - timeoutCount; i++) {
            hosts.add(RandomValue.randomHost());
        }
        for (int i = 0; i < timeoutCount; i++) {
            hosts.add(timeoutPrefix + RandomValue.randomHost());
        }
        for (int i = 0; i < hostCount; i++) {
            if (hosts.get(i).startsWith(timeoutPrefix)) {
                server.getInterpretHostServer().preSetRequestTimeout(hosts.get(i), -1);
            } else {
                // random response
            }
        }

        final int count = 10;
        final int time = 5 * 60 * 1000;
        final CountDownLatch testLatch = new CountDownLatch(count);
        final AtomicInteger slowCount = new AtomicInteger(0);
        ExecutorService service = Executors.newFixedThreadPool(count);
        final CountDownLatch countDownLatch = new CountDownLatch(count);
        for (int i = 0; i < count; i++) {
            service.execute(new Runnable() {
                @Override
                public void run() {
                    countDownLatch.countDown();
                    try {
                        countDownLatch.await();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    System.out.println(Thread.currentThread().getId() + " begin");
                    int all = 0;
                    int slow = 0;
                    int nullCount = 0;
                    int emptyCount = 0;
                    long maxSlot = 0;
                    long begin = System.currentTimeMillis();
                    while (System.currentTimeMillis() - begin < time) {
                        String host = hosts.get(RandomValue.randomInt(hostCount));
                        long start = System.currentTimeMillis();
                        String[] ips = app.requestInterpretHost(host);
                        long end = System.currentTimeMillis();
                        if (end - start > 100) {
                            slow++;
                            if (maxSlot < end - start) {
                                maxSlot = end - start;
                            }
                        }
                        if (ips == null) {
                            nullCount++;
                        } else if (ips.length == 0) {
                            emptyCount++;
                        }
                        all++;
                    }
                    System.out.println(Thread.currentThread().getId() + " all: " + all + ", slow: " + slow + ", null: " + nullCount + ", empty: " + emptyCount + ", max : " + maxSlot);
                    slowCount.addAndGet(slow);
                    testLatch.countDown();
                }
            });
        }
        try {
            testLatch.await();
        } catch (InterruptedException e) {
        }
        MatcherAssert.assertThat("返回慢的调用应该为0", slowCount.get(), Matchers.is(Matchers.equalTo(0)));
    }
}
