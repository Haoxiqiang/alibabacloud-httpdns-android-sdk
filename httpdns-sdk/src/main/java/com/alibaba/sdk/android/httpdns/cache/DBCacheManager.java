package com.alibaba.sdk.android.httpdns.cache;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.alibaba.sdk.android.httpdns.net.SpStatusManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by tomchen on 2017/4/26.
 */

public class DBCacheManager {

    private static final String TAG = DBConfig.TAG;

    private static final boolean DEBUG = DBConfig.DEBUG;

    private static boolean isEnable = false;
    private static boolean isAutoCleanCacheAfterLoad = true;

    private static SpStatusManager sSpMgr;

    private static IDBCache sDB_IMPL;

    public static void init(Context context) {
        init(context, null);
    }

    public static void init(Context context, SpStatusManager spMgr) {
        sDB_IMPL = new DBCacheImpl(context);
        sSpMgr = spMgr;
        if (sSpMgr == null) {
            sSpMgr = SpStatusManager.getInstance();
        }
    }

    public static void enable(boolean enable, boolean autoCleanCacheAfterLoad) {
        isEnable = enable;
        isAutoCleanCacheAfterLoad = autoCleanCacheAfterLoad;
    }

    public static boolean isEnable() {
        return isEnable;
    }

    public static boolean isAutoCleanCacheAfterLoad() {
        return isAutoCleanCacheAfterLoad;
    }

    /**
     * 查询host
     *
     * @param host 域名
     * @return @{@link HostRecord}
     */
    public static HostRecord load(String host) {
        if (!isEnable || TextUtils.isEmpty(host)) {
            return null;
        }

        final HostRecord h = sDB_IMPL.load(sSpMgr.getSP(), host);

        if (DEBUG) {
            Log.d(TAG, "[DBCacheManager] load: " + h);
        }

        return h;
    }

    public static List<HostRecord> loadAll() {
        final ArrayList<HostRecord> hosts = new ArrayList<>();

        if (!isEnable) {
            return hosts;
        }

        hosts.addAll(sDB_IMPL.loadAll());

        if (DEBUG) {
            Log.d(TAG, "[DBCacheManager] loadAll size: " + hosts.size());
            Log.d(TAG, "[DBCacheManager] loadAll: " + hosts);
        }

        return hosts;
    }

    /**
     * 更新host
     *
     * @param host @{@link HostRecord}
     */
    public static void store(HostRecord host) {
        if (host == null) {
            return;
        }

        if (DEBUG) {
            Log.d(TAG, "[DBCacheManager] store: " + host);
        }

        sDB_IMPL.store(host);
    }

    /**
     * 清除host
     *
     * @param host
     */
    public static void clean(HostRecord host) {
        if (host == null) {
            return;
        }

        if (DEBUG) {
            Log.d(TAG, "[DBCacheManager] clean: " + host);
        }

        sDB_IMPL.clean(host);
    }

    public static void updateSp(Context context) {
        if (context != null) {
            sSpMgr.update(context);
        }
    }

    public static String getSP() {
        return sSpMgr.getSP();
    }
}
