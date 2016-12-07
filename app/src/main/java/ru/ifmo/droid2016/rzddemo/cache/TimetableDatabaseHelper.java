package ru.ifmo.droid2016.rzddemo.cache;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by Anya on 07.12.2016.
 */

public class TimetableDatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_FILE_NAME = "timetable.db";
    private final int VERSION;
    private static volatile TimetableDatabaseHelper instance;

    public TimetableDatabaseHelper(Context context, @DataSchemeVersion int version) {
        super(context, DB_FILE_NAME, null /*factory*/, version,
                new DatabaseCorruptionHandler(context, DB_FILE_NAME));
        VERSION = version;
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
        if (VERSION != DataSchemeVersion.V1) {
            db.execSQL(TimetableContract.Timetable.CREATE_TABLE_V2);
        } else {
            db.execSQL(TimetableContract.Timetable.CREATE_TABLE_V1);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("ALTER TABLE " + TimetableContract.Timetable.TABLE + " ADD COLUMN " + TimetableContract.Timetable.TRAIN_NAME);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        String TEMP_TABLE = "timetable_temp";
        db.execSQL("ALTER TABLE " + TimetableContract.Timetable.TABLE + " RENAME TO " + TEMP_TABLE);
        db.execSQL(TimetableContract.Timetable.CREATE_TABLE_V1);

        String columns = TimetableContract.Timetable.DEPARTURE_DATE + ", "
                + TimetableContract.Timetable.DEPARTURE_STATION_ID + ", "
                + TimetableContract.Timetable.DEPARTURE_STATION_NAME + ", "
                + TimetableContract.Timetable.DEPARTURE_TIME + ", "
                + TimetableContract.Timetable.ARRIVAL_STATION_ID + ", "
                + TimetableContract.Timetable.ARRIVAL_STATION_NAME + ", "
                + TimetableContract.Timetable.ARRIVAL_TIME + ", "
                + TimetableContract.Timetable.TRAIN_ROUTE_ID + ", "
                + TimetableContract.Timetable.ROUTE_START_STATION_NAME + ", "
                + TimetableContract.Timetable.ROUTE_END_STATION_NAME;

        db.execSQL("INSERT INTO " + TimetableContract.Timetable.TABLE
                + " (" + columns + ") SELECT " + columns + " FROM " + TEMP_TABLE);
        db.execSQL("DROP TABLE " + TEMP_TABLE);
    }
}
