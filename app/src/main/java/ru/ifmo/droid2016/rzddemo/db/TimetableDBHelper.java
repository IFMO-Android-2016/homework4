package ru.ifmo.droid2016.rzddemo.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import ru.ifmo.droid2016.rzddemo.cache.DataSchemeVersion;
import ru.ifmo.droid2016.rzddemo.db.util.DatabaseCorruptionHandler;

import static ru.ifmo.droid2016.rzddemo.db.TimetableContract.Timetable.*;

public class TimetableDBHelper extends SQLiteOpenHelper {

    private static final String DB_FILE_NAME = "timetable.db";

    @DataSchemeVersion
    private static int VERSION;

    private static volatile TimetableDBHelper instance;

    public static TimetableDBHelper getInstance(Context c, @DataSchemeVersion int v) {
        if (instance == null) {
            synchronized (TimetableDBHelper.class) {
                if (instance == null) {
                    instance = new TimetableDBHelper(c, v);
                }
            }
        }
        return instance;
    }

    public TimetableDBHelper(Context c, @DataSchemeVersion int v) {
        super(c, DB_FILE_NAME, null, v,
                new DatabaseCorruptionHandler(c, DB_FILE_NAME));

        VERSION = v;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(VERSION == DataSchemeVersion.V1
                ? CREATE_TABLE_V1
                : CREATE_TABLE_V2);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int i, int i1) {
        db.execSQL("ALTER TABLE " + TABLE + " ADD COLUMN " + TRAIN_NAME + " TEXT");
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        String tmpTable = TABLE + "_TMP";
        db.execSQL("ALTER TABLE " + TABLE + " RENAME TO " + tmpTable);
        db.execSQL(CREATE_TABLE_V1);

        String columns = TIMETABLE_ID + ", "
                + DEPARTURE_DATE + ", "
                + DEPARTURE_STATION_ID + ", "
                + DEPARTURE_STATION_NAME + ", "
                + DEPARTURE_TIME + ", "
                + ARRIVAL_STATION_ID + ", "
                + ARRIVAL_STATION_NAME + ", "
                + ARRIVAL_TIME + ", "
                + TRAIN_ROUTE_ID + ", "
                + ROUTE_START_STATION_NAME + ", "
                + ROUTE_END_STATION_NAME;

        db.execSQL("INSERT INTO " + TABLE + " (" + columns
                + ") SELECT " + columns + " FROM " + tmpTable);
        db.execSQL("DROP TABLE " + tmpTable);
    }
}
