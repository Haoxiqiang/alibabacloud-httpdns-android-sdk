package com.alibaba.sdk.android.httpdns.probe;

/**
 * IP探测服务接口
 * Created by liyazhou on 2017/12/14.
 */
public interface IPProbeService {

    /**
     * 设置IP更新回调
     *
     * @param callback
     */
    void setIPListUpdateCallback(IPListUpdateCallback callback);

    /**
     * 发起ip探测任务
     *
     * @param hostname
     * @param ips
     */
    void launchIPProbeTask(String hostname, int port, String[] ips);


    /**
     * 获取探测状态
     *
     * @param hostname
     * @return
     */
    IPProbeStatus getProbeStatus(String hostname);

    /**
     * 停止一个IP探测任务
     *
     * @param hostname
     * @return
     */
    boolean stopIPProbeTask(String hostname);

    /**
     * 探测状态：
     * NO_PROBING: 当前未探测
     * PROBING: 当前域名正在进行探测
     */
    enum IPProbeStatus {
        NO_PROBING,
        PROBING
    }
}
