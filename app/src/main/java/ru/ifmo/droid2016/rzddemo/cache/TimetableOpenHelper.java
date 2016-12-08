package ru.ifmo.droid2016.rzddemo.cache;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by garik on 08.12.16.
 */

public class TimetableOpenHelper extends SQLiteOpenHelper {
    private final int version;

    public TimetableOpenHelper(Context context, @DataSchemeVersion int version) {
        super(context, TimetableContract.TimetableConst.DATABASE_NAME, null, version);
        this.version = version;
    }

    private static volatile TimetableOpenHelper instance;

    public static TimetableOpenHelper getInstance(Context context, @DataSchemeVersion int version) {
        if (instance == null) {
            synchronized (TimetableOpenHelper.class) {
                if (instance == null) {
                    instance = new TimetableOpenHelper(context, version);
                }
            }
        }
        return instance;
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(
                TimetableContract.createTableCommand(false, version));
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, int i, int i1) {
        database.execSQL(TimetableContract.getColumnInsert());
    }

    @Override
    public void onDowngrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        database.execSQL(TimetableContract.createTableCommand(true, DataSchemeVersion.V2));
        database.execSQL(TimetableContract.insertToTemp());
        database.execSQL(TimetableContract.deleteTable());
        database.execSQL(TimetableContract.renameTable());
    }


}
