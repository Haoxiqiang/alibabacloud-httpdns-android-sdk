package com.alibaba.sdk.android.httpdns.request;

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
    public static final int DEFAULT_TIMEOUT = 15 * 1000;
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
    private int timeout = DEFAULT_TIMEOUT;

    public HttpRequestConfig(String ip, int port, String path) {
        this.ip = ip;
        this.port = port;
        this.path = path;
    }

    public HttpRequestConfig(String schema, String ip, int port, String path, int timeout) {
        this.schema = schema;
        this.ip = ip;
        this.port = port;
        this.path = path;
        this.timeout = timeout;
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

    public String url() {
        return schema + ip + ":" + port + path;
    }
}
