package com.alibaba.sdk.android.httpdns.cache;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.alibaba.sdk.android.httpdns.log.HttpDnsLog;
import com.alibaba.sdk.android.httpdns.net.NetworkStateManager;
import com.alibaba.sdk.android.httpdns.utils.CommonUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * 数据库存取操作
 *
 * @author zonglin.nzl
 * @date 2020/12/11
 */
public class RecordDBHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "aliclound_httpdns_";
    private static final int DB_VERSION = 0x02;

    static class HOST {

        static final String TABLE_NAME = "host";

        static final String COL_ID = "id";

        static final String COL_REGION = "region";

        static final String COL_HOST = "host";

        static final String COL_IPS = "ips";

        static final String COL_TYPE = "type";

        static final String COL_TIME = "time";

        static final String COL_TTL = "ttl";

        static final String COL_EXTRA = "extra";

        static final String COL_CACHE_KEY = "cache_key";

        // 旧版本 用于存储网络标识的字段，由于合规的影响，删除了相关代码，此字段变为固定字段，目前已经没有意义
        static final String COL_SP = "sp";

        static final String CREATE_HOST_TABLE_SQL = "CREATE TABLE " + TABLE_NAME + " ("
                + COL_ID + " INTEGER PRIMARY KEY,"
                + COL_REGION + " TEXT,"
                + COL_HOST + " TEXT,"
                + COL_IPS + " TEXT,"
                + COL_TYPE + " INTEGER,"
                + COL_TIME + " INTEGER,"
                + COL_TTL + " INTEGER,"
                + COL_EXTRA + " TEXT,"
                + COL_CACHE_KEY + " TEXT,"
                + COL_SP + " TEXT"
                + ");";
    }

    private String accountId;
    private Object lock = new Object();
    private SQLiteDatabase db;

    public RecordDBHelper(Context context, String accountId) {
        super(context, DB_NAME + accountId + ".db", null, DB_VERSION);
        this.accountId = accountId;
    }

    private SQLiteDatabase getDB() {
        if (db == null) {
            try {
                db = getWritableDatabase();
            } catch (Exception e) {
            }
        }
        return db;
    }

    @Override
    protected void finalize() throws Throwable {
        if (db != null) {
            try {
                db.close();
            } catch (Exception e) {
            }
        }
        super.finalize();
    }

    /**
     * 从数据库获取全部数据
     *
     * @param region
     * @return
     */
    public List<HostRecord> readFromDb(String region) {
        synchronized (lock) {
            ArrayList<HostRecord> hosts = new ArrayList<>();

            SQLiteDatabase db = null;
            Cursor cursor = null;
            try {
                db = getDB();
                cursor = db.query(HOST.TABLE_NAME, null, HOST.COL_REGION + " = ?", new String[]{region}, null, null, null);
                if (cursor != null && cursor.getCount() > 0) {
                    cursor.moveToFirst();
                    do {
                        HostRecord hostRecord = new HostRecord();
                        hostRecord.setId(cursor.getLong(cursor.getColumnIndex(HOST.COL_ID)));
                        hostRecord.setRegion(cursor.getString(cursor.getColumnIndex(HOST.COL_REGION)));
                        hostRecord.setHost(cursor.getString(cursor.getColumnIndex(HOST.COL_HOST)));
                        hostRecord.setIps(CommonUtil.parseStringArray(cursor.getString(cursor.getColumnIndex(HOST.COL_IPS))));
                        hostRecord.setType(cursor.getInt(cursor.getColumnIndex(HOST.COL_TYPE)));
                        hostRecord.setTtl(cursor.getInt(cursor.getColumnIndex(HOST.COL_TTL)));
                        hostRecord.setQueryTime(cursor.getLong(cursor.getColumnIndex(HOST.COL_TIME)));
                        hostRecord.setExtra(cursor.getString(cursor.getColumnIndex(HOST.COL_EXTRA)));
                        hostRecord.setCacheKey(cursor.getString(cursor.getColumnIndex(HOST.COL_CACHE_KEY)));
                        hostRecord.setFromDB(true);
                        hosts.add(hostRecord);
                    } while (cursor.moveToNext());
                }
            } catch (Exception e) {
                HttpDnsLog.w("read from db fail " + accountId, e);
            } finally {
                try {
                    if (cursor != null) {
                        cursor.close();
                    }
                } catch (Exception e) {
                }
            }
            return hosts;
        }
    }

    /**
     * 从数据库删除数据
     *
     * @param records
     */
    public void delete(List<HostRecord> records) {
        synchronized (lock) {
            SQLiteDatabase db = null;
            try {
                db = getDB();
                db.beginTransaction();
                for (HostRecord record : records) {
                    db.delete(HOST.TABLE_NAME, HOST.COL_ID + " = ? ", new String[]{String.valueOf(record.getId())});
                }
                db.setTransactionSuccessful();
            } catch (Exception e) {
                HttpDnsLog.w("delete record fail " + accountId, e);
            } finally {
                if (db != null) {
                    try {
                        db.endTransaction();
                    } catch (Exception e) {
                    }
                }
            }
        }
    }

    /**
     * 更新数据
     *
     * @param records
     */
    public void insertOrUpdate(List<HostRecord> records) {
        synchronized (lock) {
            SQLiteDatabase db = null;
            try {
                db = getDB();
                db.beginTransaction();
                for (HostRecord record : records) {
                    ContentValues cv = new ContentValues();
                    cv.put(HOST.COL_REGION, record.getRegion());
                    cv.put(HOST.COL_HOST, record.getHost());
                    cv.put(HOST.COL_IPS, CommonUtil.translateStringArray(record.getIps()));
                    cv.put(HOST.COL_CACHE_KEY, record.getCacheKey());
                    cv.put(HOST.COL_EXTRA, record.getExtra());
                    cv.put(HOST.COL_TIME, record.getQueryTime());
                    cv.put(HOST.COL_TYPE, record.getType());
                    cv.put(HOST.COL_TTL, record.getTtl());

                    if (record.getId() != -1) {
                        db.update(HOST.TABLE_NAME, cv, HOST.COL_ID + " = ?", new String[]{String.valueOf(record.getId())});
                    } else {
                        long id = db.insert(HOST.TABLE_NAME, null, cv);
                        record.setId(id);
                    }
                }
                db.setTransactionSuccessful();
            } catch (Exception e) {
                HttpDnsLog.w("insertOrUpdate record fail " + accountId, e);
            } finally {
                if (db != null) {
                    try {
                        db.endTransaction();
                    } catch (Exception e) {
                    }
                }
            }
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            db.execSQL(HOST.CREATE_HOST_TABLE_SQL);
        } catch (Exception e) {
            HttpDnsLog.w("create db fail " + accountId, e);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion != newVersion) {
            try {
                db.beginTransaction();
                db.execSQL("DROP TABLE IF EXISTS " + HOST.TABLE_NAME + ";");
                db.setTransactionSuccessful();
                db.endTransaction();
                onCreate(db);
            } catch (Exception e) {
                HttpDnsLog.w("upgrade db fail " + accountId, e);
            }
        }
    }
}
