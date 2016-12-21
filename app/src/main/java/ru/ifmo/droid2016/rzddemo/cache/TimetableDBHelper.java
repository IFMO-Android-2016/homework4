package ru.ifmo.droid2016.rzddemo.cache;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import static ru.ifmo.droid2016.rzddemo.cache.TimetableContract.Timetable.*;


/**
 * + * Created by 1 on 16.12.2016.
 * +
 */

public class TimetableDBHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "ttcache.db";

    private static volatile TimetableDBHelper instance = null;
    @DataSchemeVersion
    private int VERSION;


    private final Context context;

    public TimetableDBHelper(Context context, @DataSchemeVersion int version) {
        super(context, DB_NAME, null, version, new DatabaseCorruptionHandler(context, DB_NAME));
        VERSION = version;
        this.context = context.getApplicationContext();
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(VERSION == DataSchemeVersion.V1 ? CREATETABLEV1 : CREATETABLEV2);
        instance = this;
    }

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

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        sqLiteDatabase.execSQL("ALTER TABLE " + TABLE + " ADD COLUMN " + TRAINNAME + " TEXT");
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        String tmpTable = TABLE + "temp";
        db.execSQL("ALTER TABLE " + TABLE + " RENAME TO " + tmpTable);
        db.execSQL(CREATETABLEV1);
        StringBuilder columns = new StringBuilder();
        for (int i = 0; i < TABLEV1.length; i++) {
            columns.append(TABLEV1[i]);
            if (i != TABLEV1.length - 1) {
                columns.append(", ");
            }
        }
        db.execSQL("INSERT INTO " + TABLE + " (" + columns.toString() + ") SELECT " + columns.toString() + " FROM " + tmpTable);
        db.execSQL("DROP TABLE " + tmpTable);
    }


}
