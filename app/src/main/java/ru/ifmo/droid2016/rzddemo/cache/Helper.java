package ru.ifmo.droid2016.rzddemo.cache;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import ru.ifmo.droid2016.rzddemo.cache.Handler;

public class Helper  extends SQLiteOpenHelper {
    private static final String DB_FILE_NAME = "timetable.db";

    private final int DB_VERSION;

    private static volatile Helper instance;

    static Helper getInstance(Context context, @DataSchemeVersion int version) {
        if (instance == null) {
            synchronized (Helper.class) {
                if (instance == null) {
                    instance = new Helper(context, version);
                }
            }
        }
        return instance;
    }

    private Helper(Context context, @DataSchemeVersion int version) {
        super(context, DB_FILE_NAME, null, version,
                new Handler(context, DB_FILE_NAME));
        DB_VERSION = version;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable;
        if (DB_VERSION == DataSchemeVersion.V1) {
            createTable = Contract.Timetable.CREATE_TABLE_V1;
        } else {
            createTable = Contract.Timetable.CREATE_TABLE_V2;
        }
        Log.d(TAG, "onCreate: " + createTable);
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(TAG, "onUpgrade: oldVersion=" + oldVersion + " newVersion=" + newVersion + " helperVersion = " + DB_VERSION);

        db.execSQL("ALTER TABLE " + Contract.Timetable.TABLE + " ADD COLUMN " + Contract.Timetable.TRAIN_NAME);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(TAG, "onDowngrade: oldVersion=" + oldVersion + " newVersion=" + newVersion + " helperVersion = " + DB_VERSION);

        String tempName = Contract.Timetable.TABLE + "_temp";
        db.execSQL("ALTER TABLE " + Contract.Timetable.TABLE + " RENAME TO " + tempName);
        db.execSQL(Contract.Timetable.CREATE_TABLE_V1);
        String allColumns = Contract.Timetable.DEPARTURE_DATE + ", ";
        for (int i = 0; i < Contract.Timetable.V1.length; i++) {
            allColumns += Contract.Timetable.V1[i];
            if (i != Contract.Timetable.V1.length - 1) {
                allColumns += ", ";
            }
        }
        db.execSQL("INSERT INTO " + Contract.Timetable.TABLE + " (" + allColumns + ") SELECT " + allColumns + " FROM " + tempName);
        db.execSQL("DROP TABLE " + tempName);
    }

    private static final String TAG = "Helper";
}
