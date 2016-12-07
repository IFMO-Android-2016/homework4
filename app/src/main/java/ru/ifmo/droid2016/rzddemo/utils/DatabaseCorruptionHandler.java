package ru.ifmo.droid2016.rzddemo.utils;

import android.content.Context;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

import java.io.File;

/**
 * Created by YA on 07.12.2016.
 */

public class DatabaseCorruptionHandler implements DatabaseErrorHandler {
    private final Context con;
    private final String nameOfDB;

    public DatabaseCorruptionHandler(Context con, String nameOfDB) {
        this.con = con.getApplicationContext();
        this.nameOfDB = nameOfDB;
    }

    @Override
    public void onCorruption(SQLiteDatabase db) {
        final boolean isDBGood = db.isDatabaseIntegrityOk();

        try {
            db.close();
        } catch (SQLiteException e) {}

        final File fileOFDB = con.getDatabasePath(nameOfDB);
        if (isDBGood) {
            Log.d("DatabaseCorruption", "in the database file: " + fileOFDB.getPath() + " isn't corruption");
        } else {
            Log.d("DatabaseCorruption", "the database file: " + fileOFDB.getPath() + "is deleted");
            fileOFDB.delete();
        }
    }
}
