package ru.ifmo.droid2016.rzddemo;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import ru.ifmo.droid2016.rzddemo.cache.DataSchemeVersion;

import java.util.HashMap;

/**
 * Created by faerytea on 06.12.16.
 */
public class DBHelper extends SQLiteOpenHelper {
    public static final String TABLE_NAME = "RZD_CASH";
    public static final String DEPARTURE_STATION_ID = "DEPSTID";
    public static final String DEPARTURE_STATION_NAME = "DEPSTNAME";
    public static final String DEPARTURE_TIME = "DEPTIME";
    public static final String ARRIVAL_STATION_ID = "ARRSTID";
    public static final String ARRIVAL_STATION_NAME = "ARRSTNAME";
    public static final String ARRIVAL_TIME = "ARRTIME";
    public static final String TRAIN_ROUTE_ID = "TRAINROUTEID";
    public static final String TRAIN_NAME = "TRAINNAME";
    public static final String ROUTE_START_STATION_NAME = "ROUTESTARTSTNAME";
    public static final String ROUTE_END_STATION_NAME = "ROUTEENDSTNAME";
    public static final HashMap<Integer, DBHelper> instances = new HashMap<>(2);

    @Nullable
    public static DBHelper getInstance(@DataSchemeVersion int version) {
        return instances.get(version);
    }

    @NonNull
    public static DBHelper getOrCreateInstance(Context context, @DataSchemeVersion int version) {
        DBHelper ret;
        synchronized (instances) {
            ret = instances.get(version);
            if (ret == null) ret = createInstance(context, version);
        }
        return ret;
    }

    @NonNull
    public static DBHelper createInstance(Context context, @DataSchemeVersion int version) {
        return createInstance(context, version, null);
    }

    @NonNull
    public static DBHelper createInstance(Context context, int version, DatabaseErrorHandler errorHandler) {
        synchronized (instances) {
            if (instances.get(version) != null) throw new IllegalStateException("instance already here");
            DBHelper ret = new DBHelper(context, version, errorHandler);
            instances.put(version, ret);
            return ret;
        }
    }

    private DBHelper(Context context, @DataSchemeVersion int version) {
        this(context, version, null);
    }

    public DBHelper(Context context, @DataSchemeVersion int version, DatabaseErrorHandler errorHandler) {
        super(context, "ruIfmoDroid2016RzdDemo", null, version, errorHandler);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.beginTransaction();
        try {
            createTable(db, db.getVersion());
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion == newVersion) return;
        db.beginTransaction();
        try {
            if (newVersion == 1) {
                db.execSQL("ALTER TABLE " + TABLE_NAME + " RENAME TO TMP");
                createTable(db, 1);
                Cursor c = null;
                try {
                    c = db.rawQuery("SELECT * FROM TMP", new String[0]);
                    for (int i = 0; i < c.getCount(); ++i) {
                        ContentValues cv = new ContentValues(c.getColumnCount());
                        cv.put(DEPARTURE_STATION_ID, c.getLong(c.getColumnIndex(DEPARTURE_STATION_ID)));
                        cv.put(DEPARTURE_STATION_NAME, c.getString(c.getColumnIndex(DEPARTURE_STATION_NAME)));
                        cv.put(DEPARTURE_TIME, c.getString(c.getColumnIndex(DEPARTURE_TIME)));
                        cv.put(ARRIVAL_STATION_ID, c.getLong(c.getColumnIndex(ARRIVAL_STATION_ID)));
                        cv.put(ARRIVAL_STATION_NAME, c.getString(c.getColumnIndex(ARRIVAL_STATION_NAME)));
                        cv.put(ARRIVAL_TIME, c.getString(c.getColumnIndex(ARRIVAL_TIME)));
                        cv.put(TRAIN_ROUTE_ID, c.getLong(c.getColumnIndex(TRAIN_ROUTE_ID)));
                        cv.put(ROUTE_START_STATION_NAME, c.getString(c.getColumnIndex(ROUTE_START_STATION_NAME)));
                        cv.put(ROUTE_END_STATION_NAME, c.getString(c.getColumnIndex(ROUTE_END_STATION_NAME)));
                        db.insert(TABLE_NAME, null, cv); // TODO: 07.12.16 replace with SQLiteStatement
                    }
                } finally {
                    if (c != null) c.close();
                }
                db.setTransactionSuccessful();
            } else {
                if (newVersion != 2) throw new RuntimeException("Wrong DB version: " + newVersion);
                db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN " + TRAIN_NAME + " TEXT;");
                db.setTransactionSuccessful();
            }
        } finally {
            db.endTransaction();
        }
    }

    private void createTable(SQLiteDatabase db, int version) {
        db.execSQL("CREATE TABLE " + TABLE_NAME + " ("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + DEPARTURE_STATION_ID + " INTEGER, "
                + DEPARTURE_STATION_NAME + " TEXT, "
                + DEPARTURE_TIME + " INTEGER, "
                + ARRIVAL_STATION_ID + " INTEGER, "
                + ARRIVAL_STATION_NAME + " TEXT, "
                + ARRIVAL_TIME + " INTEGER, "
                + TRAIN_ROUTE_ID + " INTEGER, "
                + (version == 2 ? TRAIN_NAME + " TEXT, " : "")
                + ROUTE_START_STATION_NAME + " TEXT, "
                + ROUTE_END_STATION_NAME + " TEXT);");
    }
}
