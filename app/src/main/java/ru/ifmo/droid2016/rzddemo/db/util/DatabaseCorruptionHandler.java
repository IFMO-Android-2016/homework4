package ru.ifmo.droid2016.rzddemo.db.util;

import android.content.Context;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

import java.io.File;

public class DatabaseCorruptionHandler implements DatabaseErrorHandler {

    private final Context c;
    private final String dbName;

    public DatabaseCorruptionHandler(Context c, String dbName) {
        this.c = c.getApplicationContext();
        this.dbName = dbName;
    }

    @Override
    public void onCorruption(SQLiteDatabase db) {
        final boolean isOk = db.isDatabaseIntegrityOk();

        try {
            db.close();
        } catch (SQLiteException ignored) {
        }

        final File f = c.getDatabasePath(dbName);
        if (!isOk) {
            //noinspection ResultOfMethodCallIgnored
            f.delete();
        }
    }
}
