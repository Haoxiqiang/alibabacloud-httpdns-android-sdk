package com.alibaba.sdk.android.httpdns.cache;

/**
 * Created by tomchen on 2017/4/26.
 */

final class DBConstants {

    //测试升级时数据是否正常存储
    static final String DB_NAME = "aliclound_httpdns.db";

    static final int DB_VERSION = 0x01;

    static class HOST {

        static final String TABLE_NAME = "host";

        static final String COL_ID = "id";

        static final String COL_HOST = "host";

        static final String COL_SP = "sp";

        static final String COL_TIME = "time";

        static final String COL_EXTRA = "extra";

        static final String COL_CACHE_KEY = "cache_key";

        static final String CREATE_HOST_TABLE_SQL = "CREATE TABLE " + TABLE_NAME + " ("
                + COL_ID + " INTEGER PRIMARY KEY,"
                + COL_HOST + " TEXT,"
                + COL_SP + " TEXT,"
                + COL_TIME + " TEXT,"
                + COL_EXTRA + " TEXT,"
                + COL_CACHE_KEY + " TEXT"
                + ");";
    }

    static class IP {

        static final String TABLE_NAME = "ip";

        static final String COL_ID = "id";

        static final String COL_HOST_ID = "host_id";

        static final String COL_IP = "ip";

        static final String COL_TTL = "ttl";

        static final String CREATE_IP_TABLE_SQL = "CREATE TABLE " + TABLE_NAME + " ("
                + COL_ID + " INTEGER PRIMARY KEY,"
                + COL_HOST_ID + " INTEGER,"
                + COL_IP + " TEXT,"
                + COL_TTL + " TEXT"
                + ");";
    }

    static class IPV6 {

        static final String TABLE_NAME = "ipv6";

        static final String COL_ID = "id";

        static final String COL_HOST_ID = "host_id";

        static final String COL_IP = "ip";

        static final String COL_TTL = "ttl";

        static final String CREATE_IPV6_TABLE_SQL = "CREATE TABLE " + TABLE_NAME + " ("
                + COL_ID + " INTEGER PRIMARY KEY,"
                + COL_HOST_ID + " INTEGER,"
                + COL_IP + " TEXT,"
                + COL_TTL + " TEXT"
                + ");";
    }

}
