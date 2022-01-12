package com.alibaba.sdk.android.httpdns.serverip;

import com.alibaba.sdk.android.httpdns.impl.HttpDnsConfig;
import com.alibaba.sdk.android.httpdns.log.HttpDnsLog;
import com.alibaba.sdk.android.httpdns.request.RequestCallback;
import com.alibaba.sdk.android.httpdns.utils.CommonUtil;

/**
 * 服务IP的调度服务
 *
 * @author zonglin.nzl
 * @date 2020/12/3
 */
public class ScheduleService {

    private HttpDnsConfig config;
    private OnServerIpUpdate onServerIpUpdate;
    private ServerIpRepo repo;

    public ScheduleService(HttpDnsConfig config, OnServerIpUpdate onServerIpUpdate) {
        this.config = config;
        this.onServerIpUpdate = onServerIpUpdate;
        this.repo = new ServerIpRepo();
    }

    /**
     * 修改region
     *
     * @param newRegion
     */
    public void updateServerIps(final String newRegion) {
        String[] serverIps = repo.getServerIps(newRegion);
        int[] ports = repo.getPorts(newRegion);
        if (serverIps != null) {
            updateServerConfig(newRegion, serverIps, ports);
            return;
        }

        UpdateServerTask.updateServer(config.copy(), newRegion, new RequestCallback<UpdateServerResponse>() {
            @Override
            public void onSuccess(UpdateServerResponse updateServerResponse) {
                if (!updateServerResponse.isEnable()) {
                    HttpDnsLog.i("disable service by server response " + updateServerResponse.toString());
                    config.setEnabled(false);
                    return;
                } else {
                    if (!config.isEnabled()) {
                        config.setEnabled(true);
                    }
                }
                if (updateServerResponse.getServerIps() != null) {
                    updateServerConfig(newRegion, updateServerResponse.getServerIps(), updateServerResponse.getServerPorts());
                    repo.save(newRegion, updateServerResponse.getServerIps(), updateServerResponse.getServerPorts());
                }
            }

            @Override
            public void onFail(Throwable throwable) {
                HttpDnsLog.w("update server ips fail", throwable);
            }
        });
    }

    private void updateServerConfig(String newRegion, String[] serverIps, int[] serverPorts) {
        boolean regionUpdated = !CommonUtil.regionEquals(this.config.getServerConfig().getCurrentServerRegion(), newRegion);
        boolean updated = config.getServerConfig().setServerIps(newRegion, serverIps, serverPorts);
        if (updated && onServerIpUpdate != null) {
            onServerIpUpdate.serverIpUpdated(regionUpdated);
        }
    }

    /**
     * 更新服务ip
     */
    public void updateServerIps() {
        updateServerIps(this.config.getRegion());
    }

    /**
     * 设置服务IP的更新间隔
     * @param timeInterval
     */
    public void setTimeInterval(int timeInterval) {
        this.repo.setTimeInterval(timeInterval);
    }

    /**
     * 服务ip更新时的回调接口
     */
    public interface OnServerIpUpdate {
        /**
         * 服务ip更新了
         */
        void serverIpUpdated(boolean regionUpdated);
    }
}
