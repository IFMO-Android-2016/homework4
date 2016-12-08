package ru.ifmo.droid2016.rzddemo.cache.util;

import android.content.Context;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

import java.io.File;

/**
 * Created by AdminPC on 08.12.2016.
 */

public class DatabaseCorruptionHandler  implements DatabaseErrorHandler {

    private final Context context;
    private final String dbName;

    public DatabaseCorruptionHandler(Context context, String dbName) {
        this.context = context.getApplicationContext();
        this.dbName = dbName;
    }

    @Override
    public void onCorruption(SQLiteDatabase db) {
        final boolean databaseOk = db.isDatabaseIntegrityOk();
        try {
            db.close();
        } catch (SQLiteException e) {
        }
        final File dbFile = context.getDatabasePath(dbName);
        if (!databaseOk) {
            dbFile.delete();
        }
    }

}
