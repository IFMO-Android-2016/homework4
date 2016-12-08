package ru.ifmo.droid2016.rzddemo.cache;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;


/**
 * Created by Andrey on 08.12.2016.
 */

public class TimetableDBHelper extends SQLiteOpenHelper {
    private static final String LOG_TAG = "TimetableDBHelper";
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
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        Log.d(LOG_TAG, "onCreate version " + version);
        if (version == DataSchemeVersion.V1) {
            sqLiteDatabase.execSQL(TimetableContract.CREATE_TABLE_V1);
        } else {
            sqLiteDatabase.execSQL(TimetableContract.CREATE_TABLE_V2);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        Log.d(LOG_TAG, "onUpgrade");
        sqLiteDatabase.execSQL("ALTER TABLE " + TimetableContract
                .TABLE + " ADD COLUMN " + TimetableContract.TRAIN_NAME +
                " TEXT;");
    }

    @Override
    public void onDowngrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        Log.d(LOG_TAG, "onDowngrade");
        StringBuilder args = new StringBuilder();
        for (int j = 0; j < TimetableContract.V1Fields.length; ++j) {
            args.append(TimetableContract.V1Fields[j]);
            if (j != TimetableContract.V1Fields.length - 1) {
                args.append(", ");
            }
        }
        sqLiteDatabase.execSQL(TimetableContract.CREATE_TABLE_TEMP);
        sqLiteDatabase.execSQL("INSERT INTO " + TimetableContract
                .TEMP_TABLE + " (" + args.toString() + ") SELECT " +
                args.toString() + " FROM " + TimetableContract.TABLE
                + ";");
        sqLiteDatabase.execSQL("DROP TABLE " + TimetableContract
                .TABLE + ";");
        sqLiteDatabase.execSQL("ALTER TABLE " + TimetableContract
                .TEMP_TABLE + " RENAME TO " + TimetableContract.TABLE
                + ";");
    }

}
