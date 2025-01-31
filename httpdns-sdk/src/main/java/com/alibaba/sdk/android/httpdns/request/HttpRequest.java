package com.alibaba.sdk.android.httpdns.request;

import com.alibaba.sdk.android.httpdns.log.HttpDnsLog;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

/**
 * 网络请求
 *
 * @author zonglin.nzl
 * @date 2020/10/20
 */
public class HttpRequest<T> {
    private HttpRequestConfig requestConfig;
    private ResponseTranslator<T> translator;

    protected HttpRequest() {
    }

    public HttpRequest(HttpRequestConfig requestConfig, ResponseTranslator<T> translator) {
        this.requestConfig = requestConfig;
        this.translator = translator;
    }

    /**
     * 获取本次请求的配置
     *
     * @return
     */
    public HttpRequestConfig getRequestConfig() {
        return requestConfig;
    }

    /**
     * 发起请求
     *
     * @return 返回的数据
     * @throws Throwable 发生的错误
     */
    public T request() throws Throwable {
        HttpURLConnection conn = null;
        InputStream in = null;
        BufferedReader streamReader = null;

        long start = System.currentTimeMillis();
        String url = requestConfig.url();
        if (HttpDnsLog.isPrint()) {
            HttpDnsLog.d("request url " + url);
        }
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setReadTimeout(requestConfig.getTimeout());
            conn.setConnectTimeout(requestConfig.getTimeout());
            if (conn instanceof HttpsURLConnection) {
                ((HttpsURLConnection) conn).setHostnameVerifier(new HostnameVerifier() {
                    @Override
                    public boolean verify(String hostname, SSLSession session) {
                        return HttpsURLConnection.getDefaultHostnameVerifier().verify(HttpRequestConfig.HTTPS_CERTIFICATE_HOSTNAME, session);
                    }
                });
            }
            if (conn.getResponseCode() != 200) {
                in = conn.getErrorStream();
                streamReader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
                String errStr = readStringFrom(streamReader).toString();
                throw HttpException.create(conn.getResponseCode(), errStr);
            } else {
                in = conn.getInputStream();
                streamReader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
                String responseStr = readStringFrom(streamReader).toString();
                if (HttpDnsLog.isPrint()) {
                    HttpDnsLog.d("request success " + responseStr);
                }
                return translator.translate(responseStr);
            }
        } catch (Throwable e) {
            long cost = System.currentTimeMillis() - start;
            HttpDnsLog.w("request " + url + " fail, cost " + cost, e);
            throw e;
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
            }
        }
    }

    /**
     * stream to string
     *
     * @param streamReader
     * @return
     * @throws IOException
     */
    public static StringBuilder readStringFrom(BufferedReader streamReader) throws IOException {
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = streamReader.readLine()) != null) {
            sb.append(line);
        }
        return sb;
    }
}
