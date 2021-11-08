package com.alibaba.sdk.android.httpdns.interpret;

import com.alibaba.sdk.android.httpdns.HTTPDNSResult;
import com.alibaba.sdk.android.httpdns.RequestIpType;
import com.alibaba.sdk.android.httpdns.cache.RecordDBHelper;
import com.alibaba.sdk.android.httpdns.impl.HttpDnsConfig;
import com.alibaba.sdk.android.httpdns.probe.ProbeService;
import com.alibaba.sdk.android.httpdns.test.helper.ServerHelper;
import com.alibaba.sdk.android.httpdns.test.utils.RandomValue;
import com.alibaba.sdk.android.httpdns.test.utils.TestExecutorService;
import com.alibaba.sdk.android.httpdns.test.utils.UnitTestUtil;

import org.hamcrest.MatcherAssert;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author zonglin.nzl
 * @date 2020/12/8
 */
@RunWith(RobolectricTestRunner.class)
public class InterpretHostResultRepoTest {

    private String accountId = RandomValue.randomStringWithFixedLength(8);
    private String host = RandomValue.randomHost();
    private InterpretHostResultRepo repo;
    private String[] ips = RandomValue.randomIpv4s();
    private String[] ipv6s = RandomValue.randomIpv6s();
    private ProbeService ipProbeService = Mockito.mock(ProbeService.class);
    private HttpDnsConfig config;
    private TestExecutorService worker;
    private RecordDBHelper dbHelper;

    @Before
    public void setUp() {
        worker = new TestExecutorService(new ThreadPoolExecutor(0, 10, 0, TimeUnit.SECONDS, new SynchronousQueue<Runnable>()));
        config = new HttpDnsConfig(RuntimeEnvironment.application, accountId);
        config.setWorker(worker);
        dbHelper = new RecordDBHelper(config.getContext(), config.getAccountId());
        repo = new InterpretHostResultRepo(config, ipProbeService, dbHelper, new InterpretHostCacheGroup());
    }


    @Test
    public void testSaveUpdateGet() {

        MatcherAssert.assertThat("没有解析过的域名返回空", repo.getIps(host, RequestIpType.v4, null) == null);
        MatcherAssert.assertThat("没有解析过的域名返回空", repo.getIps(host, RequestIpType.v6, null) == null);
        MatcherAssert.assertThat("没有解析过的域名返回空", repo.getIps(host, RequestIpType.both, null) == null);

        InterpretHostResponse response = ServerHelper.randomInterpretHostResponse(host);

        repo.save(host, RequestIpType.v4, null, null, response);
        UnitTestUtil.assertIpsEqual("解析过的域名返回上次的解析结果", repo.getIps(host, RequestIpType.v4, null).getIps(), response.getIps());
        MatcherAssert.assertThat("没有解析过的域名返回空", repo.getIps(host, RequestIpType.v6, null) == null);
        MatcherAssert.assertThat("get方法传入both时，返回空", repo.getIps(host, RequestIpType.both, null) == null);

        repo.save(host, RequestIpType.v6, null, null, response);
        UnitTestUtil.assertIpsEqual("解析过的域名返回上次的解析结果", repo.getIps(host, RequestIpType.v4, null).getIps(), response.getIps());
        UnitTestUtil.assertIpsEqual("解析过的域名返回上次的解析结果", repo.getIps(host, RequestIpType.v6, null).getIpv6s(), response.getIpsv6());
        UnitTestUtil.assertIpsEqual("get方法传入both时，返回ipv4 ipv6的结果", repo.getIps(host, RequestIpType.both, null).getIps(), response.getIps());
        UnitTestUtil.assertIpsEqual("get方法传入both时，返回ipv4 ipv6的结果", repo.getIps(host, RequestIpType.both, null).getIpv6s(), response.getIpsv6());

        repo.update(host, RequestIpType.v4, null, ips);
        UnitTestUtil.assertIpsEqual("更新结果之后再请求，返回更新后的结果", repo.getIps(host, RequestIpType.v4, null).getIps(), ips);
        repo.update(host, RequestIpType.v6, null, ipv6s);
        UnitTestUtil.assertIpsEqual("更新结果之后再请求，返回更新后的结果", repo.getIps(host, RequestIpType.v6, null).getIpv6s(), ipv6s);

        InterpretHostResponse response1 = ServerHelper.randomInterpretHostResponse(host);
        repo.save(host, RequestIpType.both, null, null, response1);
        UnitTestUtil.assertIpsEqual("新的解析结果会覆盖原来的解析结果", repo.getIps(host, RequestIpType.v4, null).getIps(), response1.getIps());
        UnitTestUtil.assertIpsEqual("新的解析结果会覆盖原来的解析结果", repo.getIps(host, RequestIpType.v6, null).getIpv6s(), response1.getIpsv6());
        UnitTestUtil.assertIpsEqual("新的解析结果会覆盖原来的解析结果", repo.getIps(host, RequestIpType.both, null).getIps(), response1.getIps());
        UnitTestUtil.assertIpsEqual("新的解析结果会覆盖原来的解析结果", repo.getIps(host, RequestIpType.both, null).getIpv6s(), response1.getIpsv6());

        repo.clear();
        MatcherAssert.assertThat("清除记录之后返回空", repo.getIps(host, RequestIpType.v4, null) == null);
        MatcherAssert.assertThat("清除记录之后返回空", repo.getIps(host, RequestIpType.v6, null) == null);
        MatcherAssert.assertThat("清除记录之后返回空", repo.getIps(host, RequestIpType.both, null) == null);
    }

    @Test
    public void testLocalCache() throws JSONException {
        repo.setCachedIPEnabled(true, false);
        try {
            worker.await();
        } catch (InterruptedException e) {
        }
        HashMap<String, InterpretHostResponse> responses = new HashMap<>();
        HashMap<String, RequestIpType> types = new HashMap<>();
        HashMap<String, String> cacheKeys = new HashMap<>();
        HashMap<String, String> extras = new HashMap<>();

        // 存储预解析数据
        for (int k = 0; k < 3; k++) {
            RequestIpType type = RequestIpType.values()[k];
            int preCount = RandomValue.randomInt(10) + 5;
            ArrayList<String> preHosts = new ArrayList<>();
            for (int i = 0; i < preCount; i++) {
                preHosts.add(RandomValue.randomHost());
            }
            ResolveHostResponse resolveHostResponse = ServerHelper.randomResolveHostResponse(preHosts, type);
            repo.save(type, resolveHostResponse);
            for (String host : resolveHostResponse.getHosts()) {
                if (type == RequestIpType.v4) {
                    responses.put(host, new InterpretHostResponse(host, resolveHostResponse.getItem(host).getIps(), null, resolveHostResponse.getItem(host).getTtl(), null));
                } else if (type == RequestIpType.v6) {
                    responses.put(host, new InterpretHostResponse(host, null, resolveHostResponse.getItem(host).getIpv6s(), resolveHostResponse.getItem(host).getTtl(), null));
                } else {
                    responses.put(host, new InterpretHostResponse(host, resolveHostResponse.getItem(host).getIps(), resolveHostResponse.getItem(host).getIpv6s(), resolveHostResponse.getItem(host).getTtl(), null));
                }
                types.put(host, type);
                cacheKeys.put(host, null);
                extras.put(host, null);
            }
        }

        // 存储解析数据
        for (int i = 0; i < 50; i++) {
            String host = RandomValue.randomHost();
            InterpretHostResponse response = ServerHelper.randomInterpretHostResponse(host);
            boolean sdns = RandomValue.randomInt(1) == 1;
            String cacheKey = sdns ? RandomValue.randomStringWithMaxLength(10) : null;
            String extra = sdns ? RandomValue.randomJsonMap() : null;
            RequestIpType type = RequestIpType.values()[RandomValue.randomInt(3)];
            repo.save(host, type, extra, cacheKey, response);
            responses.put(host, response);
            types.put(host, type);
            cacheKeys.put(host, cacheKey);
            extras.put(host, extra);
        }

        ArrayList<String> hosts = new ArrayList<>(responses.keySet());
        // 更新30个记录
        for (int i = 0; i < 30; i++) {
            String host = hosts.get(RandomValue.randomInt(hosts.size()));
            InterpretHostResponse response = ServerHelper.randomInterpretHostResponse(host);
            repo.save(host, types.get(host), extras.get(host), cacheKeys.get(host), response);
            responses.put(host, response);
        }

        // 更新30个ip记录
        for (int i = 0; i < 30; i++) {
            String host = hosts.get(RandomValue.randomInt(hosts.size()));
            if (types.get(host) != RequestIpType.v6) {
                InterpretHostResponse old = responses.get(host);
                InterpretHostResponse response = new InterpretHostResponse(old.getHostName(), RandomValue.randomIpv4s(), old.getIpsv6(), old.getTtl(), old.getExtras());
                repo.update(host, RequestIpType.v4, cacheKeys.get(host), response.getIps());
                responses.put(host, response);
            }
        }

        InterpretHostResultRepo anotherRepo = new InterpretHostResultRepo(config, ipProbeService, dbHelper, new InterpretHostCacheGroup());
        anotherRepo.setCachedIPEnabled(true, false);
        try {
            worker.await();
        } catch (InterruptedException e) {
        }

        for (String host : hosts) {
            HTTPDNSResult result = anotherRepo.getIps(host, types.get(host), cacheKeys.get(host));
            if (types.get(host) != RequestIpType.v4) {
                UnitTestUtil.assertIpsEqual("测试repo本地缓存", result.getIpv6s(), responses.get(host).getIpsv6());
            }
            if (types.get(host) != RequestIpType.v6) {
                UnitTestUtil.assertIpsEqual("测试repo本地缓存", result.getIps(), responses.get(host).getIps());
            }
            if (cacheKeys.get(host) != null) {
                assertExtras("测试repo本地缓存", result.getExtras(), responses.get(host).getExtras());
            }
        }
    }

    private void assertExtras(String reason, Map<String, String> extras, String extraStr) throws JSONException {
        JSONObject jsonObject = new JSONObject(extraStr);
        if (extras.size() == 0) {
            MatcherAssert.assertThat(reason, extraStr == null);
            return;
        }
        for (String key : extras.keySet()) {
            MatcherAssert.assertThat(reason, extras.get(key).equals(jsonObject.optString(key)));
        }
    }
}
