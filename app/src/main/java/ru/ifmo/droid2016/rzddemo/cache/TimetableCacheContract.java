package ru.ifmo.droid2016.rzddemo.cache;

import android.provider.BaseColumns;

final class TimetableCacheContract {

    private interface TimetableColumns extends BaseColumns {

        String DEPARTURE_DATE = "departure_date";

        String DEPARTURE_STATION_ID = "departure_station_id";

        String DEPARTURE_STATION_NAME = "departure_name";

        String DEPARTURE_TIME = "departure_time";

        String ARRIVAL_STATION_ID = "arrival_station_id";

        String ARRIVAL_STATION_NAME = "arrival_Station_Name";

        String ARRIVAL_TIME = "arrival_time";

        String TRAIN_ROUTE_ID = "train_route_id";

        String TRAIN_NAME = "train_name";

        String ROUTE_START_STATION_NAME = "route_start_station_name";

        String ROUTE_END_STATION_NAME = "route_end_station_name";


        String[] V1_COMPONENTS = {DEPARTURE_STATION_ID, DEPARTURE_STATION_NAME, DEPARTURE_TIME, ARRIVAL_STATION_ID,

                ARRIVAL_STATION_NAME, ARRIVAL_TIME, TRAIN_ROUTE_ID, ROUTE_START_STATION_NAME, ROUTE_END_STATION_NAME};

        String[] V2_COMPONENTS = {DEPARTURE_STATION_ID, DEPARTURE_STATION_NAME, DEPARTURE_TIME, ARRIVAL_STATION_ID,

                ARRIVAL_STATION_NAME, ARRIVAL_TIME, TRAIN_ROUTE_ID, ROUTE_START_STATION_NAME, ROUTE_END_STATION_NAME, TRAIN_NAME};
    }

    static final class Timetable implements TimetableColumns {

        static final String TABLE = "timetable";

        static final String CREATE_TABLE = String.format("CREATE TABLE %s ("
                        + "%s INTEGER PRIMARY KEY, "
                        + "%s INTEGER, "
                        + "%s TEXT, "
                        + "%s TEXT, "
                        + "%s INTEGER, "
                        + "%s TEXT, "
                        + "%s TEXT, "
                        + "%s INTEGER, "
                        + "%s TEXT, "
                        + "%s TEXT, "
                        + "%s TEXT",
                TABLE, _ID, DEPARTURE_DATE, DEPARTURE_STATION_ID, DEPARTURE_STATION_NAME,
                DEPARTURE_TIME, ARRIVAL_STATION_ID, ARRIVAL_STATION_NAME, ARRIVAL_TIME,
                TRAIN_ROUTE_ID, ROUTE_START_STATION_NAME, ROUTE_END_STATION_NAME);

        static final String V1_TABLE = CREATE_TABLE + ")";

        static final String V2_TABLE = CREATE_TABLE + ", " + TRAIN_NAME + " TEXT)";
    }

    private TimetableCacheContract() {
    }
}