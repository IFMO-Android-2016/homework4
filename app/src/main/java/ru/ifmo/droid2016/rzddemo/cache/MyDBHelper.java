package ru.ifmo.droid2016.rzddemo.cache;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import ru.ifmo.droid2016.rzddemo.cache.CacheContract.Caches;

import static ru.ifmo.droid2016.rzddemo.cache.CacheContract.Caches.*;

/**
 * Created by maria on 07.12.16.
 */
public class MyDBHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "rzdDB.db";
    private static int DB_VERSION;

    private static volatile MyDBHelper mInstance;
    private final Context context;

    static MyDBHelper getInstance(Context context, int version) {
        if (mInstance == null) {
            synchronized (MyDBHelper.class) {
                if (mInstance == null) {
                    mInstance = new MyDBHelper(context, version);
                }
            }
        }
        return mInstance;
    }

    public MyDBHelper(Context context, @DataSchemeVersion int version) {
        super(context, DB_NAME, null, version, new DBCorruptionHandler(context, DB_NAME));
        DB_VERSION = version;
        this.context = context.getApplicationContext();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.e(TAG, "onCreate: ctreated " + CREATE_TABLE1 );
        db.execSQL(DB_VERSION == DataSchemeVersion.V1 ? CREATE_TABLE1 : CREATE_TABLE2);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(TAG, "ALTER TABLE "
                + Caches.TABLE_NAME
                + " ADD COLUMN " + TRAIN_NAME + " TEXT;");
        db.execSQL("ALTER TABLE "
                + Caches.TABLE_NAME
                + " ADD COLUMN " + TRAIN_NAME + " TEXT;");
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(TAG, "ONDOWNGRADE :" + " OLVERSION " + oldVersion + " NEWVERSION " + newVersion);
        final String TEMP = TABLE_NAME + "_downgrade";
        db.execSQL("ALTER TABLE " + TABLE_NAME + " RENAME TO " + TEMP);
        db.execSQL(CREATE_TABLE1);
        String columns = DEPARTURE_TIME + ", ";
        for (int i = 0; i < ARGS1.length; i++) {
            columns += ARGS1[i];
            if (i != ARGS1.length - 1) {
                columns += ", ";
            }
        }
        db.execSQL("INSERT INTO "
                + TABLE_NAME + " (" + columns
                + ") SELECT " + columns + " FROM " + TEMP);
        db.execSQL("DROP TABLE " + TEMP);
    }

    private static final String TAG = "DBHELPER";
}
