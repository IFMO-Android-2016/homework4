package ru.ifmo.droid2016.rzddemo.cache;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Created by Ivan-PC on 04.12.2016.
 */

public class DBHelper extends SQLiteOpenHelper{
    private static final String DB_FILE_NAME = "trains.db";
    private static final String LOG_TAG = DBHelper.class.getSimpleName();

    private static volatile DBHelper instance;

    static DBHelper getInstance(Context context, int version) {
        if (instance == null) {
            synchronized (DBHelper.class) {
                if (instance == null) {
                    instance = new DBHelper(context, version);
                }
            }
        }
        return instance;
    }

    public DBHelper(Context context, int version) {
        super(context, DB_FILE_NAME, null, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d(LOG_TAG, CacheContract.getTableCreationCommand(CacheContract.TABLE_NAME, db.getVersion()));
        db.execSQL(CacheContract.getTableCreationCommand(CacheContract.TABLE_NAME, db.getVersion()));
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int i, int i1) {
        Log.d(LOG_TAG, "ALTER TABLE " + CacheContract.TABLE_NAME +
                " ADD COLUMN " + CacheContract.TRAIN_NAME + " TEXT;");
        db.execSQL("ALTER TABLE " + CacheContract.TABLE_NAME +
                " ADD COLUMN " + CacheContract.TRAIN_NAME + " TEXT;");
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(CacheContract.getTableCreationCommand(CacheContract.TEMP_TABLE_NAME, 1));
        db.execSQL("INSERT INTO " + CacheContract.TEMP_TABLE_NAME +
                " (" + CacheContract.getArgs(DataSchemeVersion.V1) + ") SELECT " +
                CacheContract.getArgs(DataSchemeVersion.V1) + " FROM " +
                CacheContract.TABLE_NAME + ";");
        db.execSQL("DROP TABLE " + CacheContract.TABLE_NAME + ";");
        db.execSQL("ALTER TABLE " + CacheContract.TEMP_TABLE_NAME + " RENAME TO " +
                CacheContract.TABLE_NAME + ";");
    }
}
