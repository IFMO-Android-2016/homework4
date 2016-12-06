package ru.ifmo.droid2016.rzddemo.cache;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.Calendar;

/**
 * Created by Vlad_kv on 05.12.2016.
 */

public class MySQLiteOpenHelper extends SQLiteOpenHelper{
    final String LOG_TAG = "my_tag";

    public static final String TABLE_NAME = "timetable";

    public final int DATABASE_VERSION;
    public static final String DATABASE_NAME = "Timetable.db";

// +   final String departureStationId = cursor.getString(0);
// +   final String departureStationName = cursor.getString(1);
// +   final Calendar departureTime;
// +   final String arrivalStationId;
// +   final String arrivalStationName;
// +   final Calendar arrivalTime;
//    final String trainRouteId;
//    final String trainName;
//    final String routeStartStationName;
//    final String routeEndStationName;


    public static final String DEPARTURES_STATION_ID = "COLUMN_DEPARTURES_STATION_ID";
    public static final String DEPARTURES_STATION_NAME = "COLUMN_DEPARTURES_STATION_NAME";
    public static final String DEPARTURES_TIME = "COLUMN_DEPARTURES_TIME";

    public static final String ARRIVAL_STATION_ID = "COLUMN_ARRIVAL_STATION_ID";
    public static final String ARRIVAL_STATION_NAME = "COLUMN_ARRIVAL_STATION_NAME";
    public static final String ARRIVAL_TIME = "COLUMN_ARRIVAL_TIME";


    public static final String SQL_CREATE_ENTRIES = "CREATE TABLE " + TABLE_NAME
            + "(_id INTEGER PRIMARY KEY, "
            + DEPARTURES_STATION_ID + " TEXT, "
            + DEPARTURES_STATION_NAME + " TEXT, "
            + DEPARTURES_TIME + " TEXT, "

            + ARRIVAL_STATION_ID + " TEXT, "
            + ARRIVAL_STATION_NAME + " TEXT, "
            + ARRIVAL_TIME + " TEXT"

            + ");";

    private static volatile MySQLiteOpenHelper instance = null;

    private MySQLiteOpenHelper(Context context, int version) {
        super(context, DATABASE_NAME, null, version);
        DATABASE_VERSION = version;
    }

    public static MySQLiteOpenHelper getInstance(Context context, int version) {
        if (instance == null) {
            synchronized (MySQLiteOpenHelper.class) {
                if (instance == null) {
                    instance = new MySQLiteOpenHelper(context, version);
                }
            }
        }
        return instance;
    }




    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);

        Log.d(LOG_TAG, "in onCreate()");
    }


    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(LOG_TAG, "onUpgrade" + oldVersion + " " + newVersion);

    }


}
