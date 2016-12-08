package ru.ifmo.droid2016.rzddemo.cache;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.io.File;

import ru.ifmo.droid2016.rzddemo.utils.DatabaseCorruptionHandler;

/**
 * Created by Anna Kopeliovich on 07.12.2016.
 */

public class TimetableDBHelper extends SQLiteOpenHelper {

    private static final String DB_FILE_NAME = "timetable.db";

    @DataSchemeVersion
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

    private final Context context;

    public TimetableDBHelper(Context context, @DataSchemeVersion int version) {
        super(context, DB_FILE_NAME, null /*factory*/, version,
                new DatabaseCorruptionHandler(context, DB_FILE_NAME));
        this.context = context.getApplicationContext();
        DB_VERSION = version;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        if (DB_VERSION == DataSchemeVersion.V1) {
            Log.d(LOG_TAG, "onCreate: " + TimetableContract.Timetable.CREATE_TABLE_V1);
            db.execSQL(TimetableContract.Timetable.CREATE_TABLE_V1);
        } else {
            Log.d(LOG_TAG, "onCreate: " + TimetableContract.Timetable.CREATE_TABLE_V2);
            db.execSQL(TimetableContract.Timetable.CREATE_TABLE_V2);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(LOG_TAG, "onUpgrade: oldVersion=" + oldVersion + " newVersion=" + newVersion);

        db.execSQL("ALTER TABLE " + TimetableContract.Timetable.TABLE + " ADD COLUMN " + TimetableContract.Timetable.TRAIN_NAME +  " TEXT");
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(LOG_TAG, "onDowngrade: oldVersion=" + oldVersion + " newVersion=" + newVersion);

        String SWAP_TABLE = TimetableContract.Timetable.TABLE + "_swap";
        db.execSQL("ALTERE TABLE " + TimetableContract.Timetable.TABLE + " RENAME TO " + SWAP_TABLE);
        db.execSQL(TimetableContract.Timetable.CREATE_TABLE_V1);
        db.execSQL("INSERT INTO " + TimetableContract.Timetable.TABLE + "(" +
                TimetableContract.Timetable.ARGUMENTS_ON_CREATE_TABLE + ") SELECT " +
                TimetableContract.Timetable.ARGUMENTS_ON_CREATE_TABLE + " FROM " + SWAP_TABLE);

        db.execSQL("DROP TABLE " + SWAP_TABLE);
    }

    private static final String LOG_TAG = "TimetableDB";
}
