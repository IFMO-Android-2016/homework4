package ru.ifmo.droid2016.rzddemo.cache;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import static ru.ifmo.droid2016.rzddemo.cache.TimetableCacheContract.Timetable.*;

public class TimetableHelper extends SQLiteOpenHelper {

    private static final String FILENAME = "timetable.db";
    private static int VERSION;

    private static volatile TimetableHelper instance;

    public static TimetableHelper getInstance(Context context, @DataSchemeVersion int version) {
        if (instance == null) {
            synchronized (TimetableHelper.class) {
                if (instance == null) {
                    instance = new TimetableHelper(context, version);
                }
            }
        }
        return instance;
    }

    public TimetableHelper(Context context, @DataSchemeVersion int version) {
        super(context, FILENAME, null, version,
                new DatabaseCorruptionHandler(context, FILENAME));
        VERSION = version;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(VERSION == DataSchemeVersion.V1 ? CREATE_TABLE_V1 : CREATE_TABLE_V2);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int i, int i1) {
        db.execSQL("ALTER TABLE " + TABLE + " ADD COLUMN " + TRAIN_NAME + " TEXT");
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int i, int i1) {
        String TABLE_TMP = TABLE + "_temp";
        db.execSQL("ALTER TABLE " + TABLE + " RENAME TO " + TABLE_TMP);
        db.execSQL(CREATE_TABLE_V1);

        String cols = DEPARTURE_DATE
                    + ", " + DEPARTURE_STATION_ID
                    + ", " + DEPARTURE_STATION_NAME
                    + ", " + DEPARTURE_TIME
                    + ", " + ARRIVAL_STATION_ID
                    + ", " + ARRIVAL_STATION_NAME
                    + ", " + ARRIVAL_TIME
                    + ", " + TRAIN_ROUTE_ID
                    + ", " + ROUTE_START_STATION_NAME
                    + ", " + ROUTE_END_STATION_NAME;

        db.execSQL("INSERT INTO " + TABLE + " (" + cols + ") " +
                   "SELECT " + cols + " FROM " + TABLE_TMP);
    }
}
