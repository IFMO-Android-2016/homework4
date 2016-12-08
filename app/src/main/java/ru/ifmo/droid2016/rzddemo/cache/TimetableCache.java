package ru.ifmo.droid2016.rzddemo.cache;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.support.annotation.AnyThread;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;

import java.io.FileNotFoundException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import ru.ifmo.droid2016.rzddemo.model.TimetableEntry;
import ru.ifmo.droid2016.rzddemo.utils.TimeUtils;

import static ru.ifmo.droid2016.rzddemo.Constants.LOG_DATE_FORMAT;
import static ru.ifmo.droid2016.rzddemo.cache.DataSchemeVersion.*;

public class TimetableCache {

    static final String DATE = "date";
    static final String DEPARTURE_ID = "departure_id";
    static final String DEPARTURE_NAME = "departure_name";
    static final String DEPARTURE_TIME = "departure_time";
    static final String ROUTE_END_STATION_NAME = "route_end_station_name";
    static final String ROUTE_START_STATION_NAME = "route_start_station_name";
    static final String STATION_ID = "station_id";
    static final String STATION_NAME = "station_name";
    static final String STATION_TIME = "station_time";
    static final String TABLE_NAME = "schedule";
    static final String TRAIN_NAME = "train_name";
    static final String TRAIN_ROUTE_ID = "train_route_id";

    static final String[] firstList = {
            DEPARTURE_ID,
            DEPARTURE_NAME,
            DEPARTURE_TIME,
            STATION_ID,
            STATION_NAME,
            STATION_TIME,
            TRAIN_ROUTE_ID,
            ROUTE_START_STATION_NAME,
            ROUTE_END_STATION_NAME
    };

    static final String[] secondList = {
            DEPARTURE_ID,
            DEPARTURE_NAME,
            DEPARTURE_TIME,
            STATION_ID,
            STATION_NAME,
            STATION_TIME,
            TRAIN_ROUTE_ID,
            TRAIN_NAME,
            ROUTE_START_STATION_NAME,
            ROUTE_END_STATION_NAME
    };

    @DataSchemeVersion
    private final int version;
    private final DatabaseHandler databaseHandler;
    private final SimpleDateFormat schemaDate;
    private final SimpleDateFormat schemaDateTime;

    @AnyThread
    public TimetableCache(@NonNull Context context,
                          @DataSchemeVersion int version) {
        this.version = version;
        this.databaseHandler = new DatabaseHandler(context.getApplicationContext(), this.version);
        this.schemaDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        this.schemaDateTime = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault());
    }

    @WorkerThread
    @NonNull
    public List<TimetableEntry> get(@NonNull String fromStationId,
                                    @NonNull String toStationId,
                                    @NonNull Calendar dateMsk)
            throws FileNotFoundException {

        SQLiteDatabase db = databaseHandler.getReadableDatabase();

        Cursor cursor = db.query(
                TABLE_NAME,
                (version == V2 ? secondList : firstList),
                DATE + "=? AND "
                        + DEPARTURE_ID + "=? AND "
                        + STATION_ID + "=?",
                new String[]{
                        schemaDate.format(dateMsk.getTime()),
                        fromStationId,
                        toStationId
                }, null, null, null, null);

        try {
            if (cursor != null && cursor.moveToFirst()) {
                List<TimetableEntry> timetable = new ArrayList<>();

                do {
                    TimetableEntry entry;
                    try {
                        int cnt = 0;
                        entry = new TimetableEntry(
                                cursor.getString(cnt++),
                                cursor.getString(cnt++),
                                parseCalendar(cursor.getString(cnt++)),
                                cursor.getString(cnt++),
                                cursor.getString(cnt++),
                                parseCalendar(cursor.getString(cnt++)),
                                cursor.getString(cnt++),
                                ((version == V2) ? cursor.getString(cnt++) : null),
                                cursor.getString(cnt++),
                                cursor.getString(cnt)
                        );
                    } catch (ParseException e) {
                        e.printStackTrace();
                        continue; // skip entry
                    }

                    timetable.add(entry);
                } while (cursor.moveToNext());

                return timetable;
            }

            throw new FileNotFoundException("No data in timetable cache for: fromStationId="
                    + fromStationId + ", toStationId=" + toStationId
                    + ", dateMsk=" + LOG_DATE_FORMAT.format(dateMsk.getTime()));
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }
    }

    private Calendar parseCalendar(String date) throws ParseException {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeZone(TimeUtils.getMskTimeZone());
        calendar.setTime(schemaDateTime.parse(date));
        return calendar;
    }


    @WorkerThread
    public void put(@NonNull String fromStationId,
                    @NonNull String toStationId,
                    @NonNull Calendar dateMsk,
                    @NonNull List<TimetableEntry> timetable) {

        SQLiteDatabase database = databaseHandler.getWritableDatabase();
        boolean firstVersion = version == V1;
        String[] list = firstVersion ? firstList : secondList;
        String sqlQuery = "INSERT INTO " + TABLE_NAME +
                " (" + DATE + intercalate(",", list) + ") " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?" + (firstVersion ? ")" : ", ?)");

        SQLiteStatement statement = database.compileStatement(sqlQuery);
        database.beginTransaction();

        try {
            for (TimetableEntry entry : timetable) {
                int counter = 1;

                statement.bindString(counter++, schemaDate.format(dateMsk.getTime()));
                statement.bindString(counter++, entry.departureStationId);
                statement.bindString(counter++, entry.departureStationName);
                statement.bindString(counter++, schemaDateTime.format(entry.departureTime.getTime()));
                statement.bindString(counter++, entry.arrivalStationId);
                statement.bindString(counter++, entry.arrivalStationName);
                statement.bindString(counter++, schemaDateTime.format(entry.arrivalTime.getTime()));
                statement.bindString(counter++, entry.trainRouteId);

                if (version == V2) {
                    if (entry.trainName == null) {
                        statement.bindNull(counter++);
                    } else {
                        statement.bindString(counter++, entry.trainName);
                    }
                }

                statement.bindString(counter++, entry.routeStartStationName);
                statement.bindString(counter, entry.routeEndStationName);

                statement.executeInsert();
            }

            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }

        database.close();

    }

    private static class DatabaseHandler extends SQLiteOpenHelper {
        private static final String DATABASE_NAME = "rzd.timetable.db";

        @DataSchemeVersion
        private int version;

        DatabaseHandler(Context context, @DataSchemeVersion int version) {
            super(context, DATABASE_NAME, null, version);
            this.version = version;
        }

        private void createTable(SQLiteDatabase db, @DataSchemeVersion int version) {
            String[] list = version == V1 ? firstList : secondList;
            String sqlQuery = "CREATE TABLE " + TABLE_NAME + " (" + DATE;
            sqlQuery += intercalate(" TEXT, ", list) + " TEXT)";
            db.execSQL(sqlQuery);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            createTable(db, version);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL(
                    "ALTER TABLE " + TABLE_NAME + " ADD COLUMN " + TRAIN_NAME + " TEXT");
        }

        @Override
        public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            final String DOWNGRADE = TABLE_NAME + "_down";

            db.execSQL("ALTER TABLE " + TABLE_NAME + " RENAME TO " + DOWNGRADE);

            createTable(db, V1);
            String columns = intercalate(" , ", firstList);

            db.execSQL("INSERT INTO " + TABLE_NAME + " (" + columns + ") " +
                    "SELECT " + columns + " FROM " + DOWNGRADE);

            db.execSQL("DROP TABLE " + DOWNGRADE);
        }
    }

    private static String intercalate(String sep, String[] strings ) {
        if (strings == null || strings.length == 0) {
            return "";
        }

        if (sep == null) {
            sep = "";
        }
        StringBuilder builder = new StringBuilder(strings[0]);
        for(int i = 1; i < strings.length; ++i) {
            builder.append(sep);
            builder.append(strings[i]);
        }
        return builder.toString();
    }

}
