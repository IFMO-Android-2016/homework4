package ru.ifmo.droid2016.rzddemo.cache;

import android.util.Log;

/**
 * Created by Ivan-PC on 04.12.2016.
 */

public class CacheContract {
    private static final String LOG_TAG = CacheContract.class.getSimpleName();

    static final String TABLE_NAME = "trainTable";
    static final String TEMP_TABLE_NAME = "tempTable";

    static final String ID = "_id";

    static final String DEPARTURE_STATION_ID = "departureStationId";
    static final String DEPARTURE_STATION_NAME = "departureStationName";
    static final String DEPARTURE_TIME = "departureTime";

    static final String ARRIVAL_STATION_ID = "arrivalStationId";
    static final String ARRIVAL_STATION_NAME = "arrivalStationName";
    static final String ARRIVAL_TIME = "arrivalTime";

    static final String TRAIN_ROUTE_ID = "trainRouteId";
    static final String ROUTE_START_STATION_NAME = "routeStartStationId";
    static final String ROUTE_END_STATION_NAME = "routeEndStationId";
    static final String TRAIN_NAME = "trainName";

    static final String FROM_STATION_ID = "fromStationId";
    static final String TO_STATION_ID = "toStationId";
    static final String DATE_MSK = "dateMsk";

    static final String[] ARGS_V1 = {
        ID, FROM_STATION_ID, TO_STATION_ID, DATE_MSK,
                DEPARTURE_STATION_ID, DEPARTURE_STATION_NAME, DEPARTURE_TIME,
                ARRIVAL_STATION_ID, ARRIVAL_STATION_NAME, ARRIVAL_TIME,
                TRAIN_ROUTE_ID, ROUTE_START_STATION_NAME, ROUTE_END_STATION_NAME
    };

    static final String[] ARGS_V2 = {
            ID, FROM_STATION_ID, TO_STATION_ID, DATE_MSK,
            DEPARTURE_STATION_ID, DEPARTURE_STATION_NAME, DEPARTURE_TIME,
            ARRIVAL_STATION_ID, ARRIVAL_STATION_NAME, ARRIVAL_TIME,
            TRAIN_ROUTE_ID, ROUTE_START_STATION_NAME, ROUTE_END_STATION_NAME, TRAIN_NAME
    };

    static String getArgs(int version) {
        String[] s;
        if (version == DataSchemeVersion.V1) {
            s = ARGS_V1;
        }
        else {
            s = ARGS_V2;
        }

        String ret = "";
        for (int i = 0; i < s.length; ++i) {
            ret += s[i];
            if (i + 1 != s.length) {
                ret += ", ";
            }
        }
        return ret;
    }

    static String getTableCreationCommand(String tableName, int version) {
        String ret = "CREATE TABLE " + tableName + " ("
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
        if (version == DataSchemeVersion.V2) {
            ret += ", " + TRAIN_NAME + " TEXT";
        }
        ret += ");";
        Log.d(LOG_TAG, ret);
        return ret;
    }
}
