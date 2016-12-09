package ru.ifmo.droid2016.rzddemo.cache;

import static   android.provider.BaseColumns.*;

/**
 * Определения реляционной модели данных городов.
 */
public final class TimetableCacheContract {

    /**
     * Колонки таблицы гордов.
     *
     * Из базового интерфейса BaseColumn используется колонка _ID -- ID строки SQLite таблицы,
     * в котором хранится ID города.
     */
    public interface TimetableColumns {
        String ROUTE_DATE = "route_date";
        String DEPARTURE_STATION_ID = "departure_station_id";
        String DEPARTURE_STATION_NAME = "departure_station_name";
        String DEPARTURE_TIME = "departure_time";
        String ARRIVAL_STATION_ID = "arrival_station_id";
        String ARRIVAL_STATION_NAME = "arrival_station_name";
        String ARRIVAL_TIME = "arrival_time";
        String TRAIN_ROUTE_ID = "train_route_id";
        String TRAIN_NAME = "train_name";
        String ROUTE_START_STATION_NAME = "route_start_station_name";
        String ROUTE_END_STATION_NAME = "route_end_station_name";
        String[] Everything_V1 = {DEPARTURE_STATION_ID, DEPARTURE_STATION_NAME, DEPARTURE_TIME, ARRIVAL_STATION_ID, ARRIVAL_STATION_NAME, ARRIVAL_TIME, TRAIN_ROUTE_ID, ROUTE_START_STATION_NAME, ROUTE_END_STATION_NAME };
        String[] Everything_V2 = {DEPARTURE_STATION_ID, DEPARTURE_STATION_NAME, DEPARTURE_TIME, ARRIVAL_STATION_ID, ARRIVAL_STATION_NAME, ARRIVAL_TIME, TRAIN_ROUTE_ID, ROUTE_START_STATION_NAME, ROUTE_END_STATION_NAME, TRAIN_NAME};

    }

    public static final class Timetable implements TimetableColumns {

        public static final String TABLE = "timetable";


        static final String CREATE_TABLE_V1 = "CREATE TABLE " + TABLE
                + " ("
                + _ID + " INTEGER_PRIMARY_KEY, "
                + ROUTE_DATE + " INTEGER, "
                + DEPARTURE_STATION_ID + " TEXT, "
                + DEPARTURE_STATION_NAME + " TEXT, "
                + DEPARTURE_TIME + " INTEGER, "
                + ARRIVAL_STATION_ID + " TEXT, "
                + ARRIVAL_STATION_NAME + " TEXT, "
                + ARRIVAL_TIME + " INTEGER, "
                + TRAIN_ROUTE_ID + " TEXT, "
                + ROUTE_START_STATION_NAME + " TEXT, "
                + ROUTE_END_STATION_NAME + " TEXT)";

        static final String CREATE_TABLE_V2 = "CREATE TABLE " + TABLE
                + " ("
                + _ID + " INTEGER_PRIMARY_KEY, "
                + ROUTE_DATE + " INTEGER, "
                + DEPARTURE_STATION_ID + " TEXT, "
                + DEPARTURE_STATION_NAME + " TEXT, "
                + DEPARTURE_TIME + " INTEGER, "
                + ARRIVAL_STATION_ID + " TEXT, "
                + ARRIVAL_STATION_NAME + " TEXT, "
                + ARRIVAL_TIME + " INTEGER, "
                + TRAIN_ROUTE_ID + " TEXT, "
                + ROUTE_START_STATION_NAME + " TEXT, "
                + ROUTE_END_STATION_NAME + " TEXT"
                + TRAIN_NAME + " TEXT)";

        static final String CREATE_TABLE_TEMP = "CREATE TABLE " + TABLE + "_TEMP"
                + " ("
                + _ID + " INTEGER_PRIMARY_KEY, "
                + ROUTE_DATE + " INTEGER, "
                + DEPARTURE_STATION_ID + " TEXT, "
                + DEPARTURE_STATION_NAME + " TEXT, "
                + DEPARTURE_TIME + " INTEGER, "
                + ARRIVAL_STATION_ID + " TEXT, "
                + ARRIVAL_STATION_NAME + " TEXT, "
                + ARRIVAL_TIME + " INTEGER, "
                + TRAIN_ROUTE_ID + " TEXT, "
                + ROUTE_START_STATION_NAME + " TEXT, "
                + ROUTE_END_STATION_NAME + " TEXT)";

    }

    private TimetableCacheContract() {}
}
