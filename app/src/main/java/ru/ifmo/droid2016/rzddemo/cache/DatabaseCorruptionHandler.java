package ru.ifmo.droid2016.rzddemo.cache;

import android.content.Context;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

import java.io.File;

/**
 * Created by roman on 07.12.16.
 */

public class DatabaseCorruptionHandler implements DatabaseErrorHandler {

    private final Context context;
    private final String name;

    public DatabaseCorruptionHandler(Context context, String name) {
        this.context = context;
        this.name = name;
    }

    @Override
    public void onCorruption(SQLiteDatabase sqLiteDatabase) {
        final boolean state = sqLiteDatabase.isDatabaseIntegrityOk();
        try {
            sqLiteDatabase.close();
        } catch (SQLiteException ignored) {}
        final File file = context.getDatabasePath(name);
        if (!state) {
            Log.e(LOG_TAG, "deleting the database file: " + file.getPath());
            file.delete();
        } else {
            Log.e(LOG_TAG, "deleting the database file: " + file.getPath());
        }
    }
    private static final String LOG_TAG = "DBCorruption";
}
