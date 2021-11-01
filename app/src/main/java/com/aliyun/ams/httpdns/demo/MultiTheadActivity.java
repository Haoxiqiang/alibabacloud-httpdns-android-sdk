package com.aliyun.ams.httpdns.demo;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.alibaba.sdk.android.httpdns.HTTPDNSResult;
import com.alibaba.sdk.android.httpdns.HttpDnsService;
import com.alibaba.sdk.android.httpdns.RequestIpType;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author zonglin.nzl
 * @date 11/1/21
 */
public class MultiTheadActivity extends Activity {

    public static final String TAG = "MultiTheadActivity";

    private String[] validHosts = new String[]{
            "www.aliyun.com",
            "www.taobao.com",
            "www.google.com",
            "api.vpgame.com",
            "yqh.aliyun.com",
            "www.test1.com",
            "ib.snssdk.com",
            "www.baidu.com",
            "www.dddzzz.com",
            "isub.snssdk.com",
            "www.facebook.com",
            "m.vpgame.com",
            "img.momocdn.com",
            "ipv6.l.google.com",
            "ipv6.sjtu.edu.cn",
            "fdfs.xmcdn.com",
            "testcenter.dacangjp.com",
            "mobilegw.alipaydns.com",
            "ww1.sinaimg.cn.w.alikunlun.com",
            "x2ipv6.tcdn.qq.com",
            "hiphotos.jomodns.com",
            "suning.xdwscache.ourwebcdn.com",
            "gw.alicdn.com",
            "dou.bz",
            "www.apple.com",
    };
    StringBuilder sb = new StringBuilder();
    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multi_thread);

        findViewById(R.id.btnExecute).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                execute();
            }
        });
    }

    private void postLog(String msg) {
        Log.w(TAG, msg);
        sb.append(msg).append("\n");
        handler.post(new Runnable() {
            @Override
            public void run() {
                TextView logView = findViewById(R.id.tvLog);
                logView.setText(sb.toString());
            }
        });
    }

    private int getNum(int resId, int defaultValue) {
        EditText et = findViewById(resId);
        if (et == null) {
            return defaultValue;
        }
        String str = et.getEditableText().toString();
        int i = Integer.parseInt(str);
        if (i <= 0) {
            return defaultValue;
        }
        return i;
    }

    private int getThreadCount() {
        return getNum(R.id.etThreadCount, 10);
    }

    private int getExecuteTimeInMinute() {
        return getNum(R.id.etExecuteTime, 10);
    }

    private int getAllHostCount() {
        return getNum(R.id.etAllHostCount, 100);
    }

    private int getValidHostCount() {
        return getNum(R.id.etValidHostCount, 30);
    }

    private int getTimeoutInSecond() {
        return getNum(R.id.etTimeout, 10);
    }


    private void execute() {
        final int threadCount = getThreadCount();
        final int executeTime = getExecuteTimeInMinute() * 60 * 1000;
        final int hostCount = getAllHostCount();
        int validCount = getValidHostCount();
        if (validCount > hostCount) {
            validCount = hostCount;
        }
        if (validCount > validHosts.length) {
            validCount = validHosts.length;
        }
        final int timeout = getTimeoutInSecond() * 1000;
        final int tmpValidCount = validCount;
        final HttpDnsService httpdns = MainActivity.httpdns;
        postLog(threadCount + "线程并发，执行" + executeTime + ", 总域名" + hostCount + "个，有效" + validCount + "个，超时时间为" + timeout);
        new Thread(new Runnable() {
            @Override
            public void run() {

                httpdns.setTimeoutInterval(timeout);

                final ArrayList<String> hosts = new ArrayList<>(hostCount);
                for (int i = 0; i < hostCount - tmpValidCount; i++) {
                    hosts.add("test" + i + ".aliyun.com");
                }
                for (int i = 0; i < tmpValidCount; i++) {
                    hosts.add(validHosts[i]);
                }

                final CountDownLatch testLatch = new CountDownLatch(threadCount);
                ExecutorService service = Executors.newFixedThreadPool(threadCount);
                final CountDownLatch countDownLatch = new CountDownLatch(threadCount);
                for (int i = 0; i < threadCount; i++) {
                    service.execute(new Runnable() {
                        @Override
                        public void run() {
                            countDownLatch.countDown();
                            try {
                                countDownLatch.await();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            postLog(Thread.currentThread().getId() + " begin");
                            Random random = new Random(Thread.currentThread().getId());
                            int all = 0;
                            int slow = 0;
                            int nill = 0;
                            int empty = 0;
                            long maxSlow = 0;
                            long subSlow = 0;
                            long begin = System.currentTimeMillis();
                            while (System.currentTimeMillis() - begin < executeTime) {
                                String host = hosts.get(random.nextInt(hostCount));
                                long start = System.currentTimeMillis();
                                HTTPDNSResult ips = httpdns.getIpsByHostAsync(host, RequestIpType.v4, null, null);
                                long end = System.currentTimeMillis();
                                if (end - start > 100) {
                                    slow++;
                                    subSlow += end - start;
                                    if (maxSlow < end - start) {
                                        maxSlow = end - start;
                                    }
                                }
                                if (ips == null) {
                                    nill++;
                                } else if (ips.getIps() == 0) {
                                    empty++;
                                }
                                all++;
                            }

                            postLog(Thread.currentThread().getId() + " all: " + all + ", slow: " + slow + ", null: " + nill + ", empty: " + empty + ", max : " + maxSlow + ", avgMax: " + (slow == 0 ? 0 : subSlow / slow));
                            testLatch.countDown();
                        }
                    });
                }

                try {
                    testLatch.await();
                } catch (InterruptedException e) {
                }
                postLog(Thread.currentThread().getId() + " done ");
            }
        }).start();
    }
}
