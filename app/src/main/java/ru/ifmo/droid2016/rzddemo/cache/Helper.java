package ru.ifmo.droid2016.rzddemo.cache;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import ru.ifmo.droid2016.rzddemo.utils.DatabaseCorruptionHandler;

/**
 * Created by ivantrofimov on 07.12.16.
 */

class Helper extends SQLiteOpenHelper {

    private static final String FILE_NAME = "timetable.db";

    private final int VERSION;

    private static volatile Helper instance;

    static Helper getInstance(Context context, @DataSchemeVersion int version) {
        if (instance == null) {
            synchronized (Helper.class) {
                if (instance == null) {
                    instance = new Helper(context, version);
                }
            }
        }
        return instance;
    }

    private Helper(Context context, @DataSchemeVersion int version) {
        super(context, FILE_NAME, null /*factory*/, version,
                new DatabaseCorruptionHandler(context, FILE_NAME));
        VERSION = version;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable;
        if (VERSION == DataSchemeVersion.V1) {
            createTable = Contracts.Timetable.CREATE_TABLE_V1;
        } else {
            createTable = Contracts.Timetable.CREATE_TABLE_V2;
        }
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("ALTER TABLE " + Contracts.Timetable.TABLE + " ADD COLUMN " + Contracts.Timetable.TRAIN_NAME);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        String tempName = Contracts.Timetable.TABLE + "_temp";
        db.execSQL("ALTER TABLE " + Contracts.Timetable.TABLE + " RENAME TO " + tempName);
        db.execSQL(Contracts.Timetable.CREATE_TABLE_V1);
        String allColumns = Contracts.Timetable.DEPARTURE_DATE + ", ";
        for (int i = 0; i < Contracts.Timetable.COLUMNS_V1.length; i++) {
            allColumns += Contracts.Timetable.COLUMNS_V1[i];
            if (i != Contracts.Timetable.COLUMNS_V1.length - 1) {
                allColumns += ", ";
            }
        }
        db.execSQL("INSERT INTO " + Contracts.Timetable.TABLE + " (" + allColumns + ") SELECT " + allColumns + " FROM " + tempName);
        db.execSQL("DROP TABLE " + tempName);
    }
}
