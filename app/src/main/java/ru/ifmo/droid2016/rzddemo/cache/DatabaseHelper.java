package ru.ifmo.droid2016.rzddemo.cache;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.concurrent.atomic.AtomicInteger;


class DatabaseHelper extends SQLiteOpenHelper {

    static final String TABLE_NAME = "timetable";
    private static final String DATABASE_NAME = "rzdTimetableCache.db";
    private static final String TAG = "DB_Helper";

    @DataSchemeVersion
    private final int dbVersion;
    private AtomicInteger openCounter = new AtomicInteger();
    private static DatabaseHelper instance = null;
    private static SQLiteDatabase mDatabase;

    private DatabaseHelper(Context context, int databaseVersion) {
        super(context, DATABASE_NAME, null, databaseVersion);
        dbVersion = databaseVersion;
    }

    static synchronized void initializeInstance(Context context,
                                                       @DataSchemeVersion int databaseVersion){
        if(instance == null){
            instance = new DatabaseHelper(context, databaseVersion);
        } else {
            if(instance.dbVersion != databaseVersion){
                throw new IllegalStateException(DatabaseHelper.class.getSimpleName() + " is already initialized.");
            }
        }
    }

    static synchronized DatabaseHelper getInstance(){
        if(instance == null){
            throw new IllegalStateException(DatabaseHelper.class.getSimpleName() + " is not initialized.");
        } else {
            return instance;
        }
    }

    @Override
    public synchronized SQLiteDatabase getWritableDatabase() {
        if(openCounter.incrementAndGet() == 1){
            mDatabase = super.getWritableDatabase();
        }
        return mDatabase;
    }

    @Override
    public synchronized void close(){
        if(openCounter.decrementAndGet() == 0){
            mDatabase.close();
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d(TAG,"Created table " + TABLE_NAME + " with version " + Integer.toString(dbVersion));

        switch (dbVersion){
            case DataSchemeVersion.V1:
                db.execSQL("CREATE TABLE [" + TABLE_NAME +"] (\n" +
                        "[departureStationId] TEXT  NOT NULL,\n" +
                        "[departureStationName] TEXT  NOT NULL,\n" +
                        "[departureTime] INTEGER  NOT NULL,\n" +
                        "[arrivalStationId] TEXT  NOT NULL,\n" +
                        "[arrivalStationName] TEXT  NOT NULL,\n" +
                        "[arrivalTime] INTEGER  NOT NULL,\n" +
                        "[trainRouteId] TEXT  NOT NULL,\n" +
                        "[routeStartStationName] TEXT  NOT NULL,\n" +
                        "[routeEndStationName] TEXT  NOT NULL,\n" +
                        "PRIMARY KEY ([departureStationId],[departureStationName],[departureTime])\n" +
                        ")");
                break;
            case DataSchemeVersion.V2:
                db.execSQL("CREATE TABLE [" + TABLE_NAME +"] (\n" +
                        "[departureStationId] TEXT  NOT NULL,\n" +
                        "[departureStationName] TEXT  NOT NULL,\n" +
                        "[departureTime] INTEGER  NOT NULL,\n" +
                        "[arrivalStationId] TEXT  NOT NULL,\n" +
                        "[arrivalStationName] TEXT  NOT NULL,\n" +
                        "[arrivalTime] INTEGER  NOT NULL,\n" +
                        "[trainRouteId] TEXT  NOT NULL,\n" +
                        "[routeStartStationName] TEXT  NOT NULL,\n" +
                        "[routeEndStationName] TEXT  NOT NULL,\n" +
                        "[trainName] TEXT DEFAULT NULL NULL,\n" +
                        "PRIMARY KEY ([departureStationId],[departureStationName],[departureTime])\n" +
                        ")");
                break;
            default:
                throw new IllegalStateException(DatabaseHelper.class.getSimpleName() + " does not support database version" + Integer.toString(dbVersion));
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(TAG,"Upgraded table " + TABLE_NAME + " with version "
                + Integer.toString(oldVersion) + " to " + Integer.toString(newVersion));
        switch(oldVersion){
            case DataSchemeVersion.V1:
                db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN trainName TEXT DEFAULT NULL");
            case DataSchemeVersion.V2:
                if(newVersion == DataSchemeVersion.V2){
                    break;
                }
                throw new IllegalStateException("Wrong database upgrade to version " + Integer.toString(newVersion));
            default:
                throw new IllegalStateException("Wrong database upgrade from version " + Integer.toString(oldVersion));
        }
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(TAG,"Downgraded table " + TABLE_NAME + " with version "
                + Integer.toString(oldVersion) + " to " + Integer.toString(newVersion));
        switch(oldVersion){
            case DataSchemeVersion.V2:
                db.execSQL("CREATE TABLE downgradeTable AS SELECT " +
                        "departureStationId," +
                        "departureStationName," +
                        "departureTime," +
                        "arrivalStationId," +
                        "arrivalStationName," +
                        "arrivalTime," +
                        "trainRouteId," +
                        "routeStartStationName," +
                        "routeEndStationName" +
                        " FROM " + TABLE_NAME);
                db.execSQL("DROP TABLE " + TABLE_NAME);
                db.execSQL("ALTER TABLE downgradeTable RENAME TO " + TABLE_NAME);
            case DataSchemeVersion.V1:
                if(newVersion == DataSchemeVersion.V1){
                    break;
                }
                throw new IllegalStateException("Wrong database downgrade to version " + Integer.toString(newVersion));
            default:
                throw new IllegalStateException("Wrong database downgrade from version " + Integer.toString(oldVersion));
        }
    }
}
