package com.alibaba.sdk.android.httpdns.serverip;

import com.alibaba.sdk.android.httpdns.utils.CommonUtil;

import java.util.HashMap;

/**
 * 服务ip数据仓库
 * 实现有效期逻辑，有效期内返回值，有效期外返回空
 * @author zonglin.nzl
 * @date 2020/12/4
 */
public class ServerIpRepo {

    private int interval = 5 * 60 * 1000;
    private HashMap<String, ServerIpData> cache = new HashMap<>();

    public String[] getServerIps(String region) {
        region = CommonUtil.fixRegion(region);
        ServerIpData data = cache.get(region);
        if (data == null || data.getRequestTime() + interval < System.currentTimeMillis()) {
            return null;
        }
        return data.getServerIps();
    }

    public int[] getPorts(String region) {
        region = CommonUtil.fixRegion(region);
        ServerIpData data = cache.get(region);
        if (data == null || data.getRequestTime() + interval < System.currentTimeMillis()) {
            return null;
        }
        return data.getServerPorts();
    }

    public void save(String region, String[] serverIps, int[] ports) {
        region = CommonUtil.fixRegion(region);
        ServerIpData data = new ServerIpData(region, serverIps, ports);
        cache.put(region, data);
    }

    public void setTimeInterval(int timeInterval) {
        this.interval = timeInterval;
    }

}
