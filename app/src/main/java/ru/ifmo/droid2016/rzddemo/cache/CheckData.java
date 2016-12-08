package ru.ifmo.droid2016.rzddemo.cache;

import android.content.Context;
import android.database.DatabaseErrorHandler;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.io.File;

/**
 * Created by Дарья on 08.12.2016.
 */

public class CheckData implements DatabaseErrorHandler {
    private final Context c;
    private final String name;

    CheckData(Context c, String name) {
        this.c = c.getApplicationContext();
        this.name = name;
    }

    @Override
    public void onCorruption(SQLiteDatabase sqLiteDatabase) {
        final boolean DataIsBad = !sqLiteDatabase.isDatabaseIntegrityOk();
        try {
            sqLiteDatabase.close();
        } catch (SQLException ignored) {
        }
        final File dbFile = c.getDatabasePath(name);
        if (!DataIsBad) {
            Log.d(TAG, "Database isn't bad");
        } else {
            Log.d(TAG, "Database is bad");
            dbFile.delete();
        }
    }

    private String TAG = "Check Data";
}
