package ru.ifmo.droid2016.rzddemo.cache;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import static ru.ifmo.droid2016.rzddemo.cache.TimetableContract.*;

/**
 * Created by penguinni on 07.12.16.
 */

public class TimetableDBHelper extends SQLiteOpenHelper {
    public TimetableDBHelper(Context context, int version) {
        super(context, DB_FILENAME, null, version);
        this.version = version;
    }

    private static final String DB_FILENAME = "timetable_cache.db";
    private static volatile TimetableDBHelper instance = null;
    private static int version;


    public static TimetableDBHelper getInstance(Context context, int version) {
        Log.d(TAG, "get instance");
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
        Log.d(TAG, "onCreate timetable, version " + version);

        sqLiteDatabase.execSQL(Timetable.create(Timetable.TABLE, version));
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        Log.d(TAG, "onUpgrade: oldVersion=" + oldVersion + " newVersion=" + newVersion);

        sqLiteDatabase.execSQL("ALTER TABLE " + Timetable.TABLE +
                " ADD COLUMN " + TimetableColumns.TRAIN_NAME + " TEXT");
    }

    @Override
    public void onDowngrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        Log.d(TAG, "onUpgrade: oldVersion=" + oldVersion + " newVersion=" + newVersion);

        String table_temporary = Timetable.TABLE + "_temporary";
        sqLiteDatabase.execSQL(Timetable.create(table_temporary, version));
        sqLiteDatabase.execSQL("INSERT INTO " + table_temporary +
                " (" + Timetable.getArgsStr(newVersion) + ") " +
                "SELECT " + Timetable.getArgsStr(newVersion) + " FROM " + Timetable.TABLE);
        sqLiteDatabase.execSQL("DROP TABLE " + Timetable.TABLE);
        sqLiteDatabase.execSQL("ALTER TABLE " + table_temporary + " RENAME TO " + Timetable.TABLE);
    }

    private static final String TAG = "TimetableDBHelper";
}
