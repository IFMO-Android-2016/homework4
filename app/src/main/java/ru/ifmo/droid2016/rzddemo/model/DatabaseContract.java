package ru.ifmo.droid2016.rzddemo.model;

import android.provider.BaseColumns;

/**
 * Created by Анатолий on 07.12.2016.
 */

public class DatabaseContract {

    public DatabaseContract() {
    }

    public interface Columns extends BaseColumns {

        String NAME = "TTable";
        String DATE = "date";
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
    }

}
