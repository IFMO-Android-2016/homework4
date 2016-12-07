package ru.ifmo.droid2016.rzddemo.cache;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Created by roman on 07.12.16.
 */

public class TimetableDatabaseHepler extends SQLiteOpenHelper {

    private static final String FILENAME = "timetable.db";

    private final int VERSION;

    private static volatile TimetableDatabaseHepler instance;

    public TimetableDatabaseHepler(Context context, int version) {
        super(context, FILENAME, null, version, new DatabaseCorruptionHandler(context, FILENAME));
        VERSION = version;
    }

    static TimetableDatabaseHepler getInstance(Context context, @DataSchemeVersion int version) {
        if (instance == null) {
            synchronized (TimetableDatabaseHepler.class) {
                if (instance == null) {
                    instance = new TimetableDatabaseHepler(context, version);
                }
            }
        }
        return instance;
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        String table;
        if (VERSION == DataSchemeVersion.V1) {
            table = TimetableContract.Timetable.CREATE_TABLE_V1;
        } else {
            table = TimetableContract.Timetable.CREATE_TABLE_V2;
        }
        Log.d(TAG, "onCreate: " + table);
        sqLiteDatabase.execSQL(table);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        Log.d(TAG, "onUpgrade: oldVersion=" + oldVersion + " newVersion=" + newVersion + " helperVersion = " + VERSION);
        sqLiteDatabase.execSQL("ALTER TABLE " + TimetableContract.Timetable.TABLE + " ADD COLUMN " + TimetableContract.Timetable.TRAIN_NAME);
    }

    @Override
    public void onDowngrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        Log.d(TAG, "onDowngrade: oldVersion=" + oldVersion + " newVersion=" + newVersion + " helperVersion = " + VERSION);
        String tmpName = TimetableContract.Timetable.TABLE + "_temp";
        sqLiteDatabase.execSQL("ALTER TABLE " + TimetableContract.Timetable.TABLE + " RENAME TO " + tmpName);
        sqLiteDatabase.execSQL(TimetableContract.Timetable.CREATE_TABLE_V1);

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
        sqLiteDatabase.execSQL("INSERT INTO " + TimetableContract.Timetable.TABLE + " (" + columns + ") SELECT " + columns + " FROM " + tmpName);
        sqLiteDatabase.execSQL("DROP TABLE " + tmpName);
    }
    private static final String TAG = "TimetableDbHelper";
}
