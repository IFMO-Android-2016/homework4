package ru.ifmo.droid2016.rzddemo.db;


/**
 * Created by Disa on 16.12.2016.
 */


public class TimetableContract {

    public interface TimetableColumns {

        String TIMETABLE_ID = "_id";

        String DEPARTURE_DATE = "departureDate";

        String DEPARTURE_STATION_ID = "departureStationId";

        String DEPARTURE_STATION_NAME = "departureStationName";

        String DEPARTURE_TIME = "departureTime";

        String ARRIVAL_STATION_ID = "arrivalStationId";

        String ARRIVAL_STATION_NAME = "arrivalStationName";

        String ARRIVAL_TIME = "arrivalTime";

        String TRAIN_ROUTE_ID = "trainRouteId";

        String TRAIN_NAME = "trainName";

        String ROUTE_START_STATION_NAME = "routeStartStationName";

        String ROUTE_END_STATION_NAME = "routeEndStationName";
    }

    public static final class Timetable implements TimetableColumns {

        public static final String TABLE = "timetable";

        private static final String CREATE_TABLE = "CREATE TABLE " + TABLE
                + " ("
                + TIMETABLE_ID + " INTEGER PRIMARY KEY, "
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

        public static final String CREATE_TABLE_V1 = CREATE_TABLE
                + " )";

        public static final String CREATE_TABLE_V2 = CREATE_TABLE
                + ", " + TRAIN_NAME + " TEXT )";
    }
}