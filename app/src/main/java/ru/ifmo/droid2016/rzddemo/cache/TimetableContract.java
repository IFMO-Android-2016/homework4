package ru.ifmo.droid2016.rzddemo.cache;

import android.provider.BaseColumns;

/**
 * Created by Lenovo on 07.12.2016.
 */

class TimetableContract {
    private TimetableContract() {}

    interface TimetableColumns extends BaseColumns {
        String DEPARTUREDATE = "DEPARTUREDATE";
        String DEPARTURESTATIONID = "DEPARTURESTATIONID";
        String DEPARTURESTATIONNAME = "DEPARTURESTATIONNAME";
        String DEPARTURETIME = "DEPARTURETIME";
        String ARRIVALSTATIONID = "ARRIVALSTATIONID";
        String ARRIVALSTATIONNAME = "ARRIVALSTATIONNAME";
        String ARRIVALTIME = "ARRIVALTIME";
        String TRAINROUTEID = "TRAINROUTEID";
        String TRAINNAME = "TRAINNAME";
        String ROUTESTARTSTATIONNAME = "ROUTESTARTSTATIONNAME";
        String ROUTEENDSTATIONNAME = "ROUTEENDSTATIONNAME";
    }

    static final class Timetable implements TimetableColumns {
        static final String TABLE = "timetable";
        private static final String CREATE_TABLE =
                "CREATE TABLE " + TABLE + "("
                        + _ID + " INTEGER PRIMARY KEY, "
                        + DEPARTUREDATE + " INTEGER, "
                        + DEPARTURESTATIONID + " TEXT, "
                        + DEPARTURESTATIONNAME + " TEXT, "
                        + DEPARTURETIME + " INTEGER, "
                        + ARRIVALSTATIONID + " TEXT, "
                        + ARRIVALSTATIONNAME + " TEXT, "
                        + ARRIVALTIME + " INTEGER, "
                        + TRAINROUTEID + " TEXT, "
                        + ROUTESTARTSTATIONNAME + " TEXT, "
                        + ROUTEENDSTATIONNAME + " TEXT";
        static final String CREATETABLEV1 = CREATE_TABLE + " )";
        static final String CREATETABLEV2 = CREATE_TABLE + ", " + TRAINNAME + " TEXT )";
        static final String[] TABLEV1 = new String[]{DEPARTUREDATE, DEPARTURESTATIONID, DEPARTURESTATIONNAME,
        DEPARTURETIME, ARRIVALSTATIONID, ARRIVALSTATIONNAME, ARRIVALTIME, TRAINROUTEID, ROUTESTARTSTATIONNAME,
        ROUTEENDSTATIONNAME};

    }


}
