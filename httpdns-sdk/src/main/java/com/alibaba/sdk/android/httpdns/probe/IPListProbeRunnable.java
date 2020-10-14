package com.alibaba.sdk.android.httpdns.probe;

import com.alibaba.sdk.android.httpdns.GlobalDispatcher;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * IP列表探测runnable
 * Created by liyazhou on 2017/12/14.
 */

class IPListProbeRunnable implements Runnable {
    private long taskNumber;
    private String host;
    private String[] ips;
    private int port;
    private ConcurrentHashMap<String, Long> map = new ConcurrentHashMap<>();
    private IPProbeTaskCallback callback = null;

    public IPListProbeRunnable(long taskNumber, String host, String[] ips, int port, IPProbeTaskCallback callback) {
        this.taskNumber = taskNumber;
        this.host = host;
        this.ips = ips;
        this.port = port;
        this.callback = callback;
    }

    @Override
    public void run() {
        if (this.ips != null && this.ips.length != 0) {
            CountDownLatch countDownLatch = new CountDownLatch(ips.length);
            for (int i = 0; i < ips.length; i++) {
                GlobalDispatcher.getDispatcher().execute(new IPProbeTaskRunnable(ips[i], port, countDownLatch, map));
            }

            try {
                countDownLatch.await(ProbeConfig.PROBE_TOTAL_TIME_OUT, TimeUnit.MILLISECONDS);

                if (this.callback != null) {
                    String[] result = resultSort(map);
                    if (result != null && result.length != 0) {
                        this.callback.onIPProbeTaskFinished(taskNumber, this.buildOptimizeItem(result));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private String[] resultSort(ConcurrentHashMap<String, Long> map) {
        if (map == null) {
            return null;
        }

        String[] result = new String[map.size()];
        int i = 0;
        for (String key : map.keySet()) {
            result[i] = new String(key);
            i++;
        }

        // 排序
        for (i = 0; i < result.length - 1; i++) {
            for (int j = 0; j < result.length - i - 1; j++) {
                if (map.get(result[j]) > map.get(result[j + 1])) {
                    String temp = result[j];
                    result[j] = result[j + 1];
                    result[j + 1] = temp;
                }
            }
        }

        return result;

    }

    private IPProbeOptimizeItem buildOptimizeItem(String[] result) {
        if (this.ips != null && this.ips.length != 0 &&
                result != null && result.length != 0) {
            String defaultIp = this.ips[0];
            String selectedIp = result[0];
            long defaultTimeCost = this.map.containsKey(defaultIp) ? this.map.get(defaultIp) : Integer.MAX_VALUE;
            long selectedTimeCost = this.map.containsKey(selectedIp) ? this.map.get(selectedIp) : Integer.MAX_VALUE;
            return new IPProbeOptimizeItem(this.host, result, defaultIp, selectedIp, defaultTimeCost, selectedTimeCost);
        }
        return null;
    }

}
