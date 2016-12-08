package ru.ifmo.droid2016.rzddemo.cache;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import static ru.ifmo.droid2016.rzddemo.cache.DataSchemeVersion.V1;


public class TimetableDBHelper extends SQLiteOpenHelper {
    private static final String TAG = "TimetableDBHelper";
    private static final String DB_FILE_NAME = "timetable.dp";
    private static volatile TimetableDBHelper instance;
    private int version;

    TimetableDBHelper(Context context, int version) {
        super(context, DB_FILE_NAME, null, version);
        this.version = version;
    }

    public static TimetableDBHelper getInstance(Context context, int version) {
        if (instance == null) {
            synchronized (TimetableDBHelper.class) {
                if (instance == null) {
                    instance = new TimetableDBHelper(context, version);
                }
            }
        }
        return instance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d(TAG, "onCreate version " + version);
        if (version == V1) {
            db.execSQL(TimetableContract.Timetables.CREATE_TABLE_V1);
        } else {
            db.execSQL(TimetableContract.Timetables.CREATE_TABLE_V2);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(TAG, "onUpgrade ");
        db.execSQL("ALTER TABLE " + TimetableContract.Timetables.TABLE + " ADD COLUMN " + TimetableContract.Timetables.TRAIN_NAME + " TEXT;");
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        StringBuilder args = new StringBuilder();
        for (int j = 0; j < TimetableContract.Timetables.argumentsV1.length; ++j) {
            args.append(TimetableContract.Timetables.argumentsV1[j]);
            if (j != TimetableContract.Timetables.argumentsV1.length - 1) {
                args.append(", ");
            }
        }
        Log.d(TAG, "onDowngrade ");
        db.execSQL(TimetableContract.Timetables.CREATE_TABLE_TEMP);
        db.execSQL("INSERT INTO " + TimetableContract.Timetables.TEMP_TABLE + " (" + args.toString() + ") SELECT " +
                args.toString() + " FROM " + TimetableContract.Timetables.TABLE + ";");
        db.execSQL("DROP TABLE " + TimetableContract.Timetables.TABLE + ";");
        db.execSQL("ALTER TABLE " + TimetableContract.Timetables.TEMP_TABLE + " RENAME TO " + TimetableContract.Timetables.TABLE + ";");
    }
}