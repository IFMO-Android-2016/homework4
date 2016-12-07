package ru.ifmo.droid2016.rzddemo.cache;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.support.annotation.AnyThread;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;
import android.util.Log;

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
import static ru.ifmo.droid2016.rzddemo.cache.TimetableConstants.*;
import static ru.ifmo.droid2016.rzddemo.cache.TimetableConstants.listV1;

public class TimetableCache {

    @NonNull
    private final Context context;

    @DataSchemeVersion
    private final int version;
    private final DatabaseHandler databaseHandler;
    private final SimpleDateFormat schemaDate;
    private final SimpleDateFormat schemaDateTime;
    private static final String TAG = "SQL:";

    @AnyThread
    public TimetableCache(@NonNull Context context,
                          @DataSchemeVersion int version) {
        this.context = context.getApplicationContext();
        this.version = version;
        this.databaseHandler = new DatabaseHandler(this.context, this.version);
        this.schemaDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        this.schemaDateTime = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault());
    }

    /**
     * @param fromStationId ID станции отправления
     * @param toStationId   ID станции прибытия
     * @param dateMsk       дата в московском часовом поясе
     * @return - список {@link TimetableEntry}
     * @throws FileNotFoundException - если в кэше отсуствуют запрашиваемые данные.
     */
    @WorkerThread
    @NonNull
    public List<TimetableEntry> get(@NonNull String fromStationId,
                                    @NonNull String toStationId,
                                    @NonNull Calendar dateMsk)
            throws FileNotFoundException {

        SQLiteDatabase db = databaseHandler.getReadableDatabase();

        Cursor cursor = db.query(
                TABLE_NAME,
                (version == V2 ? listV2 : listV1),
                DATE + "=? AND "
                        + DEPARTURE_ID + "=? AND "
                        + ARRIVAL_ID + "=?",
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

        SQLiteDatabase db = databaseHandler.getWritableDatabase();

        AlmostStringJoiner sj = new AlmostStringJoiner(
                "INSERT INTO " + TABLE_NAME + " (",
                ",",
                ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?"
                        + ((version == V2) ? ", ?)" : ")")
        );
        sj.add(DATE);
        for (String column : version == V2 ? listV2 : listV1) sj.add(column);

        String sql = sj.toString();
        Log.e(TAG, "put: " + sql);
        SQLiteStatement ps = db.compileStatement(sql);

        db.beginTransaction();

        try {
            for (TimetableEntry entry : timetable) {
                int key = 1;

                ps.bindString(key++, schemaDate.format(dateMsk.getTime()));
                ps.bindString(key++, entry.departureStationId);
                ps.bindString(key++, entry.departureStationName);
                ps.bindString(key++, schemaDateTime.format(entry.departureTime.getTime()));
                ps.bindString(key++, entry.arrivalStationId);
                ps.bindString(key++, entry.arrivalStationName);
                ps.bindString(key++, schemaDateTime.format(entry.arrivalTime.getTime()));
                ps.bindString(key++, entry.trainRouteId);

                if (version == V2) {
                    if (entry.trainName == null) {
                        ps.bindNull(key++);
                    } else {
                        ps.bindString(key++, entry.trainName);
                    }
                }

                ps.bindString(key++, entry.routeStartStationName);
                ps.bindString(key, entry.routeEndStationName);

                ps.executeInsert();
            }

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        db.close();

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
            AlmostStringJoiner sj = new AlmostStringJoiner(
                    "CREATE TABLE " + TABLE_NAME + " (",
                    " TEXT, ",
                    " TEXT)"
            );
            sj.add(DATE);
            for (String column : version == V2 ? listV2 : listV1) sj.add(column);
            String sql = sj.toString();
            Log.e(TAG, "createTable: " + sql);
            db.execSQL(sql);
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
            final String DOWNGRADE = TABLE_NAME + "_downgrade";

            db.execSQL("ALTER TABLE " + TABLE_NAME + " RENAME TO " + DOWNGRADE);

            createTable(db, V1);
            AlmostStringJoiner sj = new AlmostStringJoiner("", ",", "");
            for (String column : listV1) sj.add(column);
            String columns = sj.toString();

            db.execSQL("INSERT INTO " + TABLE_NAME + " (" + columns + ") " +
                    "SELECT " + columns + " FROM " + DOWNGRADE);

            db.execSQL("DROP TABLE " + DOWNGRADE);
        }
    }

    /*1.8stuff*/
    private static class AlmostStringJoiner {
        final private String prefix;
        final private String delimiter;
        final private String suffix;
        private StringBuilder sb;


        AlmostStringJoiner(String prefix, String delimiter, String suffix) {
            this.prefix = prefix;
            this.delimiter = delimiter;
            this.suffix = suffix;
        }

        void add(String seq) {
            prepareBuilder().append(seq);
        }

        private StringBuilder prepareBuilder() {
            if (sb != null) {
                sb.append(delimiter);
            } else {
                sb = new StringBuilder().append(prefix);
            }
            return sb;
        }

        @Override
        public String toString() {
            return sb.toString() + suffix;
        }
    }

}
