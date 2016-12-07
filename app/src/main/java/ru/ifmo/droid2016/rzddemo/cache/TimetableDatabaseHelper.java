package ru.ifmo.droid2016.rzddemo.cache;

/**
 * Created by Михаил on 07.12.2016.
 */
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

class TimetableDatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_FILE_NAME = "timetable.db";

    private final int DB_VERSION;

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

    private TimetableDatabaseHelper(Context context, @DataSchemeVersion int version) {
        super(context, DB_FILE_NAME, null /*factory*/, version,
                new DatabaseCorruptionHandler(context, DB_FILE_NAME));
        DB_VERSION = version;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable;
        if (DB_VERSION == DataSchemeVersion.V1) {
            createTable = TimetableCacheContract.Timetable.CREATE_TABLE_V1;
        } else {
            createTable = TimetableCacheContract.Timetable.CREATE_TABLE_V2;
        }
        Log.d(TAG, "onCreate: " + createTable);
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(TAG, "onUpgrade: oldVersion=" + oldVersion + " newVersion=" + newVersion + " helperVersion = " + DB_VERSION);

        db.execSQL("ALTER TABLE " + TimetableCacheContract.Timetable.TABLE + " ADD COLUMN " + TimetableCacheContract.Timetable.TRAIN_NAME);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(TAG, "onDowngrade: oldVersion=" + oldVersion + " newVersion=" + newVersion + " helperVersion = " + DB_VERSION);

        String tempName = TimetableCacheContract.Timetable.TABLE + "_temp";
        db.execSQL("ALTER TABLE " + TimetableCacheContract.Timetable.TABLE + " RENAME TO " + tempName);
        db.execSQL(TimetableCacheContract.Timetable.CREATE_TABLE_V1);

        StringBuilder sb = new StringBuilder();
        sb.append(TimetableCacheContract.Timetable.DEPARTURE_DATE).append(", ");
        for (int i = 0; i < TimetableCacheContract.Timetable.V1.length; i++) {
            sb.append(TimetableCacheContract.Timetable.V1[i]);
            if (i != TimetableCacheContract.Timetable.V1.length - 1) {
                sb.append(", ");
            }
        }
        String columns = sb.toString();
        db.execSQL("INSERT INTO " + TimetableCacheContract.Timetable.TABLE + " (" + columns + ") SELECT " + columns + " FROM " + tempName);
        db.execSQL("DROP TABLE " + tempName);
    }

    private static final String TAG = "TimetableDbHelper";
}