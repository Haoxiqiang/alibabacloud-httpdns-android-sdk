package com.alibaba.sdk.android.httpdns;

import com.alibaba.sdk.android.httpdns.probe.IPProbeItem;
import com.alibaba.sdk.android.httpdns.utils.Constants;

import java.util.HashMap;
import java.util.List;

/**
 * 初始化配置
 * 之前的初始化方式，每个配置都是单独设置的，有可能造成一些时序上的问题
 * 所以增加统一初始化配置的方法，由内部固定初始化逻辑，避免时序问题
 *
 * @author zonglin.nzl
 * @date 1/14/22
 */
public class InitConfig {

    private static final HashMap<String, InitConfig> configs = new HashMap<>();

    private static void addConfig(String accountId, InitConfig config) {
        configs.put(accountId, config);
    }

    public static InitConfig getInitConfig(String accountId) {
        return configs.get(accountId);
    }

    public static void removeConfig(String accountId) {
        if (accountId == null || accountId.isEmpty()) {
            configs.clear();
        } else {
            configs.remove(accountId);
        }
    }


    private boolean enableExpiredIp;
    private boolean enableCacheIp;
    private int timeout;
    private boolean enableHttps;
    private List<IPProbeItem> ipProbeItems;
    private String region;

    private InitConfig(Builder builder) {
        enableExpiredIp = builder.enableExpiredIp;
        enableCacheIp = builder.enableCacheIp;
        timeout = builder.timeout;
        enableHttps = builder.enableHttps;
        ipProbeItems = builder.ipProbeItems;
        region = builder.region;
    }

    public boolean isEnableExpiredIp() {
        return enableExpiredIp;
    }

    public boolean isEnableCacheIp() {
        return enableCacheIp;
    }

    public int getTimeout() {
        return timeout;
    }

    public boolean isEnableHttps() {
        return enableHttps;
    }

    public List<IPProbeItem> getIpProbeItems() {
        return ipProbeItems;
    }

    public String getRegion() {
        return region;
    }

    public static class Builder {
        private boolean enableExpiredIp = Constants.DEFAULT_ENABLE_EXPIRE_IP;
        private boolean enableCacheIp = Constants.DEFAULT_ENABLE_CACHE_IP;
        private int timeout = Constants.DEFAULT_TIMEOUT;
        private boolean enableHttps = Constants.DEFAULT_ENABLE_HTTPS;
        private List<IPProbeItem> ipProbeItems = null;
        private String region = Constants.REGION_DEFAULT;

        public Builder setEnableExpiredIp(boolean enableExpiredIp) {
            this.enableExpiredIp = enableExpiredIp;
            return this;
        }

        public Builder setEnableCacheIp(boolean enableCacheIp) {
            this.enableCacheIp = enableCacheIp;
            return this;
        }

        public Builder setTimeout(int timeout) {
            this.timeout = timeout;
            return this;
        }

        public Builder setEnableHttps(boolean enableHttps) {
            this.enableHttps = enableHttps;
            return this;
        }

        public Builder setIpProbeItems(List<IPProbeItem> ipProbeItems) {
            this.ipProbeItems = ipProbeItems;
            return this;
        }

        public Builder setRegion(String region) {
            this.region = region;
            return this;
        }

        public InitConfig buildFor(String accountId) {
            InitConfig config = new InitConfig(this);
            addConfig(accountId, config);
            return config;
        }
    }
}
