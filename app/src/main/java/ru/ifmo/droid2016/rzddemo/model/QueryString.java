package ru.ifmo.droid2016.rzddemo.model;

import ru.ifmo.droid2016.rzddemo.cache.DataSchemeVersion;

import static ru.ifmo.droid2016.rzddemo.model.DatabaseContract.Columns.*;

/**
 * Created by Анатолий on 07.12.2016.
 */

public class QueryString extends DatabaseContract {
    private static final String[] query1 =

            {
                    DEPARTURE_STATION_ID, DEPARTURE_STATION_NAME, DEPARTURE_TIME, ARRIVAL_STATION_ID, ARRIVAL_STATION_NAME,
                    ARRIVAL_TIME, TRAIN_ROUTE_ID, ROUTE_START_STATION_NAME, ROUTE_END_STATION_NAME
            };
    private static final String[] query2 =

            {
                    DEPARTURE_STATION_ID, DEPARTURE_STATION_NAME, DEPARTURE_TIME, ARRIVAL_STATION_ID, ARRIVAL_STATION_NAME,
                    ARRIVAL_TIME, TRAIN_ROUTE_ID, TRAIN_NAME, ROUTE_START_STATION_NAME, ROUTE_END_STATION_NAME
            };


    public static String[] getColumns(@DataSchemeVersion int ver) {
        if (ver == DataSchemeVersion.V1) {
            return query1;
        } else {
            return query2;
        }
    }
}
