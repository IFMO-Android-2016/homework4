package ru.ifmo.droid2016.rzddemo.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import ru.ifmo.droid2016.rzddemo.cache.DataSchemeVersion;

import static ru.ifmo.droid2016.rzddemo.database.TimeDBHelper.TimeTableStrings.*;


public class TimeDBHelper extends SQLiteOpenHelper {

    private static int DATABASE_VERSION;

    public interface TimeTableStrings {
        String DATABASE_NAME = "timeLineDb";
        String TABLE_TIMELINES = "timeLine";

        String KEY_ID = "_id";
        String DEPARTURE_STATION_ID = "departureID";
        String ARRIVAL_STATION_ID = "arrivalID";
        String DATE = "сurrentDate";

        String ROUTE_ID = "journey";
        String ROUTE_FROM = "journeyFrom";
        String ROUTE_TO = "journeyTo";

        String TRAIN_NAME = "train";
        String DEPARTURE_STATION = "departure";
        String DEPARTURE_TIME = "depTime";
        String ARRIVAL_STATION = "arriaval";
        String ARRIVAL_TIME = "arrTime";
        String COUNT_SIMPLE_COLUMS =
                DATE + ", " +
                        DEPARTURE_TIME + ", " +
                        ARRIVAL_TIME + ", " +
                        ROUTE_ID + ", " +
                        ROUTE_FROM + ", " +
                        ROUTE_TO + ", " +
                        DEPARTURE_STATION_ID + ", " +
                        DEPARTURE_STATION + ", " +
                        ARRIVAL_STATION_ID + ", " +
                        ARRIVAL_STATION;
    }

    private TimeDBHelper(Context context, @DataSchemeVersion int version) {
        super(context, DATABASE_NAME, null, version);
        DATABASE_VERSION = version;
    }

    private static volatile TimeDBHelper instance;

    public static TimeDBHelper getInstance(Context context, @DataSchemeVersion int version) {
        if (instance == null)
            synchronized (TimeDBHelper.class) {
                if (instance == null)
                    instance = new TimeDBHelper(context, version);
            }

        return instance;
    }


    private static final String CREATE_SIMPLE_STR = "create table " + TABLE_TIMELINES + " (" +
            KEY_ID + " integer primary key, " +
            DATE + " integer, " +
            DEPARTURE_TIME + " integer, " +
            ARRIVAL_TIME + " integer, " +
            ROUTE_ID + " text, " +
            ROUTE_FROM + " text, " +
            ROUTE_TO + " text, " +
            DEPARTURE_STATION_ID + " text, " +
            DEPARTURE_STATION + " text, " +
            ARRIVAL_STATION_ID + " text, " +
            ARRIVAL_STATION + " text";

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.e("DBHelper", "onCreate");
        String createStr = CREATE_SIMPLE_STR;
        if (DATABASE_VERSION == DataSchemeVersion.V2)
            createStr += ", " + TRAIN_NAME + " text";
        createStr += " )";

        db.execSQL(createStr);
    }


    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.e("DBHelper", "onUpgrade");

            db.execSQL("alter table " + TABLE_TIMELINES + " add column " + TRAIN_NAME + " text");
    }

    private static final String TMP_TABLE_TIMELINES = "tmpTimeLine";


    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.e("DBHelper", "onDowngrade");
        db.execSQL("alter table " + TABLE_TIMELINES + " rename to " + TMP_TABLE_TIMELINES);
        db.execSQL(CREATE_SIMPLE_STR + ")");
        db.execSQL("insert into " + TABLE_TIMELINES + " (" + KEY_ID + ", " + COUNT_SIMPLE_COLUMS + ")" +
                " select " + KEY_ID + ", " + COUNT_SIMPLE_COLUMS + " from " + TMP_TABLE_TIMELINES);
        db.execSQL("drop table " + TMP_TABLE_TIMELINES);
    }
}
