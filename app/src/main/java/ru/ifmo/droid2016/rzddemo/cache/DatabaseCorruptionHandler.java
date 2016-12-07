package ru.ifmo.droid2016.rzddemo.cache;

import android.content.Context;
import android.database.DatabaseErrorHandler;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.io.File;

/**
 * Created by Lenovo on 07.12.2016.
 */

class DatabaseCorruptionHandler implements DatabaseErrorHandler {
    private final Context c;
    private final String name;

    DatabaseCorruptionHandler(Context c, String name) {
        this.c = c.getApplicationContext();
        this.name = name;
    }

    @Override
    public void onCorruption(SQLiteDatabase sqLiteDatabase) {
        final boolean isCorrupted = !sqLiteDatabase.isDatabaseIntegrityOk();
        try {
            sqLiteDatabase.close();
        } catch (SQLException ignored) {
        }
        final File dbFile = c.getDatabasePath(name);
        if (isCorrupted) {
            Log.d(TAG, "Database isn't corrupted");
        } else {
            Log.d(TAG, "Database is corrupted");
            dbFile.delete();
        }
    }

    private String TAG = "Database Corruption";
}
