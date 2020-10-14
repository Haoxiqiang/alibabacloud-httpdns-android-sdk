package com.alibaba.sdk.android.httpdns;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

public class HttpdnsQueryServerIpTask implements Callable<String[]> {
    private static HttpdnsQueryServerIpTask task;
    private int retryTimes;

    // task启动时间
    private long mTaskStartTime = 0L;

    public static HttpdnsQueryServerIpTask getInstance() {
        if (task == null) {
            task = new HttpdnsQueryServerIpTask();
        }
        return task;
    }

    synchronized public String[] call() {
        HttpURLConnection conn = null;
        InputStream in = null;
        BufferedReader streamReader = null;
        mTaskStartTime = System.currentTimeMillis();
        try {
            final String url = HttpdnsScheduleCenter.getInstance().getScheduleCenterUrl();
            if (url != null) {
                HttpDnsLog.Logd("StartIp call start");
                conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setConnectTimeout(HttpdnsScheduleCenter.SCHEDULE_CENTER_REQUEST_TIMEOUT_INTERVAL);
                conn.setReadTimeout(HttpdnsScheduleCenter.SCHEDULE_CENTER_REQUEST_TIMEOUT_INTERVAL);

                if (conn instanceof HttpsURLConnection) {
                    ((HttpsURLConnection) conn).setHostnameVerifier(new HostnameVerifier() {
                        public boolean verify(String hostname, SSLSession session) {
                            HttpDnsLog.Logd("StartIp Https request, set hostnameVerifier." + " StartIp url：" + url);
                            return HttpsURLConnection.getDefaultHostnameVerifier().verify(HttpDnsConfig.HTTPS_CERTIFICATE_HOSTNAME, session);
                        }
                    });
                }

                if (conn.getResponseCode() != 200) {
                    in = conn.getErrorStream();
                    streamReader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = streamReader.readLine()) != null) {
                        sb.append(line);
                    }

                    HttpDnsLog.Loge("StartIp response code is " + conn.getResponseCode() + " expect 200. response body is " + sb.toString());
                    HttpDnsErrorResponse errorResponse = new HttpDnsErrorResponse(conn.getResponseCode(), sb.toString());

                    throw new HttpDnsException(errorResponse.getErrorCode(), errorResponse.getErrorMsg());
                } else {
                    in = conn.getInputStream();
                    streamReader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = streamReader.readLine()) != null) {
                        sb.append(line);
                    }

                    HttpdnsScheduleCenterResponse response = new HttpdnsScheduleCenterResponse(sb.toString());
                    HttpdnsScheduleCenter.getInstance().scheduleCenterRequestSuccess(response, System.currentTimeMillis() - mTaskStartTime);
                }
            }
        } catch (final Exception ex) {
            HttpDnsLog.printStackTrace(ex);
            HttpdnsScheduleCenter.getInstance().scheduleCenterRequestFailed(ex);
            if (retryTimes > 0) {
                retryTimes--;
                try {
                    HttpDnsScheduledExecutor.getScheduledExecutorService().schedule(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                if (!HttpdnsScheduleCenter.isUpdateServerIpsSuccess) {
                                    call();
                                }
                            } catch (Exception e) {
                                HttpDnsLog.printStackTrace(e);
                            }
                        }
                    }, 5 * 60 * 1000, TimeUnit.MILLISECONDS);
                } catch (Exception e) {
                    HttpDnsLog.printStackTrace(e);
                }
            }
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
            try {
                if (in != null) {
                    in.close();
                }
                if (streamReader != null) {
                    streamReader.close();
                }
            } catch (IOException e) {
                HttpDnsLog.printStackTrace(e);
            }
        }

        return new String[0];
    }

    public void setRetryTimes(int times) {
        this.retryTimes = times;
    }

}