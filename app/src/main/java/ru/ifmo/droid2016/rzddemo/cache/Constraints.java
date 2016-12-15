package ru.ifmo.droid2016.rzddemo.cache;

import static ru.ifmo.droid2016.rzddemo.cache.DataSchemeVersion.V1;

/**
 * Created by vadim on 08.12.16.
 */

public class Constraints {

    static final String TEXT = "TEXT";
    static final String TABLE = "timetable";

    static final String DEPARTURE_DATE = "departure_date";
    static final String DEPARTURE_STATION_ID = "departure_station_id";
    static final String DEPARTURE_STATION_NAME = "departure_name";
    static final String DEPARTURE_TIME = "departure_time";
    static final String ARRIVAL_STATION_ID = "arrival_station_id";
    static final String ARRIVAL_STATION_NAME = "arrival_station_name";
    static final String ARRIVAL_TIME = "arrival_time";
    static final String TRAIN_ROUTE_ID = "train_route_id";
    static final String ROUTE_START_STATION_NAME = "route_start_station_name";
    static final String ROUTE_END_STATION_NAME = "route_end_station_name";
    static final String TRAIN_NAME = "train_name";

    static final String openString = String.format(
            "CREATE TABLE %s ("
            + "%s " + "TEXT, "
            + "%s " + "TEXT, "
            + "%s " + "TEXT, "
            + "%s " + "TEXT, "
            + "%s " + "TEXT, "
            + "%s " + "TEXT, "
            + "%s " + "TEXT, "
            + "%s " + "TEXT, "
            + "%s " + "TEXT, "
            + "%s " + "TEXT",
            TABLE,
            DEPARTURE_DATE,
            DEPARTURE_STATION_ID,
            DEPARTURE_STATION_NAME,
            DEPARTURE_TIME,
            ARRIVAL_STATION_ID,
            ARRIVAL_STATION_NAME,
            ARRIVAL_TIME,
            TRAIN_ROUTE_ID,
            ROUTE_START_STATION_NAME,
            ROUTE_END_STATION_NAME);

    static final String listString1 = String.format(
            "%s, %s, %s, %s, %s, %s, %s, %s, %s, %s",
            DEPARTURE_DATE,
            DEPARTURE_STATION_ID,
            DEPARTURE_STATION_NAME,
            DEPARTURE_TIME,
            ARRIVAL_STATION_ID,
            ARRIVAL_STATION_NAME,
            ARRIVAL_TIME,
            TRAIN_ROUTE_ID,
            ROUTE_START_STATION_NAME,
            ROUTE_END_STATION_NAME
    );

    static String getOpenWords(int version) {
        if (version == V1) {
            return openString + ")";
        } else {
            return openString + ", " + TRAIN_NAME + " " + TEXT + ")";
        }
    }
}
