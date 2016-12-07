package ru.ifmo.droid2016.rzddemo.cache;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Created by ghost on 12/7/2016.
 */
class SQLiteHelper extends SQLiteOpenHelper {
    static final String TABLE_NAME = "timetable";
    static final String DATE = "date";
    static final String DEPARTURE_STATION_ID = "departure_station_id";
    static final String DEPARTURE_STATION_NAME = "departure_station_name";
    static final String DEPARTURE_TIME = "departure_time";
    static final String ARRIVAL_STATION_ID = "arrival_station_id";
    static final String ARRIVAL_STATION_NAME = "arrival_station_name";
    static final String ARRIVAL_TIME = "arrival_time";
    static final String TRAIN_ROUTE_ID = "train_route_id";
    static final String ROUTE_START_STATION_NAME = "route_start_station_name";
    static final String ROUTE_END_STATION_NAME = "route_end_station_name";
    static final String TRAIN_NAME = "train_name";

    private static final String DB_FILENAME = "table.db";
    private static volatile SQLiteHelper instance;

    private final int version;

    private SQLiteHelper(Context context, @DataSchemeVersion int version) {
        super(context, DB_FILENAME, null, version);
        this.version = version;
    }

    static SQLiteHelper getInstance(Context context, int version) {
        if (instance == null) {
            synchronized (SQLiteHelper.class) {
                if (instance == null) {
                    Log.d("SQLiteHelper", "Creating helper instance");
                    instance = new SQLiteHelper(context, version);
                }
            }
        }
        return instance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createTable(db, TABLE_NAME);
    }

    private void createTable(SQLiteDatabase db, String tableName) {
        db.execSQL("CREATE TABLE " + tableName + " (" +
                DATE + " TEXT, " +
                DEPARTURE_STATION_ID + " TEXT, " +
                DEPARTURE_STATION_NAME + " TEXT, " +
                DEPARTURE_TIME + " TEXT, " +
                ARRIVAL_STATION_ID + " TEXT, " +
                ARRIVAL_STATION_NAME + " TEXT, " +
                ARRIVAL_TIME + " TEXT, " +
                TRAIN_ROUTE_ID + " TEXT, " +
                ROUTE_START_STATION_NAME + " TEXT, " +
                ROUTE_END_STATION_NAME + " TEXT" +
                (version == DataSchemeVersion.V2 ? (", " + TRAIN_NAME + " TEXT") : "") +
                ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db,
                          @DataSchemeVersion int oldVersion,
                          @DataSchemeVersion int newVersion) {
        if (oldVersion == DataSchemeVersion.V1 && newVersion == DataSchemeVersion.V2) {
            db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN " + TRAIN_NAME + " TEXT");
            return;
        }
        throw new SQLiteException("Can't upgrade database from version " +
                oldVersion + " to " + newVersion);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        final String tempTableName = TABLE_NAME + "_temp";

        createTable(db, tempTableName);

        String data =
                DATE + ", " +
                DEPARTURE_STATION_ID + ", " +
                DEPARTURE_STATION_NAME + ", " +
                DEPARTURE_TIME + ", " +
                ARRIVAL_STATION_ID + ", " +
                ARRIVAL_STATION_NAME + ", " +
                ARRIVAL_TIME + ", " +
                TRAIN_ROUTE_ID + ", " +
                ROUTE_START_STATION_NAME + ", " +
                ROUTE_END_STATION_NAME;

        db.execSQL("INSERT INTO " + tempTableName + " (" + data + ") " +
                "SELECT " + data + " FROM " + TABLE_NAME);

        db.execSQL("DROP TABLE " + TABLE_NAME);

        db.execSQL("ALTER TABLE " + tempTableName + " RENAME TO " + TABLE_NAME);
    }
}
