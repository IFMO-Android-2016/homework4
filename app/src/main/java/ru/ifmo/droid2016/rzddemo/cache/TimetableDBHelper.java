package ru.ifmo.droid2016.rzddemo.cache;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import ru.ifmo.droid2016.rzddemo.cache.util.DatabaseCorruptionHandler;

/**
 * Created by AdminPC on 08.12.2016.
 */

public class TimetableDBHelper extends SQLiteOpenHelper {

    private static final String DB_FILE_NAME = "timetablecache.db";

    private final int DB_VERSION;

    private static volatile TimetableDBHelper instance;

    public static TimetableDBHelper getInstance(Context context, @DataSchemeVersion int version) {
        if (instance == null) {
            synchronized (TimetableDBHelper.class) {
                if (instance == null) {
                    instance = new TimetableDBHelper(context, version);
                }
            }
        }
        return instance;
    }

    public TimetableDBHelper(Context context, @DataSchemeVersion int version) {
        super(context, DB_FILE_NAME, null, version,
                new DatabaseCorruptionHandler(context, DB_FILE_NAME));
        DB_VERSION = version;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        if (DB_VERSION == DataSchemeVersion.V1) {
            db.execSQL(TimetableContract.Timetable.CREATE_TABLE_V1);
        } else {
            db.execSQL(TimetableContract.Timetable.CREATE_TABLE_V2);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("ALTER TABLE " + TimetableContract.Timetable.TABLE + " ADD COLUMN " + TimetableContract.Timetable.TRAIN_NAME);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        String tmp = "tabletmp";
        db.execSQL("ALTER TABLE " + TimetableContract.Timetable.TABLE + " RENAME TO " + tmp);
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
                + " (" + columns + ") SELECT " + columns + " FROM " + tmp);
        db.execSQL("DROP TABLE " + tmp);
    }
}
