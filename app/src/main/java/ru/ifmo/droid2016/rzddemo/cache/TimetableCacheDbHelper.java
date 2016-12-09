package ru.ifmo.droid2016.rzddemo.cache;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.io.File;

import ru.ifmo.droid2016.rzddemo.utils.DatabaseCorruptionHandler;

/**
 * Created by xRoms on 08.12.2016.
 */

public class TimetableCacheDbHelper extends SQLiteOpenHelper{
    private static final String DB_FILE_NAME = "timetable.db";

    private static int DB_VERSION;

    private static volatile TimetableCacheDbHelper instance;

    public static TimetableCacheDbHelper getInstance(Context context, @DataSchemeVersion int version) {
        if (instance == null) {
            synchronized (TimetableCacheDbHelper.class) {
                if (instance == null) {
                    instance = new TimetableCacheDbHelper(context, version);
                }
            }
        }
        return instance;
    }

    private final Context context;

    public TimetableCacheDbHelper(Context context, @DataSchemeVersion int version) {
        super(context, DB_FILE_NAME, null /*factory*/, version,
                new DatabaseCorruptionHandler(context, DB_FILE_NAME));
        this.context = context.getApplicationContext();
        DB_VERSION = version;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        if (DB_VERSION == DataSchemeVersion.V1) {
            db.execSQL(TimetableCacheContract.Timetable.CREATE_TABLE_V1);
        }
        else {
            db.execSQL(TimetableCacheContract.Timetable.CREATE_TABLE_V2);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(LOG_TAG, "onUpgrade: oldVersion=" + oldVersion + " newVersion=" + newVersion);
        db.execSQL("ALTER TABLE " + TimetableCacheContract.Timetable.TABLE + " ADD COLUMN " + TimetableCacheContract.Timetable.TRAIN_NAME);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(LOG_TAG, "onDowngrade: oldVersion=" + oldVersion + " newVersion=" + newVersion + " helperVersion = " + DB_VERSION);

        db.execSQL(TimetableCacheContract.Timetable.CREATE_TABLE_TEMP);
        String columns = TimetableCacheContract.Timetable.ROUTE_DATE;
        for (int i = 0; i < TimetableCacheContract.Timetable.Everything_V1.length; i++) {
            columns += ", " + TimetableCacheContract.Timetable.Everything_V1[i];
        }
        db.execSQL("INSERT INTO " + TimetableCacheContract.Timetable.TABLE + "_TEMP (" + columns + ") SELECT " + columns + " FROM " + TimetableCacheContract.Timetable.TABLE);
        db.execSQL("DROP TABLE " + TimetableCacheContract.Timetable.TABLE);
        db.execSQL("ALTER TABLE " + TimetableCacheContract.Timetable.TABLE + "_TEMP RENAME TO " + TimetableCacheContract.Timetable.TABLE);
    }

    public void dropDb() {
        SQLiteDatabase db = getWritableDatabase();
        if (db.isOpen()) {
            try {
                db.close();
            } catch (Exception e) {
                Log.w(LOG_TAG, "Failed to close DB");
            }
        }
        final File dbFile = context.getDatabasePath(DB_FILE_NAME);
        try {
            Log.d(LOG_TAG, "deleting the database file: " + dbFile.getPath());
            if (!dbFile.delete()) {
                Log.w(LOG_TAG, "Failed to delete database file: " + dbFile);
            }
            Log.d(LOG_TAG, "Deleted DB file: " + dbFile);
        } catch (Exception e) {
            Log.w(LOG_TAG, "Failed to delete database file: " + dbFile, e);
        }
    }

    private static final String LOG_TAG = "CitiesDB";
}
