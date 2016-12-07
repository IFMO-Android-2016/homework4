package ru.ifmo.droid2016.rzddemo.cache;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import static android.provider.BaseColumns._ID;
import static ru.ifmo.droid2016.rzddemo.model.DatabaseContract.Columns.*;


/**
 * Created by Анатолий on 07.12.2016.
 */

public class Database extends SQLiteOpenHelper {

    private String TAG = "Database";

    final String DOWNGRADE_TABLE_NAME = NAME + "_downgrade";

    private final String CREATE_TABLE1 = "CREATE TABLE " + NAME + " ("
            + _ID + " INTEGER PRIMARY KEY" + ","
            + DATE + " TEXT" + ","
            + DEPARTURE_STATION_ID + " TEXT" + ","
            + DEPARTURE_STATION_NAME + " TEXT" + ","
            + DEPARTURE_TIME + " TEXT" + ","
            + ARRIVAL_STATION_ID + " TEXT" + ","
            + ARRIVAL_STATION_NAME + " TEXT" + ","
            + ARRIVAL_TIME + " TEXT" + ","
            + TRAIN_ROUTE_ID + " TEXT" + ","
            + ROUTE_START_STATION_NAME + " TEXT" + ","
            + ROUTE_END_STATION_NAME + " TEXT"
            + ")";
    private final String CREATE_TABLE2 = "CREATE TABLE " + NAME + " (" + _ID + " INTEGER PRIMARY KEY" + ","
            + DATE + " TEXT" + ","
            + DEPARTURE_STATION_ID + " TEXT" + ","
            + DEPARTURE_STATION_NAME + " TEXT" + ","
            + DEPARTURE_TIME + " TEXT" + ","
            + ARRIVAL_STATION_ID + " TEXT" + ","
            + ARRIVAL_STATION_NAME + " TEXT" + ","
            + ARRIVAL_TIME + " TEXT" + ","
            + TRAIN_ROUTE_ID + " TEXT" + ","
            + TRAIN_NAME + " TEXT" + ","
            + ROUTE_START_STATION_NAME + " TEXT" + ","
            + ROUTE_END_STATION_NAME + " TEXT"
            + ")";
    private int ver;

    private Database(Context context, @DataSchemeVersion int ver) {
        super(context, NAME, null, ver);
        this.ver = ver;
    }

    private volatile static Database instance;

    public static Database getInstance(Context context, @DataSchemeVersion int ver) {
        if (instance == null) {
            synchronized (Database.class) {
                if (instance == null) {
                    instance = new Database(context, ver);
                }
            }
        }
        return instance;
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        createDatabase(sqLiteDatabase, ver);
    }

    private void createDatabase(SQLiteDatabase sqLiteDatabase, int ver) {
        if (ver == DataSchemeVersion.V1) {
            Log.d(TAG, CREATE_TABLE1);
            sqLiteDatabase.execSQL(CREATE_TABLE1);
        } else {
            Log.d(TAG, CREATE_TABLE1);
            sqLiteDatabase.execSQL(CREATE_TABLE2);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("ALTER TABLE " + NAME + " " + "ADD COLUMN " + TRAIN_NAME + " TEXT");
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("ALTER TABLE " + NAME + " " + "RENAME TO " + DOWNGRADE_TABLE_NAME);
        createDatabase(db, DataSchemeVersion.V1);
        String columns = "" + _ID + "," + DATE + "," + DEPARTURE_STATION_ID + "," +
                DEPARTURE_STATION_NAME + "," + DEPARTURE_TIME + "," + ARRIVAL_STATION_ID + "," +
                ARRIVAL_STATION_NAME + "," + ARRIVAL_TIME + "," + TRAIN_ROUTE_ID + "," +
                ROUTE_START_STATION_NAME + "," + ROUTE_END_STATION_NAME;
        db.execSQL("INSERT INTO " + NAME + " (" + columns + ") "
                + "SELECT " + columns + " "
                + "FROM " + DOWNGRADE_TABLE_NAME);
        db.execSQL("DROP TABLE " + DOWNGRADE_TABLE_NAME);
    }
}
