package com.alibaba.sdk.android.httpdns;

import android.content.Context;
import android.net.TrafficStats;
import android.os.Build;

import com.alibaba.sdk.android.httpdns.net.SpStatusManager;
import com.alibaba.sdk.android.httpdns.net64.Net64Mgr;
import com.alibaba.sdk.android.httpdns.track.SessionTrackMgr;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

class QueryHostTask implements Callable<String[]> {
    private String hostName;
    private int retryTimes = HttpDnsConfig.RESOLVE_RETRY_TIMES;
    private static HostManager hostManager = HostManager.getInstance();
    private static Context context;
    private String serverIp = null;
    private String[] result = HttpDnsConfig.NO_IPS;
    private QueryType queryType;
    private static final Object LOCK = new Object();
    private boolean isConcurrentChecked = false;
    private String cache_name = null;
    private Map<String, String> extra = new HashMap<>();

    // task启动时间
    private long mTaskStartTime = 0L;

    QueryHostTask(String hostName, QueryType type) {
        this.hostName = hostName;
        this.queryType = type;
    }

    QueryHostTask(String hostName, QueryType type, Map<String, String> extra, String cache_name) {
        this.hostName = hostName;
        this.queryType = type;
        this.cache_name = cache_name;
        this.extra.putAll(extra);//putAll可以合并两个MAP，如果有相同的key那么用后面的覆盖前面的
    }


    static void setContext(Context context) {
        QueryHostTask.context = context;
    }

    @Override
    public String[] call() {
        HttpURLConnection conn = null;
        InputStream in = null;
        BufferedReader streamReader = null;
        mTaskStartTime = System.currentTimeMillis();

        if (!isConcurrentChecked) {
            synchronized (LOCK) {
                if (hostManager.isResolving(hostName)) {
                    HttpDnsLog.Logd("host:" + hostName + " is already resolving");
                    return this.result;
                } else {
                    hostManager.setHostResolving(hostName);
                    // 置位标志量，保证重试时不会因为同步检测而放弃
                    isConcurrentChecked = true;
                }
            }
        }

        try {
            if (Build.VERSION.SDK_INT >= 14) {
                TrafficStats.setThreadStatsTag(0xA005);
            }
            this.serverIp = StatusManager.getServerIp(this.queryType);
            if (this.serverIp == null) {
                HttpDnsLog.Logd("serverIp is null, give up query for hostname:" + hostName);
            } else {

                String resolveUrl;
                SpStatusManager.getInstance().update(context);
                if (AuthManager.isEnable()) {
                    final String timestamp = AuthManager.getTimestamp();
                    if (SpStatusManager.getInstance().isWifi()) {
                        resolveUrl = HttpDnsConfig.PROTOCOL + this.serverIp + ":" + HttpDnsConfig.SERVER_PORT + "/" + HttpDnsConfig.accountID + "/sign_d?host="
                                + hostName + "&sdk=android_" + BuildConfig.VERSION_NAME + "&t=" + timestamp + "&s=" + AuthManager.getSign(hostName, timestamp)
                                + "&sid=" + SessionTrackMgr.getInstance().getSessionId() + "&net=" + SessionTrackMgr.getInstance().getNetType() + "&bssid=" + URLEncoder.encode(SessionTrackMgr.getInstance().getBssid(), "UTF-8")
                                + getExtra();
                    } else {
                        resolveUrl = HttpDnsConfig.PROTOCOL + this.serverIp + ":" + HttpDnsConfig.SERVER_PORT + "/" + HttpDnsConfig.accountID + "/sign_d?host="
                                + hostName + "&sdk=android_" + BuildConfig.VERSION_NAME + "&t=" + timestamp + "&s=" + AuthManager.getSign(hostName, timestamp)
                                + "&sid=" + SessionTrackMgr.getInstance().getSessionId() + "&net=" + SessionTrackMgr.getInstance().getNetType()
                                + getExtra();
                    }
                } else {
                    if (SpStatusManager.getInstance().isWifi()) {
                        resolveUrl = HttpDnsConfig.PROTOCOL + this.serverIp + ":" + HttpDnsConfig.SERVER_PORT + "/" + HttpDnsConfig.accountID + "/d?host="
                                + hostName + "&sdk=android_" + BuildConfig.VERSION_NAME
                                + "&sid=" + SessionTrackMgr.getInstance().getSessionId() + "&net=" + SessionTrackMgr.getInstance().getNetType() + "&bssid=" + URLEncoder.encode(SessionTrackMgr.getInstance().getBssid(), "UTF-8")
                                + getExtra();
                    } else {
                        resolveUrl = HttpDnsConfig.PROTOCOL + this.serverIp + ":" + HttpDnsConfig.SERVER_PORT + "/" + HttpDnsConfig.accountID + "/d?host="
                                + hostName + "&sdk=android_" + BuildConfig.VERSION_NAME
                                + "&sid=" + SessionTrackMgr.getInstance().getSessionId() + "&net=" + SessionTrackMgr.getInstance().getNetType()
                                + getExtra();
                    }
                }

                if (Net64Mgr.getInstance().isEnable()) {
                    resolveUrl += "&query=4,6";
                }

                HttpDnsLog.Loge("resolve url: " + resolveUrl);

                conn = (HttpURLConnection) new URL(resolveUrl).openConnection();
                conn.setConnectTimeout(HttpDnsConfig.CUSTOMIZED_RESOLVE_TIMEOUT_IN_MESC);
                conn.setReadTimeout(HttpDnsConfig.CUSTOMIZED_RESOLVE_TIMEOUT_IN_MESC);
                if (conn instanceof HttpsURLConnection) {
                    ((HttpsURLConnection) conn).setHostnameVerifier(new HostnameVerifier() {
                        public boolean verify(String hostname, SSLSession session) {
                            HttpDnsLog.Logd("Https request, set hostnameVerifier");
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

                    HttpDnsLog.Loge("response code is " + conn.getResponseCode() + " expect 200. response body is " + sb.toString());
                    HttpDnsErrorResponse errorResponse = new HttpDnsErrorResponse(conn.getResponseCode(), sb.toString());
                    throw new HttpDnsException(conn.getResponseCode(), errorResponse.getErrorMsg());
                } else {

                    in = conn.getInputStream();
                    streamReader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = streamReader.readLine()) != null) {
                        sb.append(line);
                    }

                    HttpDnsLog.Logd("resolve host: " + hostName + ", return: " + sb.toString());
                    HostObject hostObject = new HostObject(sb.toString());
                    hostObject.setCacheKey(cache_name);
                    if (hostManager.count() < HttpDnsConfig.MAX_HOST_OBJECT) {
                        hostManager.put(hostName, hostObject);
                    } else {
                        throw new Exception("the total number of hosts is exceed " + HttpDnsConfig.MAX_HOST_OBJECT);
                    }

                    StatusManager.httpDnsRequestSuccess(this.hostName, this.serverIp, System.currentTimeMillis() - mTaskStartTime);
                    hostManager.setHostResolved(hostName);
                    this.result = hostObject.getIps();
                    this.extra = hostObject.getExtra();
                }
            }
        } catch (Throwable e) {
            HttpDnsLog.printStackTrace(e);
            // 上报请求错误
            StatusManager.httpDnsRequestFailed(this.hostName, this.serverIp, e);
            if (this.retryTimes > 0) {
                this.retryTimes--;
                this.call();
            } else {
                // 重试全部失败
                StatusManager.reportHttpDnsSuccess(this.hostName, 0);
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
        hostManager.setHostResolved(hostName);
        return this.result;
    }

    private String getExtra() throws UnsupportedEncodingException {
        StringBuilder sb = new StringBuilder();
        boolean isKey = true;
        boolean isValue = true;
        if (this.extra != null) {
            Iterator<Map.Entry<String, String>> iterator = this.extra.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, String> entry = iterator.next();
                sb.append("&sdns-");
                sb.append(entry.getKey());
                sb.append("=");
                sb.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
                if (!checkKey(entry.getKey())) {
                    isKey = false;
                    HttpDnsLog.Loge("设置自定义参数失败，自定义key不合法：" + entry.getKey());
                }
                if (!checkValue(entry.getValue())) {
                    isValue = false;
                    HttpDnsLog.Loge("设置自定义参数失败，自定义value不合法：" + entry.getValue());
                }
            }
        }
        if (isKey && isValue) {
            String extra = sb.toString();
            if (extra.getBytes("UTF-8").length <= 1000) {
                return extra;
            } else {
                HttpDnsLog.Loge("设置自定义参数失败，自定义参数过长");
                return "";
            }
        } else {
            return "";
        }
    }

    private boolean checkKey(String s) {
        return s.matches("[a-zA-Z0-9\\-_]+");
    }

    private boolean checkValue(String s) {
        return s.matches("[a-zA-Z0-9\\-_=]+");
    }

    public void setRetryTimes(int times) {
        if (times >= 0) {
            this.retryTimes = times;
        }
    }
}
