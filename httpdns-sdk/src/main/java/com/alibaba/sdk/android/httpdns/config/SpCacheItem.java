package com.alibaba.sdk.android.httpdns.config;

import android.content.SharedPreferences;

/**
 * @author zonglin.nzl
 * @date 1/13/22
 */
public interface SpCacheItem {
    void restoreFromCache(SharedPreferences sp);

    void saveToCache(SharedPreferences.Editor editor);
}
