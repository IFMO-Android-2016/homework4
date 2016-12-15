package ru.ifmo.droid2016.rzddemo.cache;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import static ru.ifmo.droid2016.rzddemo.cache.Constraints.*;

/**
 * Created by vadim on 08.12.16.
 */

public class TimetableDbHelper extends SQLiteOpenHelper {

    private static String NAME = "timetable.db";

    private final int version;
    private static volatile TimetableDbHelper instance;
    private Context context;

    public static TimetableDbHelper getInstance(Context context, int version) {
        if (instance == null) {
            synchronized (TimetableDbHelper.class) {
                if (instance == null) {
                    instance = new TimetableDbHelper(context, version);
                }
            }
        }
        return instance;
    }

    public TimetableDbHelper(Context context, int version) {
        super(context, NAME, null, version);
        this.context = context;
        this.version = version;
        instance = this;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d(TAG, Constraints.getOpenWords(version));
        db.execSQL(Constraints.getOpenWords(version));
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(TAG, "onUpgrade");
        db.execSQL("ALTER TABLE " + TABLE + " ADD COLUMN " + TRAIN_NAME + " TEXT");
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(TAG, "onDowngrade");
        String tempTable = Constraints.TABLE + "temp";
        db.execSQL("ALTER TABLE " + TABLE + " RENAME TO " + tempTable);
        db.execSQL(Constraints.getOpenWords(1));
        db.execSQL("INSERT INTO " + TABLE + " (" + listString1 + ") SELECT " + listString1 + " FROM " + tempTable);
        db.execSQL("DROP TABLE " + tempTable);
    }
    private String TAG = "tag";
}
