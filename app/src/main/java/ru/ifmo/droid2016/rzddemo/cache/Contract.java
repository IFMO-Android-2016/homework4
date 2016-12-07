package ru.ifmo.droid2016.rzddemo.cache;


import android.provider.BaseColumns;

final class Contract {

    private interface TimetableColumns extends BaseColumns {
        String ARRIVAL_STATION_ID = "arrivalStationID";
        String ARRIVAL_STATION_NAME = "arrivalStationName";
        String ARRIVAL_TIME = "arrivalTime";

        String DEPARTURE_TIME = "departureTime";
        String DEPARTURE_STATION_ID = "departureStationID";
        String DEPARTURE_DATE = "departureDate";
        String DEPARTURE_STATION_NAME = "departureName";

        String ROUTE_START_STATION_NAME = "routeStartStationName";
        String ROUTE_END_STATION_NAME = "routeEndStationName";

        String TRAIN_ROUTE_ID = "trainRouteID";
        String TRAIN_NAME = "trainName";

        String[] V1 = {DEPARTURE_STATION_ID, DEPARTURE_STATION_NAME, DEPARTURE_TIME, ARRIVAL_STATION_ID, ARRIVAL_STATION_NAME, ARRIVAL_TIME, TRAIN_ROUTE_ID, ROUTE_START_STATION_NAME, ROUTE_END_STATION_NAME };
        String[] V2 = {DEPARTURE_STATION_ID, DEPARTURE_STATION_NAME, DEPARTURE_TIME, ARRIVAL_STATION_ID, ARRIVAL_STATION_NAME, ARRIVAL_TIME, TRAIN_ROUTE_ID, ROUTE_START_STATION_NAME, ROUTE_END_STATION_NAME, TRAIN_NAME };
    }

    static final class Timetable implements TimetableColumns {
        static final String TABLE = "timetable";

        static final String CREATE_TABLE = "CREATE TABLE " + TABLE
                + " ("
                + _ID + " INTEGER PRIMARY KEY, "
                + DEPARTURE_DATE + " INTEGER, "
                + DEPARTURE_STATION_ID + " TEXT, "
                + DEPARTURE_STATION_NAME + " TEXT, "
                + DEPARTURE_TIME + " INTEGER, "
                + ARRIVAL_STATION_ID + " TEXT, "
                + ARRIVAL_STATION_NAME + " TEXT, "
                + ARRIVAL_TIME + " INTEGER, "
                + TRAIN_ROUTE_ID + " TEXT, "
                + ROUTE_START_STATION_NAME + " TEXT, "
                + ROUTE_END_STATION_NAME + " TEXT";

        static final String CREATE_TABLE_V1 = CREATE_TABLE + ")";

        static final String CREATE_TABLE_V2 = CREATE_TABLE + ", " + TRAIN_NAME + " TEXT)";
    }
}
