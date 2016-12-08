package ru.ifmo.droid2016.rzddemo.cache;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Created by Игорь on 08.12.2016.
 */

public class SQLHelper extends SQLiteOpenHelper {
    private static final String DB_FILE_NAME = "trains.db";
    private static final String LOG_TAG = SQLHelper.class.getSimpleName();

    private static volatile SQLHelper mInstance;

    private
    @DataSchemeVersion
    int version;

    public static synchronized SQLHelper getInstance(Context context,
                                                     @DataSchemeVersion int version) {
        if (mInstance == null) {
            mInstance = new SQLHelper(context, version);
        }
        return mInstance;
    }

    private SQLHelper(Context context, @DataSchemeVersion int version) {
        super(context, DB_FILE_NAME, null, version);
        this.version = version;

    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        Log.d(LOG_TAG, "onCreate");
        sqLiteDatabase.execSQL(CacheContract.
                getTableCreateCommand(CacheContract.TABLE_NAME, version));
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        Log.d(LOG_TAG, "onUpgrade");
        sqLiteDatabase.execSQL("ALTER TABLE " + CacheContract.TABLE_NAME +
                " ADD COLUMN " + CacheContract.TRAIN_NAME + " TEXT;");
    }

    @Override
    public void onDowngrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        Log.d(LOG_TAG, "onDowngrade");
        sqLiteDatabase.execSQL(CacheContract.getTableCreateCommand(
                CacheContract.TEMP_TABLE_NAME, DataSchemeVersion.V1));
        sqLiteDatabase.execSQL("INSERT INTO " + CacheContract.TEMP_TABLE_NAME +
                " (" + CacheContract.getCollums(DataSchemeVersion.V1) + ") SELECT " +
                CacheContract.getCollums(DataSchemeVersion.V1) + " FROM " +
                CacheContract.TABLE_NAME + ";");
        sqLiteDatabase.execSQL("DROP TABLE " + CacheContract.TABLE_NAME + ";");
        sqLiteDatabase.execSQL("ALTER TABLE " + CacheContract.TEMP_TABLE_NAME + " RENAME TO " +
                CacheContract.TABLE_NAME + ";");
    }

    public final static class CacheContract {
        public static final String TABLE_NAME = "rzdTrainTable";
        public static final String TEMP_TABLE_NAME = "tempTable";

        public static final String ID = "_id";

        public static final String DEPARTURE_STATION_ID = "departureStationId";
        public static final String DEPARTURE_STATION_NAME = "departureStationName";
        public static final String DEPARTURE_TIME = "departureTime";

        public static final String ARRIVAL_STATION_ID = "arrivalStationId";
        public static final String ARRIVAL_STATION_NAME = "arrivalStationName";
        public static final String ARRIVAL_TIME = "arrivalTime";

        public static final String TRAIN_ROUTE_ID = "trainRouteId";
        public static final String ROUTE_START_STATION_NAME = "routeStartStationId";
        public static final String ROUTE_END_STATION_NAME = "routeEndStationId";
        public static final String TRAIN_NAME = "trainName";

        public static final String FROM_STATION_ID = "fromStationId";
        public static final String TO_STATION_ID = "toStationId";
        public static final String DATE_MSK = "dateMsk";


        public static final String[] COLUMNS_V1 = {
                ID, FROM_STATION_ID, TO_STATION_ID, DATE_MSK,
                DEPARTURE_STATION_ID, DEPARTURE_STATION_NAME, DEPARTURE_TIME,
                ARRIVAL_STATION_ID, ARRIVAL_STATION_NAME, ARRIVAL_TIME,
                TRAIN_ROUTE_ID, ROUTE_START_STATION_NAME, ROUTE_END_STATION_NAME
        };

        public static final String[] COLUMNS_V2 = {
                ID, FROM_STATION_ID, TO_STATION_ID, DATE_MSK,
                DEPARTURE_STATION_ID, DEPARTURE_STATION_NAME, DEPARTURE_TIME,
                ARRIVAL_STATION_ID, ARRIVAL_STATION_NAME, ARRIVAL_TIME,
                TRAIN_ROUTE_ID, ROUTE_START_STATION_NAME, ROUTE_END_STATION_NAME, TRAIN_NAME
        };

        public static final String TEMPLATE_CREATE_TABLE_COMMAND = " ("
                + ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + FROM_STATION_ID + " TEXT, "
                + TO_STATION_ID + " TEXT, "
                + DATE_MSK + " TEXT, "
                + DEPARTURE_STATION_ID + " TEXT, "
                + DEPARTURE_STATION_NAME + " TEXT, "
                + DEPARTURE_TIME + " INTEGER, "
                + ARRIVAL_STATION_ID + " TEXT, "
                + ARRIVAL_STATION_NAME + " TEXT, "
                + ARRIVAL_TIME + " INTEGER, "
                + TRAIN_ROUTE_ID + " TEXT, "
                + ROUTE_START_STATION_NAME + " TEXT, "
                + ROUTE_END_STATION_NAME + " TEXT";
        private static final String TEMPLATE_CREATE_TABLE_COMMAND_V1 =
                TEMPLATE_CREATE_TABLE_COMMAND + ");";
        private static final String TEMPLATE_CREATE_TABLE_COMMAND_V2 =
                TEMPLATE_CREATE_TABLE_COMMAND + ", " + TRAIN_NAME + " TEXT);";

        public static String getCollums(@DataSchemeVersion int version) {
            String columns[];
            if (version == DataSchemeVersion.V1) {
                columns = COLUMNS_V1;
            } else {
                columns = COLUMNS_V2;
            }

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < columns.length - 1; ++i) {
                sb.append(columns[i]).append(" ,");
            }
            sb.append(columns[columns.length - 1]);
            return sb.toString();
        }

        public static String getTableCreateCommand(String tableName, @DataSchemeVersion int version) {
            if (version == DataSchemeVersion.V1) {
                return "CREATE TABLE " + tableName + TEMPLATE_CREATE_TABLE_COMMAND_V1;
            } else {
                return "CREATE TABLE " + tableName + TEMPLATE_CREATE_TABLE_COMMAND_V2;
            }
        }
    }
}
