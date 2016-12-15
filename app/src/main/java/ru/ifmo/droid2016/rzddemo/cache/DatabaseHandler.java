package ru.ifmo.droid2016.rzddemo.cache;

import android.content.Context;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteAccessPermException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;


public class DatabaseHandler implements DatabaseErrorHandler {

    public DatabaseHandler(Context context, String dbFileName) {

    }

    @Override
    public void onCorruption(SQLiteDatabase sqLiteDatabase) {
        try {
            sqLiteDatabase.close();
        } catch (SQLiteException ignored){

        }
    }
}
