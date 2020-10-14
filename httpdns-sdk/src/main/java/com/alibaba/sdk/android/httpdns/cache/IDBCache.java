package com.alibaba.sdk.android.httpdns.cache;

import java.util.List;

/**
 * Created by tomchen on 2017/4/26.
 */

interface IDBCache {

    HostRecord load(String sp, String host);

    List<HostRecord> loadAll();

    void store(HostRecord record);

    void clean(HostRecord record);
}
