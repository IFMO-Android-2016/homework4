package ru.ifmo.droid2016.rzddemo.cache;

import android.provider.BaseColumns;

/**
 * Created by Anna Kopeliovich on 07.12.2016.
 */

public class TimetableContract {
    public interface TimetableColumns extends BaseColumns {
        String DEPARTURE_DATE = "departureDate";

        String DEPARTURE_STATION_ID = "departureStationId";

        String DEPATURE_STATION_NAME = "departureStationName";

        String DEPATURE_TIME = "departureTime";

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

        static final String ARGUMENTS_ON_CREATE_TABLE = DEPARTURE_DATE + ", "
                + DEPARTURE_STATION_ID + ", "
                + DEPATURE_STATION_NAME + ", "
                + DEPATURE_TIME + ", "
                + ARRIVAL_STATION_ID + ", "
                + ARRIVAL_STATION_NAME + ", "
                + ARRIVAL_TIME + ", "
                + TRAIN_ROUTE_ID + ", "
                + ROUTE_START_STATION_NAME + ", "
                + ROUTE_END_STATION_NAME;

        static final String[] NAME_OF_ARGUMENTS_V1= {DEPARTURE_STATION_ID,
                DEPATURE_STATION_NAME,
                DEPATURE_TIME,
                ARRIVAL_STATION_ID,
                ARRIVAL_STATION_NAME,
                ARRIVAL_TIME,
                TRAIN_ROUTE_ID,
                ROUTE_START_STATION_NAME,
                ROUTE_END_STATION_NAME};

        static final String[] NAME_OF_ARGUMENTS_V2= {DEPARTURE_STATION_ID,
                DEPATURE_STATION_NAME,
                DEPATURE_TIME,
                ARRIVAL_STATION_ID,
                ARRIVAL_STATION_NAME,
                ARRIVAL_TIME,
                TRAIN_ROUTE_ID,
                ROUTE_START_STATION_NAME,
                ROUTE_END_STATION_NAME,
                TRAIN_NAME};

        static final String CREATE_TABLE = "CREATE TABLE " + TABLE
                + " ("
                + _ID + " INTEGER PRIMARY KEY, "
                + DEPARTURE_DATE + " INTEGER, "
                + DEPARTURE_STATION_ID + " TEXT, "
                + DEPATURE_STATION_NAME + " TEXT, "
                + DEPATURE_TIME + " INTEGER, "
                + ARRIVAL_STATION_ID + " TEXT, "
                + ARRIVAL_STATION_NAME + " TEXT, "
                + ARRIVAL_TIME + " INTEGER, "
                + TRAIN_ROUTE_ID + " TEXT, "
                + ROUTE_START_STATION_NAME + " TEXT, "
                + ROUTE_END_STATION_NAME + " TEXT";


        static final String CREATE_TABLE_V1 = CREATE_TABLE + ")";

        static final String CREATE_TABLE_V2 = CREATE_TABLE + ", " + TRAIN_NAME + " TEXT)";
    }

    private TimetableContract() {}
}