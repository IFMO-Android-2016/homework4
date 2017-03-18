package ru.ifmo.droid2016.rzddemo.cache;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Created by Nik on 17.03.2017.
 */

public class TimetableDatabaseHelper extends SQLiteOpenHelper {
    private static final String FILENAME = "timetable.db";

    private final int VERSION;

    private static volatile TimetableDatabaseHelper instance;

    public TimetableDatabaseHelper(Context context, int version) {
        super(context, FILENAME, null, version, new DatabaseCorruptionHandler(context, FILENAME));
        VERSION = version;
    }

    static TimetableDatabaseHelper getInstance(Context context, @DataSchemeVersion int version) {
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
    public void onCreate(SQLiteDatabase database) {
        String table;
        if (VERSION == DataSchemeVersion.V1) {
            table = TimetableContract.Timetable.CREATE_TABLE_V1;
        } else {
            table = TimetableContract.Timetable.CREATE_TABLE_V2;
        }
        Log.d(TAG, "onCreate: " + table);
        database.execSQL(table);
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        Log.d(TAG, "onUpgrade: oldVersion=" + oldVersion + " newVersion=" + newVersion + " helperVersion = " + VERSION);
        database.execSQL("ALTER TABLE " + TimetableContract.Timetable.TABLE + " ADD COLUMN " + TimetableContract.Timetable.TRAIN_NAME);
    }

    @Override
    public void onDowngrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        Log.d(TAG, "onDowngrade: oldVersion=" + oldVersion + " newVersion=" + newVersion + " helperVersion = " + VERSION);
        String tmpName = TimetableContract.Timetable.TABLE + "_temporary";
        database.execSQL("ALTER TABLE " + TimetableContract.Timetable.TABLE + " RENAME TO " + tmpName);
        database.execSQL(TimetableContract.Timetable.CREATE_TABLE_V1);

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
        database.execSQL("INSERT INTO " + TimetableContract.Timetable.TABLE + " (" + columns + ") SELECT " + columns + " FROM " + tmpName);
        database.execSQL("DROP TABLE " + tmpName);
    }
    private static final String TAG = "TimetableDbHelper";
}
