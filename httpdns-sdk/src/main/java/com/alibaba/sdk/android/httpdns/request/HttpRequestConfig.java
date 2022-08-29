package com.alibaba.sdk.android.httpdns.request;

import com.alibaba.sdk.android.httpdns.RequestIpType;
import com.alibaba.sdk.android.httpdns.utils.Constants;

/**
 * 网络请求的配置
 *
 * @author zonglin.nzl
 * @date 2020/12/3
 */
public class HttpRequestConfig {
    public static final String HTTP_SCHEMA = "http://";
    public static final String HTTPS_SCHEMA = "https://";
    public static final String HTTPS_CERTIFICATE_HOSTNAME = "203.107.1.1";
    /**
     * 请求协议
     */
    private String schema = HTTP_SCHEMA;
    /**
     * 服务器ip
     */
    private String ip;
    /**
     * 服务器端口
     */
    private int port;
    /**
     * 请求路径，包含query参数
     */
    private String path;
    /**
     * 请求超时时间
     */
    private int timeout = Constants.DEFAULT_TIMEOUT;

    /**
     * 服务器IP类型 只会是 v4 或者 v6
     */
    private RequestIpType ipType = RequestIpType.v4;

    public HttpRequestConfig(String ip, int port, String path) {
        this.ip = ip;
        this.port = port;
        this.path = path;
    }

    public HttpRequestConfig(String schema, String ip, int port, String path, int timeout, RequestIpType ipType) {
        this.schema = schema;
        this.ip = ip;
        this.port = port;
        this.path = path;
        this.timeout = timeout;
        this.ipType = ipType;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getSchema() {
        return this.schema;
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public int getTimeout() {
        return timeout;
    }

    public RequestIpType getIpType() {
        return ipType;
    }

    public String url() {
        if (ipType == RequestIpType.v6) {
            return schema + "[" + ip + "]" + ":" + port + path;
        } else {
            return schema + ip + ":" + port + path;
        }
    }
}
