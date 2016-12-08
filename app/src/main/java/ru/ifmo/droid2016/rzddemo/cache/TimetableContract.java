package ru.ifmo.droid2016.rzddemo.cache;

import android.provider.BaseColumns;

/**
 * Created by penguinni on 07.12.16.
 */

class TimetableContract {
    public interface TimetableColumns extends BaseColumns {
        String DEPARTURE_DATE = "departure_date";
        String DEPARTURE_TIME = "departure_time";
        String DEPARTURE_STATION_NAME = "departure_station_name";
        String DEPARTURE_STATION_ID = "departure_station_id";

        String ARRIVAL_TIME = "arrival_time";
        String ARRIVAL_STATION_NAME = "arrival_station_name";
        String ARRIVAL_STATION_ID = "arrival_station_id";

        String ROUTE_START_STATION_NAME = "route_start_station_name";
        String ROUTE_END_STATION_NAME = "route_end_station_name";
        String TRAIN_ROUTE_ID = "train_route_id";
        String TRAIN_NAME = "train_name";
    }

    public interface TimetableColumnsIndices {
        int DEPARTURE_DATE = 1;
        int DEPARTURE_TIME = 2;
        int DEPARTURE_STATION_NAME = 3;
        int DEPARTURE_STATION_ID = 4;

        int ARRIVAL_TIME = 5;
        int ARRIVAL_STATION_NAME = 6;
        int ARRIVAL_STATION_ID = 7;

        int ROUTE_START_STATION_NAME = 8;
        int ROUTE_END_STATION_NAME = 9;
        int TRAIN_ROUTE_ID = 10;
        int TRAIN_NAME = 11;
    }

    public static final class Timetable implements TimetableColumns {
        static final String TABLE = "timetable";

        private static final String[] V1_ARGS_ARR = {_ID, DEPARTURE_DATE, DEPARTURE_TIME, DEPARTURE_STATION_NAME, DEPARTURE_STATION_ID,
                ARRIVAL_TIME, ARRIVAL_STATION_NAME, ARRIVAL_STATION_ID,
                ROUTE_START_STATION_NAME, ROUTE_END_STATION_NAME, TRAIN_ROUTE_ID};

        private static final String[] V2_ARGS_ARR = {_ID, DEPARTURE_DATE, DEPARTURE_TIME, DEPARTURE_STATION_NAME, DEPARTURE_STATION_ID,
                ARRIVAL_TIME, ARRIVAL_STATION_NAME, ARRIVAL_STATION_ID,
                ROUTE_START_STATION_NAME, ROUTE_END_STATION_NAME, TRAIN_ROUTE_ID, TRAIN_NAME};

        private static final String BASE_FIELDS =
                        _ID + " INTEGER PRIMARY KEY, "

                        + DEPARTURE_DATE + " TEXT, "
                        + DEPARTURE_TIME + " TEXT, "
                        + DEPARTURE_STATION_NAME + " TEXT, "
                        + DEPARTURE_STATION_ID + " TEXT, "

                        + ARRIVAL_TIME + " TEXT, "
                        + ARRIVAL_STATION_NAME + " TEXT, "
                        + ARRIVAL_STATION_ID + " TEXT, "

                        + ROUTE_START_STATION_NAME + " TEXT, "
                        + ROUTE_END_STATION_NAME + " TEXT, "
                        + TRAIN_ROUTE_ID + " TEXT";

        public static String create(String name, int version) {
            String res = "CREATE TABLE " + name + " (" + BASE_FIELDS;
            switch (version) {
                case DataSchemeVersion.V1:
                    return res + ")";
                case DataSchemeVersion.V2:
                    return res + ", " + TRAIN_NAME + " TEXT)";
                default:
                    return null;
            }
        }

        public static String[] getArgsArr(int version) {
            switch (version) {
                case DataSchemeVersion.V1:
                    return V1_ARGS_ARR;
                case DataSchemeVersion.V2:
                    return V2_ARGS_ARR;
                default:
                    return null;
            }
        }

        public static String getArgsStr(int version) {
            String res = "";

            switch (version) {
                case DataSchemeVersion.V1:
                    for (int i = 0; i < V1_ARGS_ARR.length - 1; i++) {
                        res += V1_ARGS_ARR[i] + ", ";
                    }
                    return res + V1_ARGS_ARR[V1_ARGS_ARR.length - 1];

                case DataSchemeVersion.V2:
                    for (int i = 0; i < V2_ARGS_ARR.length - 1; i++) {
                        res += V2_ARGS_ARR[i] + ", ";
                    }
                    return res + V2_ARGS_ARR[V2_ARGS_ARR.length - 1];

                default:
                    return res;
            }
        }
    }
}
