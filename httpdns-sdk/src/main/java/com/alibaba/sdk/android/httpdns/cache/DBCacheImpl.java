package com.alibaba.sdk.android.httpdns.cache;

import android.content.Context;

import java.util.List;

/**
 * Created by tomchen on 2017/4/26.
 */

class DBCacheImpl implements IDBCache {

    private final DBHelper mDB;

    DBCacheImpl(Context context) {
        mDB = new DBHelper(context);
    }

    @Override
    public HostRecord load(String sp, String host) {
        return mDB.getHostRecord(sp, host);
    }

    @Override
    public List<HostRecord> loadAll() {
        return mDB.getAllHostRecords();
    }

    @Override
    public void store(HostRecord record) {
        mDB.add(record);
    }

    @Override
    public void clean(HostRecord record) {
        mDB.remove(record.sp, record.host);
    }
}
