package ru.ifmo.droid2016.rzddemo.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

/**
 * Created by Октай on 08.12.2016.
 */

public class TrainScheduleDbHelper extends SQLiteOpenHelper {

    public static int DATABASE_VERSION;
    public static final String DATABASE_NAME = "TrainSchedule.db";
    private static final String TEXT_TYPE = " TEXT";
    private static final String COMMA_SEP = ",";

    private static final String SQL_CREATE_ENTRIES_VERSION_1 =
            "CREATE TABLE " + TrainScheduleContract.FeedEntry.TABLE_NAME + " (" +
                    TrainScheduleContract.FeedEntry._ID + " INTEGER PRIMARY KEY," +
                    TrainScheduleContract.FeedEntry.COLUMN_NAME_DATE + TEXT_TYPE + COMMA_SEP +
                    TrainScheduleContract.FeedEntry.COLUMN_NAME_DEPARTURE_STATION_ID + TEXT_TYPE + COMMA_SEP +
                    TrainScheduleContract.FeedEntry.COLUMN_NAME_DEPARTURE_STATION_NAME + TEXT_TYPE + COMMA_SEP +
                    TrainScheduleContract.FeedEntry.COLUMN_NAME_DEPARTURE_TIME + TEXT_TYPE + COMMA_SEP +
                    TrainScheduleContract.FeedEntry.COLUMN_NAME_ARRIVAL_STATION_ID + TEXT_TYPE + COMMA_SEP +
                    TrainScheduleContract.FeedEntry.COLUMN_NAME_ARRIVAL_STATION_NAME + TEXT_TYPE + COMMA_SEP +
                    TrainScheduleContract.FeedEntry.COLUMN_NAME_ARRIVAL_TIME + TEXT_TYPE + COMMA_SEP +
                    TrainScheduleContract.FeedEntry.COLUMN_NAME_TRAIN_ROUTE_ID + TEXT_TYPE + COMMA_SEP +
                    TrainScheduleContract.FeedEntry.COLUMN_NAME_ROUTE_START_STATION_NAME + TEXT_TYPE + COMMA_SEP +
                    TrainScheduleContract.FeedEntry.COLUMN_NAME_ROUTE_END_STATION_NAME + TEXT_TYPE +
                    " )";

    private static final String SQL_CREATE_ENTRIES_VERSION_2 =
            "CREATE TABLE " + TrainScheduleContract.FeedEntry.TABLE_NAME + " (" +
                    TrainScheduleContract.FeedEntry._ID + " INTEGER PRIMARY KEY," +
                    TrainScheduleContract.FeedEntry.COLUMN_NAME_DATE + TEXT_TYPE + COMMA_SEP +
                    TrainScheduleContract.FeedEntry.COLUMN_NAME_DEPARTURE_STATION_ID + TEXT_TYPE + COMMA_SEP +
                    TrainScheduleContract.FeedEntry.COLUMN_NAME_DEPARTURE_STATION_NAME + TEXT_TYPE + COMMA_SEP +
                    TrainScheduleContract.FeedEntry.COLUMN_NAME_DEPARTURE_TIME + TEXT_TYPE + COMMA_SEP +
                    TrainScheduleContract.FeedEntry.COLUMN_NAME_ARRIVAL_STATION_ID + TEXT_TYPE + COMMA_SEP +
                    TrainScheduleContract.FeedEntry.COLUMN_NAME_ARRIVAL_STATION_NAME + TEXT_TYPE + COMMA_SEP +
                    TrainScheduleContract.FeedEntry.COLUMN_NAME_ARRIVAL_TIME + TEXT_TYPE + COMMA_SEP +
                    TrainScheduleContract.FeedEntry.COLUMN_NAME_TRAIN_ROUTE_ID + TEXT_TYPE + COMMA_SEP +
                    TrainScheduleContract.FeedEntry.COLUMN_NAME_TRAIN_NAME + TEXT_TYPE + COMMA_SEP +
                    TrainScheduleContract.FeedEntry.COLUMN_NAME_ROUTE_START_STATION_NAME + TEXT_TYPE + COMMA_SEP +
                    TrainScheduleContract.FeedEntry.COLUMN_NAME_ROUTE_END_STATION_NAME + TEXT_TYPE +
                    " )";

    private static final String OLD_COLUMNS = TrainScheduleContract.FeedEntry.COLUMN_NAME_DATE + COMMA_SEP +
            TrainScheduleContract.FeedEntry.COLUMN_NAME_DEPARTURE_STATION_ID + COMMA_SEP +
            TrainScheduleContract.FeedEntry.COLUMN_NAME_DEPARTURE_STATION_NAME + COMMA_SEP +
            TrainScheduleContract.FeedEntry.COLUMN_NAME_DEPARTURE_TIME + COMMA_SEP +
            TrainScheduleContract.FeedEntry.COLUMN_NAME_ARRIVAL_STATION_ID + COMMA_SEP +
            TrainScheduleContract.FeedEntry.COLUMN_NAME_ARRIVAL_STATION_NAME + COMMA_SEP +
            TrainScheduleContract.FeedEntry.COLUMN_NAME_ARRIVAL_TIME + COMMA_SEP +
            TrainScheduleContract.FeedEntry.COLUMN_NAME_TRAIN_ROUTE_ID + COMMA_SEP +
            TrainScheduleContract.FeedEntry.COLUMN_NAME_ROUTE_START_STATION_NAME + COMMA_SEP +
            TrainScheduleContract.FeedEntry.COLUMN_NAME_ROUTE_END_STATION_NAME;

    private static final String SQL_DELETE =
            "DROP TABLE IF EXISTS ";

    public TrainScheduleDbHelper(Context context, int version) {
        super(context, DATABASE_NAME, null, version);
        this.DATABASE_VERSION = version;
    }

    public void onCreate(SQLiteDatabase db) {
        onCreateWithVersion(db, DATABASE_VERSION);
    }

    private void onCreateWithVersion(SQLiteDatabase db, int databaseVersion) {
         if (databaseVersion == 1) {
            db.execSQL(SQL_CREATE_ENTRIES_VERSION_1);
        } else if (databaseVersion == 2) {
            db.execSQL(SQL_CREATE_ENTRIES_VERSION_2);
        }
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(
                "ALTER TABLE " + TrainScheduleContract.FeedEntry.TABLE_NAME + " " + "ADD COLUMN " +
                        TrainScheduleContract.FeedEntry.COLUMN_NAME_TRAIN_NAME + " TEXT");
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        final String DOWNGRADE_TABLE_NAME = TrainScheduleContract.FeedEntry.TABLE_NAME + "_downgrade";

        db.execSQL(
                "ALTER TABLE " + TrainScheduleContract.FeedEntry.TABLE_NAME + " " +
                        "RENAME TO " + DOWNGRADE_TABLE_NAME);

        onCreateWithVersion(db, 1);

        db.execSQL(
                "INSERT INTO " + TrainScheduleContract.FeedEntry.TABLE_NAME + " (" + OLD_COLUMNS + ") " +
                        "SELECT " + OLD_COLUMNS + " " +
                        "FROM " + DOWNGRADE_TABLE_NAME);

        db.execSQL(SQL_DELETE + DOWNGRADE_TABLE_NAME);
    }


    public static final class TrainScheduleContract {
        public TrainScheduleContract() {
        }

        public static abstract class FeedEntry implements BaseColumns {
            public static final String TABLE_NAME = "train_schedule";
            public static final String COLUMN_NAME_DATE = "date";
            public static final String COLUMN_NAME_DEPARTURE_STATION_ID = "departure_station_id";
            public static final String COLUMN_NAME_DEPARTURE_STATION_NAME = "departure_station_name";
            public static final String COLUMN_NAME_DEPARTURE_TIME = "departure_time";
            public static final String COLUMN_NAME_ARRIVAL_STATION_ID = "arrival_station_id";
            public static final String COLUMN_NAME_ARRIVAL_STATION_NAME = "arrival_station_name";
            public static final String COLUMN_NAME_ARRIVAL_TIME = "arrival_time";
            public static final String COLUMN_NAME_TRAIN_ROUTE_ID = "train_route_id";
            public static final String COLUMN_NAME_TRAIN_NAME = "train_name";
            public static final String COLUMN_NAME_ROUTE_START_STATION_NAME = "route_start_station_name";
            public static final String COLUMN_NAME_ROUTE_END_STATION_NAME = "route_end_station_name";
        }
    }

}
