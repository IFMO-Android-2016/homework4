package ru.ifmo.droid2016.rzddemo.cache;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

import ru.ifmo.droid2016.rzddemo.Constants;

/**
 * Created by Koroleva Yana.
 */

public class TimetableDatabaseHelper extends SQLiteOpenHelper implements BaseColumns {


    private static final String DB_NAME = "timetable.db";
    private int dbVersion;

    private static volatile TimetableDatabaseHelper instance;

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

    public TimetableDatabaseHelper(Context context,  @DataSchemeVersion int version) {
        super(context, DB_NAME, null, version);
        dbVersion = version;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        if (dbVersion == DataSchemeVersion.V1) {
            db.execSQL(Constants.DB_V1);
        } else {
            db.execSQL(Constants.DB_V2);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("ALTER TABLE " + Constants.TIMETABLE_TABLE_NAME + " ADD COLUMN " + Constants.TRAIN_NAME);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        String tmpTableName = Constants.TIMETABLE_TABLE_NAME + "tmp";
        db.execSQL("ALTER TABLE " + Constants.TIMETABLE_TABLE_NAME + " RENAME TO " + tmpTableName);
        db.execSQL(Constants.DB_V1);
        String allColumns = Constants.DEPARTURE_DATE + ", ";
        for (int i = 0; i < Constants.V1_COMPONENTS.length; i++) {
            allColumns += Constants.V1_COMPONENTS[i];
            if (i != Constants.V1_COMPONENTS.length - 1) {
                allColumns += ", ";
            }
        }
        db.execSQL("INSERT INTO " + Constants.TIMETABLE_TABLE_NAME + " (" + allColumns + ") SELECT "
                + allColumns + " FROM " + tmpTableName);
        db.execSQL("DROP TABLE " + tmpTableName);

    }
}
