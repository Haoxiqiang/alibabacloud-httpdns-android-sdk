package com.alibaba.sdk.android.httpdns.serverip;

import java.util.HashMap;

/**
 * @author zonglin.nzl
 * @date 1/17/22
 */
public class UpdateServerLocker {

    private HashMap<String, Long> cache = new HashMap<>();
    private int timeout;

    private Object lock = new Object();

    public UpdateServerLocker(int timeout) {
        this.timeout = timeout;
    }

    public UpdateServerLocker() {
        this.timeout = 5 * 60 * 1000;
    }

    public boolean begin(String region) {
        Long requestTime = cache.get(region);
        if (requestTime != null) {

            if (System.currentTimeMillis() - requestTime > timeout) {
                // 超过五分钟了，还没有end。
                // 主动end
                end(region);
            }
            return false;
        } else {
            synchronized (lock) {
                requestTime = cache.get(region);
                if (requestTime != null) {
                    return false;
                } else {
                    cache.put(region, System.currentTimeMillis());
                    return true;
                }
            }
        }
    }

    public void end(String region) {
        cache.remove(region);
    }
}
