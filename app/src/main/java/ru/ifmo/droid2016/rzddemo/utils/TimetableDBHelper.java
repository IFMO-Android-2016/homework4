package ru.ifmo.droid2016.rzddemo.utils;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import ru.ifmo.droid2016.rzddemo.cache.DataSchemeVersion;

import static ru.ifmo.droid2016.rzddemo.cache.TimetableContract.Timetable.*;

/**
 * Created by YA on 07.12.2016.
 */

public class TimetableDBHelper extends SQLiteOpenHelper {
    private static int DB_VERSION;
    private static final String DB_FILE = "timetable.db";

    private static volatile TimetableDBHelper instance;

    public static TimetableDBHelper getInstance(Context context, @DataSchemeVersion int version) {
        if (instance == null) {
            synchronized (TimetableDBHelper.class) {
                if (instance == null)
                    instance = new TimetableDBHelper(context, version);
            }
        }
        return instance;
    }

    public TimetableDBHelper(Context context, @DataSchemeVersion int version) {
        super(context, DB_FILE, null, version,
                new DatabaseCorruptionHandler(context, DB_FILE));
        DB_VERSION = version;
    }

    public void onCreate(SQLiteDatabase db) {
        if (DB_VERSION == DataSchemeVersion.V1)
            db.execSQL(NEW_FIRST_VAR);
        else
            db.execSQL(NEW_SECOND_VAR);
        Log.d("DBHelper", "new table " + Integer.toString(DB_VERSION) + " type version");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d("DBHelper", "upgrade");
        db.execSQL("ALTER TABLE " + TABLE + " ADD COLUMN " + TRAIN_NAME);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d("DBHelper", "downgrade");
        String tmp = TABLE + "_TMP";
        db.execSQL("ALTER TABLE " + TABLE + " RENAME TO " + tmp);
        db.execSQL(NEW_FIRST_VAR);
        String columns = _ID + ", " + DEPARTURE_DATE + ", "
                + DEPARTURE_STATION_ID + ", " + DEPARTURE_STATION_NAME + ", "
                + DEPARTURE_TIME + ", " + ARRIVAL_STATION_ID + ", "
                + ARRIVAL_STATION_NAME + ", " + ARRIVAL_TIME + ", "
                + TRAIN_ROUTE_ID + ", " + ROUTE_START_STATION_NAME + ", "
                + ROUTE_END_STATION_NAME;
        db.execSQL("INSERT INTO " + TABLE + " (" + columns
                + ") SELECT " + columns + " FROM " + tmp);
        db.execSQL("DROP TABLE " + tmp);
    }
}
