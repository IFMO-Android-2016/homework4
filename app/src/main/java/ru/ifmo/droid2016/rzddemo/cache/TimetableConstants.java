package ru.ifmo.droid2016.rzddemo.cache;

final class TimetableConstants {
    static final String ID = "id";
    static final String DATE = "date";
    static final String DEPARTURE_ID = "dep_id";
    static final String DEPARTURE_NAME = "dep_name";
    static final String DEPARTURE_TIME = "dep_time";
    static final String ARRIVAL_ID = "arr_id";
    static final String ARRIVAL_NAME = "arr_name";
    static final String ARRIVAL_TIME = "arr_time";
    static final String TRAIN_ROUTE_ID = "train_route_id";
    static final String TRAIN_NAME = "train_name";
    static final String ROUTE_START_STATION_NAME = "route_start_station_name";
    static final String ROUTE_END_STATION_NAME = "route_end_station_name";
    static final String TABLE_NAME = "timetable";
    static final String[] listV1 = {
            /*DATE,*/
            DEPARTURE_ID,
            DEPARTURE_NAME,
            DEPARTURE_TIME,
            ARRIVAL_ID,
            ARRIVAL_NAME,
            ARRIVAL_TIME,
            TRAIN_ROUTE_ID,
            /*TRAIN_NAME,*/
            ROUTE_START_STATION_NAME,
            ROUTE_END_STATION_NAME
    };
    static final String[]  listV2 = {
            /*DATE,*/
            DEPARTURE_ID,
            DEPARTURE_NAME,
            DEPARTURE_TIME,
            ARRIVAL_ID,
            ARRIVAL_NAME,
            ARRIVAL_TIME,
            TRAIN_ROUTE_ID,
            TRAIN_NAME,
            ROUTE_START_STATION_NAME,
            ROUTE_END_STATION_NAME
    };
}