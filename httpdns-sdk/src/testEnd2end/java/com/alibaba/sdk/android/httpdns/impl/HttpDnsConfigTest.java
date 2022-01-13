package com.alibaba.sdk.android.httpdns.impl;

import com.alibaba.sdk.android.httpdns.test.utils.RandomValue;

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
        config.getServerConfig().shiftServer(config.getServerConfig().getServerIp(), config.getServerConfig().getPort());
        config.getServerConfig().markOkServer(config.getServerConfig().getServerIp(), config.getServerConfig().getPort());
        MatcherAssert.assertThat("current ok server is " + serverIps[1], config.getServerConfig().getServerIp().equals(serverIps[1]));
        String[] newServerIps = RandomValue.randomIpv4s();
        config.getServerConfig().setServerIps(null, newServerIps, null);
        MatcherAssert.assertThat("current ok server is " + newServerIps[0], config.getServerConfig().getServerIp().equals(newServerIps[0]));
    }

    @Test
    public void shiftServerTest() {
        config.getServerConfig().setServerIps(null, serverIps, ports);
        MatcherAssert.assertThat("default index is first", serverIps[0].equals(config.getServerConfig().getServerIp()));
        MatcherAssert.assertThat("default index is first", ports[0] == config.getServerConfig().getPort());

        config.getServerConfig().markOkServer(config.getServerConfig().getServerIp(), config.getServerConfig().getPort());

        checkShiftResult(1);

        checkShiftResult(2);

        MatcherAssert.assertThat("server ip is back to first ip", checkShiftResult(0));

        checkShiftResult(1);

        config.getServerConfig().markOkServer(config.getServerConfig().getServerIp(), config.getServerConfig().getPort());

        checkShiftResult(2);

        MatcherAssert.assertThat("first ip means last ok server", !checkShiftResult(0));

        MatcherAssert.assertThat("server ip is back to last ok ip", checkShiftResult(1));
    }

    private boolean checkShiftResult(int i) {
        boolean isBackToFirst = config.getServerConfig().shiftServer(config.getServerConfig().getServerIp(), config.getServerConfig().getPort());

        MatcherAssert.assertThat("shift server", serverIps[i].equals(config.getServerConfig().getServerIp()));
        MatcherAssert.assertThat("shift server", ports[i] == config.getServerConfig().getPort());

        return isBackToFirst;
    }


    @Test
    public void shiftServerTest2() {
        MatcherAssert.assertThat("default index is first", serverIps[0].equals(config.getServerConfig().getServerIp()));

        config.getServerConfig().markOkServer(config.getServerConfig().getServerIp(), config.getServerConfig().getPort());

        checkShiftResult2(1);

        checkShiftResult2(2);

        MatcherAssert.assertThat("server ip is back to first ip", checkShiftResult2(0));

        checkShiftResult2(1);

        config.getServerConfig().markOkServer(config.getServerConfig().getServerIp(), config.getServerConfig().getPort());

        checkShiftResult2(2);

        MatcherAssert.assertThat("first ip means last ok server", !checkShiftResult2(0));

        MatcherAssert.assertThat("server ip is back to last ok ip", checkShiftResult2(1));
    }

    private boolean checkShiftResult2(int i) {
        boolean isBackToFirst = config.getServerConfig().shiftServer(config.getServerConfig().getServerIp(), config.getServerConfig().getPort());

        MatcherAssert.assertThat("shift server", serverIps[i].equals(config.getServerConfig().getServerIp()));

        return isBackToFirst;
    }

    @Test
    public void markCurrentServerSuccess() {
        MatcherAssert.assertThat("current Server can mark", config.getServerConfig().markOkServer(config.getServerConfig().getServerIp(), config.getServerConfig().getPort()));
    }

    @Test
    public void markOtherServerFail() {
        String ip = config.getServerConfig().getServerIp();
        int port = config.getServerConfig().getPort();
        config.getServerConfig().shiftServer(ip, port);
        MatcherAssert.assertThat("only current Server can mark", !config.getServerConfig().markOkServer(ip, port));
    }

    @Test
    public void testGetInitServerSize() {
        String[] initIps = RandomValue.randomIpv4s();
        config.setInitServers(initIps, null);
        MatcherAssert.assertThat("getInitServerSize", config.getInitServerSize() == initIps.length);
    }

    @Test
    public void setSameServerWillReturnFalse() {
        config.getServerConfig().setServerIps(null, serverIps, ports);
        MatcherAssert.assertThat("same server will return false", !config.getServerConfig().setServerIps(null, serverIps, ports));
    }

    @Test
    public void testConfigCache() {
        int randomShift = RandomValue.randomInt(6);
        while (randomShift > 0) {
            randomShift--;
            config.getServerConfig().shiftServer(config.getServerConfig().getServerIp(), config.getServerConfig().getPort());
        }
        config.getServerConfig().markOkServer(config.getServerConfig().getServerIp(), config.getServerConfig().getPort());

        HttpDnsConfig another = new HttpDnsConfig(RuntimeEnvironment.application, account);
        MatcherAssert.assertThat("当前服务更新后会缓存到本地，再次创建时读取缓存", another.getServerConfig().getServerIp(), Matchers.equalTo(config.getServerConfig().getServerIp()));
        MatcherAssert.assertThat("当前服务更新后会缓存到本地，再次创建时读取缓存", another.getServerConfig().getPort(), Matchers.equalTo(config.getServerConfig().getPort()));

        config.getServerConfig().setServerIps(null, RandomValue.randomIpv4s(), RandomValue.randomPorts());

        another = new HttpDnsConfig(RuntimeEnvironment.application, account);
        MatcherAssert.assertThat("服务更新后会缓存到本地，再次创建时读取缓存", another.getServerConfig().getServerIp(), Matchers.equalTo(config.getServerConfig().getServerIp()));
        MatcherAssert.assertThat("服务更新后会缓存到本地，再次创建时读取缓存", another.getServerConfig().getPort(), Matchers.equalTo(config.getServerConfig().getPort()));
    }

    @Test
    public void testRegionUpdate() {

        MatcherAssert.assertThat("默认region是国内", config.getRegion(), Matchers.nullValue());
        MatcherAssert.assertThat("默认服务节点是国内的", config.getServerConfig().getCurrentServerRegion(), Matchers.nullValue());
        MatcherAssert.assertThat("默认region匹配", config.isCurrentRegionMatch(), Matchers.is(true));

        config.setRegion("hk");

        MatcherAssert.assertThat("setRegion 更新成功", config.getRegion(), Matchers.is(Matchers.equalTo("hk")));
        MatcherAssert.assertThat("setRegion不影响服务节点的region", config.getServerConfig().getCurrentServerRegion(), Matchers.nullValue());
        MatcherAssert.assertThat("setRegion 导致region不匹配", config.isCurrentRegionMatch(), Matchers.is(false));

        config.getServerConfig().setServerIps("hk", RandomValue.randomIpv4s(), RandomValue.randomPorts());


        MatcherAssert.assertThat("setRegion 更新成功", config.getRegion(), Matchers.is(Matchers.equalTo("hk")));
        MatcherAssert.assertThat("setServerIps更像服务节点", config.getServerConfig().getCurrentServerRegion(), Matchers.is(Matchers.equalTo("hk")));
        MatcherAssert.assertThat("服务节点更新后，region匹配", config.isCurrentRegionMatch(), Matchers.is(true));

    }

}
