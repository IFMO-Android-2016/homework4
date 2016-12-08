package ru.ifmo.droid2016.rzddemo.cache;

import android.content.Context;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

import com.facebook.stetho.inspector.database.DatabaseConnectionProvider;

import java.io.File;

/**
 * Created by maria on 08.12.16.
 */
public class DBCorruptionHandler implements DatabaseErrorHandler{
    private final Context context;
    private final String dbName;

    private static final String LOG_TAG = "DBCorruption";

    public DBCorruptionHandler(Context context, String dbName) {
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
        if (databaseOk) {
            Log.e(LOG_TAG, "no corruption in the database: " + dbFile.getPath());
        } else {
            Log.e(LOG_TAG, "deleting the database file: " + dbFile.getPath());
            dbFile.delete();
        }
    }
}
