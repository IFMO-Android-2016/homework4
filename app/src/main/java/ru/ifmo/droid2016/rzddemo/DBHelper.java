package ru.ifmo.droid2016.rzddemo;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.support.annotation.InterpolatorRes;
import android.support.annotation.NonNull;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import ru.ifmo.droid2016.rzddemo.cache.DataSchemeVersion;
import ru.ifmo.droid2016.rzddemo.model.TimetableEntry;

/**
 * Created by Nemzs on 07.12.2016.
 */
public class DBHelper extends SQLiteOpenHelper {
    static String dbName = "rzhd5";
    SimpleDateFormat requestDateFormat;
    int version;
    String scheme1, scheme2;

    public DBHelper(Context context, String DBName, int dbVersion) {
        super(context, DBName, null, dbVersion);
        requestDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        //dbName = DBName;
        version = dbVersion;
        scheme1 = "create table " + dbName + " (requestTime date, departureStationId text, " +
                "departureStationName text, departureTime date, arrivalStationId text," +
                " arrivalStationName text, arrivalTime date, trainRouteId text, " +
                "routeStartStationName text, routeEndStationName text );";
        scheme2 = "create table " + dbName + " (requestTime date, departureStationId text, " +
                "departureStationName text, departureTime date, arrivalStationId text," +
                " arrivalStationName text, arrivalTime date, trainRouteId text, " +
                "trainName text, routeStartStationName text, routeEndStationName text );";
    }

    public void onCreate(SQLiteDatabase db) {
        if (version == DataSchemeVersion.V1)
            db.execSQL(scheme1);
        else db.execSQL(scheme2);
    }

    public void rowInsert(SQLiteStatement insert, TimetableEntry entry, Date date) {
        insert.bindString(1, requestDateFormat.format(date));
        insert.bindString(2, entry.departureStationId);
        insert.bindString(3, entry.departureStationName);
        insert.bindLong(4, entry.departureTime.getTimeInMillis());
        insert.bindString(5, entry.arrivalStationId);
        insert.bindString(6, entry.arrivalStationName);
        insert.bindLong(7, entry.arrivalTime.getTimeInMillis());
        insert.bindString(8, entry.trainRouteId);
        if (version == DataSchemeVersion.V2) {
            if (entry.trainName != null)
                insert.bindString(9, entry.trainName);
            else
                insert.bindNull(9);
            insert.bindString(10, entry.routeStartStationName);
            insert.bindString(11, entry.routeEndStationName);
        } else {
            insert.bindString(9, entry.routeStartStationName);
            insert.bindString(10, entry.routeEndStationName);
        }
        insert.execute();
    }

    private static String INSERT_VALUES_QUERY_V1 = "INSERT INTO " + dbName + " " +
            "( requestTime, departureStationId, departureStationName, departureTime, arrivalStationId, " +
            "arrivalStationName, arrivalTime, trainRouteId, routeStartStationName, routeEndStationName) " +
            " VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";

    private static String INSERT_VALUES_QUERY_V2 = "INSERT INTO " + dbName + " (" +
            "requestTime, departureStationId, departureStationName, departureTime, " +
            "arrivalStationId, arrivalStationName, arrivalTime, trainRouteId, trainName, " +
            "routeStartStationName, routeEndStationName)  VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";

    public void insert(@NonNull String fromStationId,
                       @NonNull String toStationId,
                       @NonNull Calendar dateMsk,
                       @NonNull List<TimetableEntry> timetable) {
        SQLiteStatement statement = null;
        SQLiteDatabase db = getWritableDatabase();
        if (version == DataSchemeVersion.V1) {
            statement = db.compileStatement(INSERT_VALUES_QUERY_V1);
        } else {
            statement = db.compileStatement(INSERT_VALUES_QUERY_V2);
        }
        db.beginTransaction();
        try {
            for (TimetableEntry row : timetable) {
                rowInsert(statement, row, dateMsk.getTime());
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d("My", "UpGrade " + Integer.toString(oldVersion) + " " + Integer.toString(newVersion));
        if (oldVersion == DataSchemeVersion.V1 && newVersion == DataSchemeVersion.V2) {
            db.execSQL("ALTER TABLE " + dbName + " ADD COLUMN trainName;");
        }

    }

    @Override
    public void onDowngrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        Log.d("My", "DownGrade " + Integer.toString(oldVersion) + " " + Integer.toString(newVersion));
        if (oldVersion == DataSchemeVersion.V2 && newVersion == DataSchemeVersion.V1) {
            String tempTable = "tmp";
            database.execSQL("ALTER TABLE " + dbName + " RENAME TO " + tempTable);
            database.execSQL(scheme1);
            String columns = "requestTime, departureStationId, departureStationName, departureTime, " +
                    "arrivalStationId, arrivalStationName, arrivalTime, trainRouteId, " +
                    "routeStartStationName, routeEndStationName";
            database.execSQL("INSERT INTO " + dbName + " (" + columns + ") SELECT " + columns + " FROM " + tempTable);
            database.execSQL("DROP TABLE " + tempTable);
        }
    }
}
