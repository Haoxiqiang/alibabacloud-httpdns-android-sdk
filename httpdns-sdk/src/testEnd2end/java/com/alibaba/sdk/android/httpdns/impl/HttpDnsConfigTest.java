package com.alibaba.sdk.android.httpdns.impl;

import com.alibaba.sdk.android.httpdns.test.utils.RandomValue;
import com.alibaba.sdk.android.httpdns.test.utils.UnitTestUtil;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

/**
 * @author zonglin.nzl
 * @date 2020/12/3
 */
@RunWith(RobolectricTestRunner.class)
public class HttpDnsConfigTest {
    private HttpDnsConfig config;
    private String account = RandomValue.randomStringWithFixedLength(6);
    private String[] serverIps = RandomValue.randomIpv4s();
    private int[] ports = RandomValue.randomPorts();

    @Before
    public void setUp() {
        config = new HttpDnsConfig(RuntimeEnvironment.application, account);
        config.setInitServers(serverIps, null);
    }

    @Test
    public void setServerIpsWillResetServerStatus() {
        config.shiftServer(config.getServerIp(), config.getPort());
        config.markOkServer(config.getServerIp(), config.getPort());
        MatcherAssert.assertThat("current ok server is " + serverIps[1], config.getServerIp().equals(serverIps[1]));
        String[] newServerIps = RandomValue.randomIpv4s();
        config.setServerIps(newServerIps, null);
        MatcherAssert.assertThat("current ok server is " + newServerIps[0], config.getServerIp().equals(newServerIps[0]));
    }

    @Test
    public void shiftServerTest() {
        config.setServerIps(serverIps, ports);
        MatcherAssert.assertThat("default index is first", serverIps[0].equals(config.getServerIp()));
        MatcherAssert.assertThat("default index is first", ports[0] == config.getPort());

        config.markOkServer(config.getServerIp(), config.getPort());

        checkShiftResult(1);

        checkShiftResult(2);

        MatcherAssert.assertThat("server ip is back to first ip", checkShiftResult(0));

        checkShiftResult(1);

        config.markOkServer(config.getServerIp(), config.getPort());

        checkShiftResult(2);

        MatcherAssert.assertThat("first ip means last ok server", !checkShiftResult(0));

        MatcherAssert.assertThat("server ip is back to last ok ip", checkShiftResult(1));
    }

    private boolean checkShiftResult(int i) {
        boolean isBackToFirst = config.shiftServer(config.getServerIp(), config.getPort());

        MatcherAssert.assertThat("shift server", serverIps[i].equals(config.getServerIp()));
        MatcherAssert.assertThat("shift server", ports[i] == config.getPort());

        return isBackToFirst;
    }


    @Test
    public void shiftServerTest2() {
        MatcherAssert.assertThat("default index is first", serverIps[0].equals(config.getServerIp()));

        config.markOkServer(config.getServerIp(), config.getPort());

        checkShiftResult2(1);

        checkShiftResult2(2);

        MatcherAssert.assertThat("server ip is back to first ip", checkShiftResult2(0));

        checkShiftResult2(1);

        config.markOkServer(config.getServerIp(), config.getPort());

        checkShiftResult2(2);

        MatcherAssert.assertThat("first ip means last ok server", !checkShiftResult2(0));

        MatcherAssert.assertThat("server ip is back to last ok ip", checkShiftResult2(1));
    }

    private boolean checkShiftResult2(int i) {
        boolean isBackToFirst = config.shiftServer(config.getServerIp(), config.getPort());

        MatcherAssert.assertThat("shift server", serverIps[i].equals(config.getServerIp()));

        return isBackToFirst;
    }

    @Test
    public void markCurrentServerSuccess() {
        MatcherAssert.assertThat("current Server can mark", config.markOkServer(config.getServerIp(), config.getPort()));
    }

    @Test
    public void markOtherServerFail() {
        String ip = config.getServerIp();
        int port = config.getPort();
        config.shiftServer(ip, port);
        MatcherAssert.assertThat("only current Server can mark", !config.markOkServer(ip, port));
    }

    @Test
    public void testCopy() {
        // 调用api，使config内部的状态发生变化
        config.setServerIps(serverIps, ports);
        config.shiftServer(config.getServerIp(), config.getPort());
        config.markOkServer(config.getServerIp(), config.getPort());
        config.shiftServer(config.getServerIp(), config.getPort());

        HttpDnsConfig testConfig = config.copy();
        MatcherAssert.assertThat("copy的状态应该一模一样", testConfig.equals(config));
        MatcherAssert.assertThat("copy的不应该是一个实例", testConfig != config);
    }

    @Test
    public void testResetToInitServer() {
        String[] initIps = RandomValue.randomIpv4s();
        int[] initPorts = RandomValue.randomPorts();

        config.setInitServers(initIps, null);
        config.setServerIps(serverIps, ports);
        config.resetServerIpsToInitServer();
        MatcherAssert.assertThat("reset whill change server to init servers", config.getServerIps(), Matchers.arrayContaining(initIps));
        MatcherAssert.assertThat("reset whill change server to init servers", config.getPorts() == null);

        config.setInitServers(initIps, initPorts);
        config.setServerIps(serverIps, ports);
        config.resetServerIpsToInitServer();
        MatcherAssert.assertThat("reset whill change server to init servers", config.getServerIps(), Matchers.arrayContaining(initIps));
        UnitTestUtil.assertIntArrayEquals(initPorts, config.getPorts());
    }

    @Test
    public void testGetInitServerSize() {
        String[] initIps = RandomValue.randomIpv4s();
        config.setInitServers(initIps, null);
        MatcherAssert.assertThat("getInitServerSize", config.getInitServerSize() == initIps.length);
    }

    @Test
    public void setSameServerWillReturnFalse() {
        config.setServerIps(serverIps, ports);
        MatcherAssert.assertThat("same server will return false", !config.setServerIps(serverIps, ports));
    }

    @Test
    public void testConfigCache() {
        int randomShift = RandomValue.randomInt(6);
        while (randomShift > 0) {
            randomShift--;
            config.shiftServer(config.getServerIp(), config.getPort());
        }
        config.markOkServer(config.getServerIp(), config.getPort());

        HttpDnsConfig another = new HttpDnsConfig(RuntimeEnvironment.application, account);
        MatcherAssert.assertThat("当前服务更新后会缓存到本地，再次创建时读取缓存", another.getServerIp(), Matchers.equalTo(config.getServerIp()));
        MatcherAssert.assertThat("当前服务更新后会缓存到本地，再次创建时读取缓存", another.getPort(), Matchers.equalTo(config.getPort()));

        config.setServerIps(RandomValue.randomIpv4s(), RandomValue.randomPorts());

        another = new HttpDnsConfig(RuntimeEnvironment.application, account);
        MatcherAssert.assertThat("服务更新后会缓存到本地，再次创建时读取缓存", another.getServerIp(), Matchers.equalTo(config.getServerIp()));
        MatcherAssert.assertThat("服务更新后会缓存到本地，再次创建时读取缓存", another.getPort(), Matchers.equalTo(config.getPort()));
    }


}
