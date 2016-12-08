package ru.ifmo.droid2016.rzddemo.cache;

import android.provider.BaseColumns;
import android.util.Log;

/**
 * Created by maria on 07.12.16.
 */
public final class CacheContract {

    public interface CacheColumns extends BaseColumns{
        String ID = "_id";
        String DEPARTURE_STATION_ID = "departureStationId";
        String DEPARTURE_STATION_NAME = "departureStationName";
        String DEPARTURE_TIME = "departureTime";

        String ARRIVAL_STATION_ID = "arrivalStationId";
        String ARRIVAL_STATION_NAME = "arrivalStationName";
        String ARRIVAL_TIME = "arrivalTime";

        String TRAIN_ROUTE_ID = "trainRouteId";
        String ROUTE_START_STATION_NAME = "routeStartStationId";
        String ROUTE_END_STATION_NAME = "routeEndStationId";
        String TRAIN_NAME = "trainName";

        String DATE_MSK = "dateMsk";

        String[] ARGS1 = {
                DEPARTURE_STATION_ID, DEPARTURE_STATION_NAME, DEPARTURE_TIME,
                ARRIVAL_STATION_ID, ARRIVAL_STATION_NAME, ARRIVAL_TIME,
                TRAIN_ROUTE_ID, ROUTE_START_STATION_NAME, ROUTE_END_STATION_NAME
        };
        String[] ARGS2 = {
                DEPARTURE_STATION_ID, DEPARTURE_STATION_NAME, DEPARTURE_TIME,
                ARRIVAL_STATION_ID, ARRIVAL_STATION_NAME, ARRIVAL_TIME,
                TRAIN_ROUTE_ID, ROUTE_START_STATION_NAME, ROUTE_END_STATION_NAME,
                TRAIN_NAME
        };
    }

    public static final class Caches implements CacheColumns {
        public static final String TABLE_NAME = "rzdTable";

        public static final String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME
                + "("
                + ID + " INTEGER PRIMARY KEY, "
                + DATE_MSK + " INTEGER, "
                + DEPARTURE_STATION_ID + " TEXT, "
                + DEPARTURE_STATION_NAME + " TEXT, "
                + DEPARTURE_TIME + " INTEGER, "
                + ARRIVAL_STATION_ID + " TEXT, "
                + ARRIVAL_STATION_NAME + " TEXT, "
                + ARRIVAL_TIME + " INTEGER, "
                + TRAIN_ROUTE_ID + " TEXT, "
                + ROUTE_START_STATION_NAME + " TEXT, "
                + ROUTE_END_STATION_NAME + " TEXT";

        static final String CREATE_TABLE1 = CREATE_TABLE + ")";
        static final String CREATE_TABLE2 = CREATE_TABLE + ", " + TRAIN_NAME +  " TEXT)";
    }


    private CacheContract() {}
}
