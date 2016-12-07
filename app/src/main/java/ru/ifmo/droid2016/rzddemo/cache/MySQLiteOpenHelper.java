package ru.ifmo.droid2016.rzddemo.cache;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import ru.ifmo.droid2016.rzddemo.model.TimetableEntry;

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
// +   final String trainName;
// +   final String routeStartStationName;
// +   final String routeEndStationName;


    public static final String DEPARTURES_STATION_ID = "COLUMN_DEPARTURES_STATION_ID";
    public static final String DEPARTURES_STATION_NAME = "COLUMN_DEPARTURES_STATION_NAME";
    public static final String DEPARTURES_TIME = "COLUMN_DEPARTURES_TIME";

    public static final String ARRIVAL_STATION_ID = "COLUMN_ARRIVAL_STATION_ID";
    public static final String ARRIVAL_STATION_NAME = "COLUMN_ARRIVAL_STATION_NAME";
    public static final String ARRIVAL_TIME = "COLUMN_ARRIVAL_TIME";


    public static final String TRAIN_ROUTE_ID = "COLUMN_TRAIN_ROUTE_ID";

    public static final String TRAIN_NAME = "COLUMN_TRAIN_NAME";

    public static final String ROUTE_START_STATION_NAME = "COLUMN_ROUTE_START_STATION_NAME";
    public static final String ROUTE_END_STATION_NAME = "COLUMN_ROUTE_END_STATION_NAME";

    public static String[] getColonsNames(int version) {
        if (version == 1) {
            String []res = {
                    DEPARTURES_STATION_ID,
                    DEPARTURES_STATION_NAME,
                    DEPARTURES_TIME,

                    ARRIVAL_STATION_ID,
                    ARRIVAL_STATION_NAME,
                    ARRIVAL_TIME,

                    TRAIN_ROUTE_ID,
                    ROUTE_START_STATION_NAME,
                    ROUTE_END_STATION_NAME,
            };
            return res;
        } else {
            String []res = {
                    DEPARTURES_STATION_ID,
                    DEPARTURES_STATION_NAME,
                    DEPARTURES_TIME,

                    ARRIVAL_STATION_ID,
                    ARRIVAL_STATION_NAME,
                    ARRIVAL_TIME,

                    TRAIN_ROUTE_ID,
                    ROUTE_START_STATION_NAME,
                    ROUTE_END_STATION_NAME,

                    TRAIN_NAME,
            };
            return res;
        }
    }


    public static final String SQL_CREATE_ENTRIES[] = {
            "CREATE TABLE " + TABLE_NAME
                    + "("
                    + DEPARTURES_STATION_ID + " TEXT, "
                    + DEPARTURES_STATION_NAME + " TEXT, "
                    + DEPARTURES_TIME + " TEXT, "

                    + ARRIVAL_STATION_ID + " TEXT, "
                    + ARRIVAL_STATION_NAME + " TEXT, "
                    + ARRIVAL_TIME + " TEXT, "

                    + TRAIN_ROUTE_ID + " TEXT, "
                    + ROUTE_START_STATION_NAME + " TEXT, "
                    + ROUTE_END_STATION_NAME + " TEXT"

                    + ");"
            ,

            "CREATE TABLE " + TABLE_NAME
                    + "("
                    + DEPARTURES_STATION_ID + " TEXT, "
                    + DEPARTURES_STATION_NAME + " TEXT, "
                    + DEPARTURES_TIME + " TEXT, "

                    + ARRIVAL_STATION_ID + " TEXT, "
                    + ARRIVAL_STATION_NAME + " TEXT, "
                    + ARRIVAL_TIME + " TEXT, "

                    + TRAIN_ROUTE_ID + " TEXT, "
                    + ROUTE_START_STATION_NAME + " TEXT, "
                    + ROUTE_END_STATION_NAME + " TEXT, "

                    + TRAIN_NAME + " TEXT"
                    + ");"
    };

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
        db.execSQL(SQL_CREATE_ENTRIES[DATABASE_VERSION - 1]);

        Log.d(LOG_TAG, "in onCreate()");
    }


    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        if ((oldVersion == 2) && (newVersion == 1)) {
            String creating_command = "CREATE TABLE " + "new_" + TABLE_NAME
                    + "("
                    + DEPARTURES_STATION_ID + " TEXT, "
                    + DEPARTURES_STATION_NAME + " TEXT, "
                    + DEPARTURES_TIME + " TEXT, "

                    + ARRIVAL_STATION_ID + " TEXT, "
                    + ARRIVAL_STATION_NAME + " TEXT, "
                    + ARRIVAL_TIME + " TEXT, "

                    + TRAIN_ROUTE_ID + " TEXT, "
                    + ROUTE_START_STATION_NAME + " TEXT, "
                    + ROUTE_END_STATION_NAME + " TEXT"

                    + ");";

            db.execSQL(creating_command);

            Cursor cursor = null;



            SQLiteStatement statement = null;

            try {
                statement = db.compileStatement(
                        "INSERT INTO "
                                + "new_" + TABLE_NAME + " VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?);");
                db.beginTransaction();

                cursor = db.query(
                        TABLE_NAME,
                        getColonsNames(1),
                        null,
                        null,
                        null, null, null
                );

                if ((cursor != null) && (cursor.moveToFirst())) {
                    try {
                        for (; !cursor.isAfterLast(); cursor.moveToNext()) {
                            statement.clearBindings();

                            Log.d(LOG_TAG, "+");

                            statement.bindString(1, cursor.getString(0));
                            statement.bindString(2, cursor.getString(1));
                            statement.bindString(3, cursor.getString(2));

                            statement.bindString(4, cursor.getString(3));
                            statement.bindString(5, cursor.getString(4));
                            statement.bindString(6, cursor.getString(5));

                            statement.bindString(7, cursor.getString(6));
                            statement.bindString(8, cursor.getString(7));
                            statement.bindString(9, cursor.getString(8));

                            statement.execute();
                        }
                        db.setTransactionSuccessful();

                        Log.d(LOG_TAG, "OK");

                    } catch (Exception ex) {
                    } finally {
                        db.endTransaction();
                    }
                }
            } finally {
                if (statement != null) {
                    statement.close();
                }
                if (cursor != null) {
                    cursor.close();
                }
            }


            db.execSQL("DROP TABLE " + TABLE_NAME);

            db.execSQL("ALTER TABLE new_" + TABLE_NAME + " RENAME TO " + TABLE_NAME);
        }
    }


    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(LOG_TAG, "onUpgrade" + oldVersion + " " + newVersion);

        if ((oldVersion == 1) && (newVersion == 2)) {
            db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD " + TRAIN_NAME + " TEXT");
        }
    }


}
