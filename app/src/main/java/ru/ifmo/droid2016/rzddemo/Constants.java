package ru.ifmo.droid2016.rzddemo;

import java.text.SimpleDateFormat;

import static android.provider.BaseColumns._ID;

/**
 * Created by dmitry.trunin on 08.11.2016.
 */

public final class Constants {

    public static final String TAG = "RZDDemo";

    public static final SimpleDateFormat LOG_DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy");

    public static final String DEMO_FROM_STATION_ID = "2004000";
    public static final String DEMO_FROM_STATION_NAME = "САНКТ-ПЕТЕРБУРГ";
    public static final String DEMO_TO_STATION_ID = "2000000";
    public static final String DEMO_TO_STATION_NAME = "МОСКВА";

    public static final String TIMETABLE_TABLE_NAME = "timetable";
    public static final String DEPARTURE_DATE = "departureDate";
    public static final String DEPARTURE_STATION_ID = "departureStationId";
    public static final String DEPARTURE_STATION_NAME = "departureStationName";
    public static final String DEPARTURE_TIME = "departureTime";
    public static final String ARRIVAL_STATION_ID = "arrivalStationId";
    public static final String ARRIVAL_STATION_NAME = "arrivalStationName";
    public static final String ARRIVAL_TIME = "arrivalTime";
    public static final String TRAIN_ROUTE_ID = "trainRouteId";
    public static final String TRAIN_NAME = "trainName";
    public static final String ROUTE_START_STATION_NAME = "routeStartStationName";
    public static final String ROUTE_END_STATION_NAME = "routeEndStationName";

    public static final String TABLE_CREATE_REQUEST = "CREATE TABLE " + TIMETABLE_TABLE_NAME + " (" +
            _ID                     + " INTEGER PRIMARY KEY, " +
            DEPARTURE_DATE          + " INTEGER, " +
            DEPARTURE_STATION_ID    + " INTEGER, " +
            DEPARTURE_STATION_NAME  + " TEXT, " +
            DEPARTURE_TIME          + " INTEGER, " +
            ARRIVAL_STATION_ID      + " TEXT, " +
            ARRIVAL_STATION_NAME    + " TEXT, " +
            ARRIVAL_TIME            + " INTEGER, " +
            TRAIN_ROUTE_ID          + " TEXT, " +
            ROUTE_START_STATION_NAME+ " TEXT, " +
            ROUTE_END_STATION_NAME   + " TEXT";

    public static final String DB_V1 = TABLE_CREATE_REQUEST + ")";
    public static final String DB_V2 = TABLE_CREATE_REQUEST + ", " + TRAIN_NAME + " TEXT)";

    public static final String[] V1_COMPONENTS = {DEPARTURE_STATION_ID, DEPARTURE_STATION_NAME, DEPARTURE_TIME, ARRIVAL_STATION_ID,

            ARRIVAL_STATION_NAME, ARRIVAL_TIME, TRAIN_ROUTE_ID, ROUTE_START_STATION_NAME, ROUTE_END_STATION_NAME};

    public static final String[] V2_COMPONENTS = {DEPARTURE_STATION_ID, DEPARTURE_STATION_NAME, DEPARTURE_TIME, ARRIVAL_STATION_ID,

            ARRIVAL_STATION_NAME, ARRIVAL_TIME, TRAIN_ROUTE_ID, ROUTE_START_STATION_NAME, ROUTE_END_STATION_NAME, TRAIN_NAME};


    private Constants() {}
}
