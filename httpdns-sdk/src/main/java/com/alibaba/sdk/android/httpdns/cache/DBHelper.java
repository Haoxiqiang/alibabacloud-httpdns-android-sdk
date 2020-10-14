package com.alibaba.sdk.android.httpdns.cache;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import static com.alibaba.sdk.android.httpdns.cache.DBConstants.DB_NAME;
import static com.alibaba.sdk.android.httpdns.cache.DBConstants.DB_VERSION;
import static com.alibaba.sdk.android.httpdns.cache.DBConstants.HOST;
import static com.alibaba.sdk.android.httpdns.cache.DBConstants.HOST.CREATE_HOST_TABLE_SQL;
import static com.alibaba.sdk.android.httpdns.cache.DBConstants.IP;
import static com.alibaba.sdk.android.httpdns.cache.DBConstants.IP.CREATE_IP_TABLE_SQL;
import static com.alibaba.sdk.android.httpdns.cache.DBConstants.IPV6;
import static com.alibaba.sdk.android.httpdns.cache.DBConstants.IPV6.CREATE_IPV6_TABLE_SQL;

/**
 * Created by tomchen on 2017/4/26.
 */

class DBHelper extends SQLiteOpenHelper {

    private static final String TAG = DBConfig.TAG;

    private static final boolean DEBUG = DBConfig.DEBUG;

    private static final Object LOCK = new Object();

    DBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            db.execSQL(CREATE_HOST_TABLE_SQL);
            db.execSQL(CREATE_IP_TABLE_SQL);
            db.execSQL(CREATE_IPV6_TABLE_SQL);
        } catch (Exception e) {
            if (DEBUG) {
                Log.d(TAG, e.getMessage(), e);
            }
        }

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion != newVersion) {
            try {
                db.beginTransaction();
                db.execSQL("DROP TABLE IF EXISTS " + HOST.TABLE_NAME + ";");
                db.execSQL("DROP TABLE IF EXISTS " + IP.TABLE_NAME + ";");
                db.execSQL("DROP TABLE IF EXISTS " + IPV6.TABLE_NAME + ";");
                db.setTransactionSuccessful();
                db.endTransaction();
                onCreate(db);
            } catch (Exception e) {
                if (DEBUG) {
                    Log.d(TAG, e.getMessage(), e);
                }
            }
        }
    }

    // ADD
    long add(HostRecord host) {
        synchronized (LOCK) {
            remove(host.sp, host.host);

            // insert new host
            SQLiteDatabase db = null;
            ContentValues cv = new ContentValues();
            try {
                db = getWritableDatabase();
                db.beginTransaction();
                cv.put(HOST.COL_HOST, host.host);
                cv.put(HOST.COL_SP, host.sp);
                cv.put(HOST.COL_TIME, DBCacheUtils.formatData(host.time));
                cv.put(HOST.COL_EXTRA, host.extra);
                cv.put(HOST.COL_CACHE_KEY, host.cacheKey);
                long new_host_id = db.insert(HOST.TABLE_NAME, null, cv);
                host.id = new_host_id;

                // insert new ip
                if (host.ips != null) {
                    for (IpRecord newIp : host.ips) {
                        newIp.host_id = new_host_id;
                        newIp.id = addInnerLocked(db, newIp);
                    }
                }

                // insert new ipv6
                if (host.ipsv6 != null) {
                    for (IpRecord newIpv6 : host.ipsv6) {
                        newIpv6.host_id = new_host_id;
                        newIpv6.id = addIpv6InnerLocked(db, newIpv6);
                    }
                }

                db.setTransactionSuccessful();

                return new_host_id;
            } catch (Exception e) {
                if (DEBUG) {
                    Log.d(TAG, e.getMessage(), e);
                }
            } finally {
                if (db != null) {
                    db.endTransaction();
                    db.close();
                }
            }

            return 0L;
        }
    }

    private long addInnerLocked(SQLiteDatabase db, IpRecord ip) {
        ContentValues cv = new ContentValues();
        cv.put(IP.COL_HOST_ID, ip.host_id);
        cv.put(IP.COL_IP, ip.ip);
        cv.put(IP.COL_TTL, ip.ttl);
        try {
            return db.insert(IP.TABLE_NAME, null, cv);
        } catch (Exception e) {
            if (DEBUG) {
                Log.d(TAG, e.getMessage(), e);
            }
            return 0L;
        }
    }

    private long addIpv6InnerLocked(SQLiteDatabase db, IpRecord ip) {
        ContentValues cv = new ContentValues();
        cv.put(IPV6.COL_HOST_ID, ip.host_id);
        cv.put(IPV6.COL_IP, ip.ip);
        cv.put(IPV6.COL_TTL, ip.ttl);
        try {
            return db.insert(IPV6.TABLE_NAME, null, cv);
        } catch (Exception e) {
            if (DEBUG) {
                Log.d(TAG, e.getMessage(), e);
            }
            return 0L;
        }
    }

    // DELETE
    void remove(String sp, String h) {
        synchronized (LOCK) {
            // delete old host
            final HostRecord host = getHostRecord(sp, h);
            if (host != null) {
                removeInnerLocked(host);
                // delete old ip
                if (host.ips != null) {
                    for (IpRecord oldIp : host.ips) {
                        removeInnerLocked(oldIp);
                    }
                }
                // delete old ipv6
                if (host.ipsv6 != null) {
                    for (IpRecord oldIpv6 : host.ipsv6) {
                        removeIpv6InnerLocked(oldIpv6);
                    }
                }
            }
        }
    }

    private void removeInnerLocked(HostRecord host) {
        removeHostInnerLocked(host.id);
    }

    private void removeInnerLocked(IpRecord ip) {
        removeIpInnerLocked(ip.id);
    }

    private void removeIpv6InnerLocked(IpRecord ip) {
        removeIpv6InnerLocked(ip.id);
    }

    private void removeHostInnerLocked(long id) {
        SQLiteDatabase db = null;
        try {
            db = getWritableDatabase();
            db.delete(HOST.TABLE_NAME, HOST.COL_ID + " = ?", new String[]{String.valueOf(id)});
        } catch (Exception e) {
            if (DEBUG) {
                Log.d(TAG, e.getMessage(), e);
            }
        } finally {
            if (db != null) {
                db.close();
            }
        }
    }

    private void removeIpInnerLocked(long id) {
        SQLiteDatabase db = null;
        try {
            db = getWritableDatabase();
            db.delete(IP.TABLE_NAME, IP.COL_ID + " = ?", new String[]{String.valueOf(id)});
        } catch (Exception e) {
            if (DEBUG) {
                Log.d(TAG, e.getMessage(), e);
            }
        } finally {
            if (db != null) {
                db.close();
            }
        }
    }

    private void removeIpv6InnerLocked(long id) {
        SQLiteDatabase db = null;
        try {
            db = getWritableDatabase();
            db.delete(IPV6.TABLE_NAME, IPV6.COL_ID + " = ?", new String[]{String.valueOf(id)});
        } catch (Exception e) {
            if (DEBUG) {
                Log.d(TAG, e.getMessage(), e);
            }
        } finally {
            if (db != null) {
                db.close();
            }
        }
    }


    // QUERY
    HostRecord getHostRecord(String sp, String host) {
        synchronized (LOCK) {
            HostRecord hostRecord = null;

            SQLiteDatabase db = null;
            Cursor cursor = null;
            try {
                db = getReadableDatabase();
                cursor = db.query(HOST.TABLE_NAME, null, HOST.COL_SP + "=? AND " + HOST.COL_HOST + "=?", new String[]{sp, host}, null, null, null);
                if (cursor != null && cursor.getCount() > 0) {
                    cursor.moveToFirst();
                    hostRecord = new HostRecord();
                    hostRecord.id = cursor.getInt(cursor.getColumnIndex(HOST.COL_ID));
                    hostRecord.host = cursor.getString(cursor.getColumnIndex(HOST.COL_HOST));
                    hostRecord.sp = cursor.getString(cursor.getColumnIndex(HOST.COL_SP));
                    hostRecord.time = DBCacheUtils.parseData(cursor.getString(cursor.getColumnIndex(HOST.COL_TIME)));
                    hostRecord.ips = (ArrayList<IpRecord>) getIpRecordList(hostRecord);
                    hostRecord.ipsv6 = (ArrayList<IpRecord>) getIpv6RecordList(hostRecord);
                    hostRecord.extra = cursor.getString(cursor.getColumnIndex(HOST.COL_EXTRA));
                    hostRecord.cacheKey = cursor.getString(cursor.getColumnIndex(HOST.COL_CACHE_KEY));
                }
            } catch (Exception e) {
                if (DEBUG) {
                    Log.d(TAG, e.getMessage(), e);
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
                if (db != null) {
                    db.close();
                }
            }

            return hostRecord;
        }
    }

    List<HostRecord> getAllHostRecords() {
        synchronized (LOCK) {
            ArrayList<HostRecord> hosts = new ArrayList<>();

            SQLiteDatabase db = null;
            Cursor cursor = null;
            try {
                db = getReadableDatabase();
                cursor = db.query(HOST.TABLE_NAME, null, null, null, null, null, null);
                if (cursor != null && cursor.getCount() > 0) {
                    cursor.moveToFirst();
                    do {
                        HostRecord hostRecord = new HostRecord();
                        hostRecord.id = cursor.getInt(cursor.getColumnIndex(HOST.COL_ID));
                        hostRecord.host = cursor.getString(cursor.getColumnIndex(HOST.COL_HOST));
                        hostRecord.sp = cursor.getString(cursor.getColumnIndex(HOST.COL_SP));
                        hostRecord.time = DBCacheUtils.parseData(cursor.getString(cursor.getColumnIndex(HOST.COL_TIME)));
                        hostRecord.ips = (ArrayList<IpRecord>) getIpRecordList(hostRecord);
                        hostRecord.ipsv6 = (ArrayList<IpRecord>) getIpv6RecordList(hostRecord);
                        hostRecord.extra = cursor.getString(cursor.getColumnIndex(HOST.COL_EXTRA));
                        hostRecord.cacheKey = cursor.getString(cursor.getColumnIndex(HOST.COL_CACHE_KEY));
                        hosts.add(hostRecord);
                    } while (cursor.moveToNext());
                }
            } catch (Exception e) {
                if (DEBUG) {
                    Log.d(TAG, e.getMessage(), e);
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
                if (db != null) {
                    db.close();
                }
            }

            return hosts;
        }
    }

    private List<IpRecord> getIpRecordList(long host_id) {
        final List<IpRecord> ips = new ArrayList<>();

        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = getWritableDatabase();
            cursor = db.query(IP.TABLE_NAME, null, IP.COL_HOST_ID + "=?", new String[]{String.valueOf(host_id)}, null, null, null);
            if (cursor != null && cursor.getCount() > 0) {
                cursor.moveToFirst();
                do {
                    IpRecord ip = new IpRecord();
                    ip.id = cursor.getInt(cursor.getColumnIndex(IP.COL_ID));
                    ip.host_id = cursor.getInt(cursor.getColumnIndex(IP.COL_HOST_ID));
                    ip.ip = cursor.getString(cursor.getColumnIndex(IP.COL_IP));
                    ip.ttl = cursor.getString(cursor.getColumnIndex(IP.COL_TTL));
                    ips.add(ip);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            if (DEBUG) {
                Log.d(TAG, e.getMessage(), e);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            if (db != null) {
                db.close();
            }
        }
        return ips;
    }

    private List<IpRecord> getIpv6RecordList(long host_id) {
        final List<IpRecord> ips = new ArrayList<>();

        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = getWritableDatabase();
            cursor = db.query(IPV6.TABLE_NAME, null, IPV6.COL_HOST_ID + "=?", new String[]{String.valueOf(host_id)}, null, null, null);
            if (cursor != null && cursor.getCount() > 0) {
                cursor.moveToFirst();
                do {
                    IpRecord ip = new IpRecord();
                    ip.id = cursor.getInt(cursor.getColumnIndex(IPV6.COL_ID));
                    ip.host_id = cursor.getInt(cursor.getColumnIndex(IPV6.COL_HOST_ID));
                    ip.ip = cursor.getString(cursor.getColumnIndex(IPV6.COL_IP));
                    ip.ttl = cursor.getString(cursor.getColumnIndex(IPV6.COL_TTL));
                    ips.add(ip);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            if (DEBUG) {
                Log.d(TAG, e.getMessage(), e);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            if (db != null) {
                db.close();
            }
        }
        return ips;
    }


    private List<IpRecord> getIpRecordList(HostRecord host) {
        return getIpRecordList(host.id);
    }

    private List<IpRecord> getIpv6RecordList(HostRecord host) {
        return getIpv6RecordList(host.id);
    }
}
