package ru.ifmo.droid2016.rzddemo.cache;

/**
 * Created by Andrey on 08.12.2016.
 */

public class TimetableContract {
    public static final String TABLE = "timetable";
    public static final String TEMP_TABLE = "temptable";
    static final String ID = "id";
    static final String FROM_STATION_ID = "fromStationId";
    static final String TO_STATION_ID = "toStationId";
    static final String DATE_MSK = "dateMsk";
    static final String DEPARTURE_STATION_ID = "departureStationId";
    static final String DEPARTURE_STATION_NAME = "departureStationName";
    static final String DEPARTURE_TIME = "departureTime";
    static final String ARRIVAL_STATION_ID = "arrivalStationId";
    static final String ARRIVAL_STATION_NAME = "arrivalStationName";
    static final String ARRIVAL_TIME = "arrivalTime";
    static final String TRAIN_ROUTE_ID = "trainRouteId";
    static final String TRAIN_NAME = "trainName";
    static final String ROUTE_START_STATION_NAME = "routeStartStationName";
    static final String ROUTE_END_STATION_NAME = "routeEndStationName";
    static final String[] V1Fields = {
            ID, FROM_STATION_ID, TO_STATION_ID, DATE_MSK, DEPARTURE_STATION_ID, DEPARTURE_STATION_NAME, DEPARTURE_TIME,
            ARRIVAL_STATION_ID, ARRIVAL_STATION_NAME, ARRIVAL_TIME, TRAIN_ROUTE_ID, ROUTE_START_STATION_NAME, ROUTE_END_STATION_NAME
    };
    static final String[] V2Fields = {
            ID, FROM_STATION_ID, TO_STATION_ID, DATE_MSK, DEPARTURE_STATION_ID, DEPARTURE_STATION_NAME, DEPARTURE_TIME,
            ARRIVAL_STATION_ID, ARRIVAL_STATION_NAME, ARRIVAL_TIME, TRAIN_ROUTE_ID, ROUTE_START_STATION_NAME, ROUTE_END_STATION_NAME, TRAIN_NAME
    };
    static final String CREATE_TABLE_V2 = "CREATE TABLE " + TABLE + "(" +
            ID + " INTEGER PRIMARY KEY, " +
            FROM_STATION_ID + " TEXT, " +
            TO_STATION_ID + " TEXT, " +
            DATE_MSK + " INTEGER, " +
            DEPARTURE_STATION_ID + " TEXT, " +
            DEPARTURE_STATION_NAME + " TEXT, " +
            DEPARTURE_TIME + " INTEGER, " +
            ARRIVAL_STATION_ID + " TEXT, " +
            ARRIVAL_STATION_NAME + " TEXT, " +
            ARRIVAL_TIME + " INTEGER, " +
            TRAIN_ROUTE_ID + " TEXT, " +
            ROUTE_START_STATION_NAME + " TEXT, " +
            ROUTE_END_STATION_NAME + " TEXT, " +
            TRAIN_NAME + " TEXT" +
            ")";

    static final String CREATE_TABLE_V1 = "CREATE TABLE " + TABLE + "(" +
            ID + " INTEGER PRIMARY KEY, " +
            FROM_STATION_ID + " TEXT, " +
            TO_STATION_ID + " TEXT, " +
            DATE_MSK + " INTEGER, " +
            DEPARTURE_STATION_ID + " TEXT, " +
            DEPARTURE_STATION_NAME + " TEXT, " +
            DEPARTURE_TIME + " INTEGER, " +
            ARRIVAL_STATION_ID + " TEXT, " +
            ARRIVAL_STATION_NAME + " TEXT, " +
            ARRIVAL_TIME + " INTEGER, " +
            TRAIN_ROUTE_ID + " TEXT, " +
            ROUTE_START_STATION_NAME + " TEXT, " +
            ROUTE_END_STATION_NAME + " TEXT" +
            ")";

    static final String CREATE_TABLE_TEMP = "CREATE TABLE " + TEMP_TABLE + "(" +
            ID + " INTEGER PRIMARY KEY, " +
            FROM_STATION_ID + " TEXT, " +
            TO_STATION_ID + " TEXT, " +
            DATE_MSK + " INTEGER, " +
            DEPARTURE_STATION_ID + " TEXT, " +
            DEPARTURE_STATION_NAME + " TEXT, " +
            DEPARTURE_TIME + " INTEGER, " +
            ARRIVAL_STATION_ID + " TEXT, " +
            ARRIVAL_STATION_NAME + " TEXT, " +
            ARRIVAL_TIME + " INTEGER, " +
            TRAIN_ROUTE_ID + " TEXT, " +
            ROUTE_START_STATION_NAME + " TEXT, " +
            ROUTE_END_STATION_NAME + " TEXT" +
            ")";
}
