package com.aliyun.ams.httpdns.demo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.alibaba.sdk.android.httpdns.DegradationFilter;
import com.alibaba.sdk.android.httpdns.HTTPDNSResult;
import com.alibaba.sdk.android.httpdns.HttpDns;
import com.alibaba.sdk.android.httpdns.HttpDnsService;
import com.alibaba.sdk.android.httpdns.ILogger;
import com.alibaba.sdk.android.httpdns.RequestIpType;
import com.alibaba.sdk.android.httpdns.SyncService;
import com.alibaba.sdk.android.httpdns.log.HttpDnsLog;
import com.alibaba.sdk.android.httpdns.probe.IPProbeItem;
import com.alibaba.sdk.android.logger.LogLevel;
import com.alibaba.sdk.android.sender.SenderLog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity implements View.OnClickListener {

    private static final String APPLE_URL = "www.apple.com";
    private static final String TAOBAO_URL = "www.taobao.com";
    private static final String DOUBAN_URL = "dou.bz";
    private static final String IPV6_HOST = "ipv6.sjtu.edu.cn";
    private static final String HTTP_SCHEMA = "http://";
    private static final String HTTPS_SCHEMA = "https://";
    private static final String TAG = "httpdns_android_demo";
    private static final String HTTPDNS_RESULT = "httpdns_result";
    private String TARGET_URL = DOUBAN_URL;

    public static final String accountID = "100000";
    public static final int SLEEP_INTERVAL = 5 * 1000;
    public static final int MESSAGE_NORMAL_KEY = 10001;
    public static final int MESSAGE_HTTPS_KEY = 10002;

    private String currentRegion = "";


    private Button btnNormalParse;
    private Button btnHttpsParse;
    private Button btnTimeout;
    private Button btnSetExpired;
    private Button btnDegrationFilter;
    private Button btnPreresolve;
    private Button btnRequestTaobao;
    private Button btnRequestApple;
    private Button btnRequestDouban;
    private Button btnSetCache;
    private Button btnRequestIpv6;
    private Button btnIpv6Parse;
    private TextView tvConsole;


    public static HttpDnsService httpdns;
    private static ExecutorService pool = Executors.newSingleThreadExecutor();

    private Handler mHandler = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();
        initHttpDns();
        initHandler();
    }

    private void initViews() {
        this.btnNormalParse = (Button) findViewById(R.id.btnNormalParse);
        this.btnHttpsParse = (Button) findViewById(R.id.btnHttpsParse);
        this.btnTimeout = (Button) findViewById(R.id.btnTimeout);
        this.btnSetExpired = (Button) findViewById(R.id.btnSetExpired);
        this.btnDegrationFilter = (Button) findViewById(R.id.btnDegrationFilter);
        this.btnPreresolve = (Button) findViewById(R.id.btnPreResove);
        this.btnRequestApple = (Button) findViewById(R.id.btnRequestApple);
        this.btnRequestTaobao = (Button) findViewById(R.id.btnRequestTaobao);
        this.btnRequestDouban = (Button) findViewById(R.id.btnRequestDouban);
        this.btnSetCache = (Button) findViewById(R.id.btnSetCache);
        this.tvConsole = (TextView) findViewById(R.id.tvConsoleText);
        this.btnRequestIpv6 = (Button) findViewById(R.id.btnRequestIpv6);
        this.btnIpv6Parse = (Button) findViewById(R.id.btnIpv6Parse);
        findViewById(R.id.btnRegion).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentRegion.equals("")) {
                    currentRegion = "sg";
                } else if (currentRegion.equals("sg")) {
                    currentRegion = "hk";
                } else {
                    currentRegion = "";
                }
                httpdns.setRegion(currentRegion);
            }
        });

        findViewById(R.id.btnSyncRequest).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if (httpdns instanceof SyncService) {
                            HTTPDNSResult result = ((SyncService) httpdns).getByHost(TARGET_URL, RequestIpType.v4);
                            Message msg = mHandler.obtainMessage();
                            msg.what = MESSAGE_NORMAL_KEY;
                            String log = "Get IPV4 for host:" + TARGET_URL;
                            if (result != null && result.getIps() != null) {
                                msg.obj = log + " success. ips:" + Arrays.toString(result.getIps());
                            } else {
                                msg.obj = log + " failed.";
                            }
                            mHandler.sendMessage(msg);
                        }
                    }
                }).start();
            }
        });

        findViewById(R.id.btnMultiSyncRequest).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final int count = 4;
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
                            if (httpdns instanceof SyncService) {
                                Log.d("Sync", "Sync Get IPV4 for host:" + TARGET_URL + " begin");
                                HTTPDNSResult result = ((SyncService) httpdns).getByHost(TARGET_URL, RequestIpType.v4);
                                Message msg = mHandler.obtainMessage();
                                msg.what = MESSAGE_NORMAL_KEY;
                                String log = "Get IPV4 for host:" + TARGET_URL;
                                if (result != null && result.getIps() != null) {
                                    msg.obj = log + " success. ips:" + Arrays.toString(result.getIps());
                                } else {
                                    msg.obj = log + " failed.";
                                }
                                Log.d("Sync", (String) msg.obj);
                                mHandler.sendMessage(msg);
                            }
                        }
                    });
                }
            }
        });

        findViewById(R.id.btnMultiRequest).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, MultiTheadActivity.class));
            }
        });

        this.btnNormalParse.setOnClickListener(this);
        this.btnHttpsParse.setOnClickListener(this);
        this.btnTimeout.setOnClickListener(this);
        this.btnSetExpired.setOnClickListener(this);
        this.btnDegrationFilter.setOnClickListener(this);
        this.btnPreresolve.setOnClickListener(this);
        this.btnRequestTaobao.setOnClickListener(this);
        this.btnRequestApple.setOnClickListener(this);
        this.btnRequestDouban.setOnClickListener(this);
        this.btnSetCache.setOnClickListener(this);
        this.btnRequestIpv6.setOnClickListener(this);
        this.btnIpv6Parse.setOnClickListener(this);

    }

    private void initHttpDns() {
        // 初始化httpdns
        SenderLog.setLevel(LogLevel.DEBUG);
        HttpDnsLog.enable(false);
//        HttpDns.enableIPv6Service(this,(true);
        httpdns = HttpDns.getService(getApplicationContext(), accountID);
        httpdns.setLogEnabled(false);
        httpdns.setLogger(new ILogger() {
            @Override
            public void log(String msg) {
                Log.d("test", "from sdk: " + msg);
            }
        });
//        // 允许过期IP以实现懒加载策略
//        httpdns.setExpiredIPEnabled(true);

        // ipv6
        httpdns.enableIPv6(true);
        httpdns.setCachedIPEnabled(true);

        List<IPProbeItem> list = new ArrayList<IPProbeItem>();
        list.add(new IPProbeItem(TAOBAO_URL, 80));
        list.add(new IPProbeItem(APPLE_URL, 80));
        list.add(new IPProbeItem(DOUBAN_URL, 80));
        httpdns.setIPProbeList(list);
    }

    private void initHandler() {
        this.mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                int what = msg.what;
                switch (what) {
                    case MESSAGE_NORMAL_KEY:
                        String obj = (String) msg.obj;
                        tvConsole.setText(obj);
                        break;
                }
            }
        };
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btnNormalParse:
                getIp();
                break;
            case R.id.btnHttpsParse:
                httpsParse();
                break;
            case R.id.btnTimeout:
                timeoutConfig();
                break;
            case R.id.btnDegrationFilter:
                setDegrationFilter();
                break;
            case R.id.btnRequestTaobao:
                setTargetUrl(TAOBAO_URL);
                break;
            case R.id.btnRequestDouban:
                setTargetUrl(DOUBAN_URL);
                break;
            case R.id.btnRequestApple:
                setTargetUrl(APPLE_URL);
                break;
            case R.id.btnRequestIpv6:
                setTargetUrl(IPV6_HOST);
                break;
            case R.id.btnPreResove:
                setPreResolveHost();
                break;
            case R.id.btnSetExpired:
                setExpired();
                break;
            case R.id.btnSetCache:
                setCache();
                break;
            case R.id.btnIpv6Parse:
                getIpv6();
                break;
        }
    }

    private void sendRunnable(Runnable runnable) {
        if (this.pool != null && runnable != null) {
            this.pool.execute(runnable);
        }
    }

    private void setTargetUrl(String host) {
        this.TARGET_URL = host;
    }

    private void getIp() {
        httpdns.setHTTPSRequestEnabled(false);
        this.sendRunnable(new Runnable() {
            @Override
            public void run() {
                // 解析IP
                long start = System.currentTimeMillis();
                String ip = httpdns.getIpByHostAsync(TARGET_URL);
                long end = System.currentTimeMillis();
                Log.d("tt", "cost time: " + (end - start));
                Message msg = mHandler.obtainMessage();
                msg.what = MESSAGE_NORMAL_KEY;
                String result = "Get IP for host:" + TARGET_URL;
                if (ip != null) {
                    msg.obj = result + " success. ip:" + ip;
                } else {
                    msg.obj = result + " failed.";
                }
                mHandler.sendMessage(msg);
            }
        });
    }

    private void getIpv6() {
        this.sendRunnable(new Runnable() {
            @Override
            public void run() {
                // 解析IP
                String ip = httpdns.getIPv6ByHostAsync(TARGET_URL);
                Message msg = mHandler.obtainMessage();
                msg.what = MESSAGE_NORMAL_KEY;
                String result = "Get IPV6 for host:" + TARGET_URL;
                if (ip != null) {
                    msg.obj = result + " success. ipv6:" + ip;
                } else {
                    msg.obj = result + " failed.";
                }
                mHandler.sendMessage(msg);
            }
        });
    }

    private void httpsParse() {
        httpdns.setHTTPSRequestEnabled(true);

        this.sendRunnable(new Runnable() {
            @Override
            public void run() {
                // 解析IP
                long start = System.currentTimeMillis();
                String ip = httpdns.getIpByHostAsync(TARGET_URL);
                long end = System.currentTimeMillis();
                Log.d("tt", "cost time: " + (end - start));
                Message msg = mHandler.obtainMessage();
                msg.what = MESSAGE_NORMAL_KEY;
                String result = "Get IP for host:" + TARGET_URL;
                if (ip != null) {
                    msg.obj = result + " success. ip:" + ip;
                } else {
                    msg.obj = result + " failed.";
                }
                mHandler.sendMessage(msg);
            }
        });
    }

    private void timeoutConfig() {
        httpdns.setTimeoutInterval(10);
    }

    private void setDegrationFilter() {
        httpdns.setDegradationFilter(new DegradationFilter() {
            @Override
            public boolean shouldDegradeHttpDNS(String hostName) {
                if (hostName.equals(TAOBAO_URL)) {
                    return true;
                } else {
                    return false;
                }
            }
        });
    }

    private void setPreResolveHost() {
        httpdns.setPreResolveHosts(new ArrayList<>(Arrays.asList(TAOBAO_URL, APPLE_URL, DOUBAN_URL)));
    }

    private void setExpired() {
        httpdns.setExpiredIPEnabled(true);
    }

    private void setCache() {
        httpdns.setCachedIPEnabled(true);
    }

}
