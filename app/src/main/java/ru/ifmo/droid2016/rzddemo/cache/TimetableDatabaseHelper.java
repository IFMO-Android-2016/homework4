package ru.ifmo.droid2016.rzddemo.cache;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by egorsafronov on 00.12.2016
 */

public class TimetableDatabaseHelper extends SQLiteOpenHelper{

    private static final String DB_FILENAME = "timetable.db";

    private final int DB_VERSION;

    private static volatile TimetableDatabaseHelper instance;

    public TimetableDatabaseHelper(Context context, @DataSchemeVersion int version) {
        super(context, DB_FILENAME, null, version, new DatabaseCorruptionHandler(context, DB_FILENAME));
        DB_VERSION = version;
    }

    public static TimetableDatabaseHelper getInstance(Context context, @DataSchemeVersion int version) {
        if (instance == null) {
            synchronized (TimetableDatabaseHelper.class) {
                if (instance == null) {
                    instance = new TimetableDatabaseHelper(context, version);
                }
            }
        }
        return instance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        if (DB_VERSION == DataSchemeVersion.V1) {
            db.execSQL(TimetableCacheContract.Timetable.CREATE_TABLE_V1);
        } else {
            db.execSQL(TimetableCacheContract.Timetable.CREATE_TABLE_V2);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("ALTER TABLE " + TimetableCacheContract.Timetable.TABLE + " ADD COLUMN " + TimetableCacheContract.Timetable.TRAIN_NAME);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        String TEMP_TABLE = "timetable_temp";
        db.execSQL("ALTER TABLE " + TimetableCacheContract.Timetable.TABLE + " RENAME TO " + TEMP_TABLE);
        db.execSQL(TimetableCacheContract.Timetable.CREATE_TABLE_V1);

        String columns = TimetableCacheContract.Timetable.DEPARTURE_DATE + ", "
                + TimetableCacheContract.Timetable.DEPARTURE_STATION_ID + ", "
                + TimetableCacheContract.Timetable.DEPARTURE_STATION_NAME + ", "
                + TimetableCacheContract.Timetable.DEPARTURE_TIME + ", "
                + TimetableCacheContract.Timetable.ARRIVAL_STATION_ID + ", "
                + TimetableCacheContract.Timetable.ARRIVAL_STATION_NAME + ", "
                + TimetableCacheContract.Timetable.ARRIVAL_TIME + ", "
                + TimetableCacheContract.Timetable.TRAIN_ROUTE_ID + ", "
                + TimetableCacheContract.Timetable.ROUTE_START_STATION_NAME + ", "
                + TimetableCacheContract.Timetable.ROUTE_END_STATION_NAME;

        db.execSQL("INSERT INTO " + TimetableCacheContract.Timetable.TABLE
                + " (" + columns + ") SELECT " + columns + " FROM " + TEMP_TABLE);
        db.execSQL("DROP TABLE " + TEMP_TABLE);
    }
}
