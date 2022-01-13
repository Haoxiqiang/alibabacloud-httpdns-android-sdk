package com.alibaba.sdk.android.httpdns.config;

import android.content.Context;
import android.content.SharedPreferences;

import com.alibaba.sdk.android.httpdns.impl.HttpDnsConfig;
import com.alibaba.sdk.android.httpdns.utils.Constants;

/**
 * 辅助配置的缓存写入和读取
 *
 * @author zonglin.nzl
 * @date 1/13/22
 */
public class ConfigCacheHelper {

    public void restoreFromCache(Context context, HttpDnsConfig config) {
        SharedPreferences sp = context.getSharedPreferences(Constants.CONFIG_CACHE_PREFIX + config.getAccountId(), Context.MODE_PRIVATE);
        SpCacheItem[] items = config.getCacheItem();
        for (int i = 0; i < items.length; i++) {
            items[i].restoreFromCache(sp);
        }
        config.setCacheHelper(this);
    }

    public void saveConfigToCache(Context context, HttpDnsConfig config) {
        SharedPreferences.Editor editor = context.getSharedPreferences(Constants.CONFIG_CACHE_PREFIX + config.getAccountId(), Context.MODE_PRIVATE).edit();
        SpCacheItem[] items = config.getCacheItem();
        for (int i = 0; i < items.length; i++) {
            items[i].saveToCache(editor);
        }
        // 虽然提示建议使用apply，但是实践证明，apply是把写文件操作推迟到了一些界面切换等时机，反而影响了UI线程。不如直接在子线程写文件
        editor.commit();
    }
}
