package ru.ifmo.droid2016.rzddemo.cache;

public class TimetableContract {

    public interface TimetableColumns {

        String ID = "id";
        String FROM_STATION_ID = "fromStationId";
        String TO_STATION_ID = "toStationId";
        String DATE_MSK = "dateMsk";
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

    public static final class Timetables implements TimetableColumns {

        static final String[] argumentsV1 = {
                ID, FROM_STATION_ID, TO_STATION_ID, DATE_MSK, DEPARTURE_STATION_ID, DEPARTURE_STATION_NAME, DEPARTURE_TIME,
                ARRIVAL_STATION_ID, ARRIVAL_STATION_NAME, ARRIVAL_TIME, TRAIN_ROUTE_ID, ROUTE_START_STATION_NAME, ROUTE_END_STATION_NAME
        };

        static final String[] argumentsV2 = {
                ID, FROM_STATION_ID, TO_STATION_ID, DATE_MSK, DEPARTURE_STATION_ID, DEPARTURE_STATION_NAME, DEPARTURE_TIME,
                ARRIVAL_STATION_ID, ARRIVAL_STATION_NAME, ARRIVAL_TIME, TRAIN_ROUTE_ID, ROUTE_START_STATION_NAME, ROUTE_END_STATION_NAME, TRAIN_NAME
        };

        public static final String TABLE = "timetable";
        public static final String TEMP_TABLE = "temptable";

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
}