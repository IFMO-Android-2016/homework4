package ru.ifmo.droid2016.rzddemo.cache;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.provider.BaseColumns;
import android.provider.BaseColumns;
import android.provider.Telephony;
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

/**
 * Кэш расписания поездов.
 *
 * Ключом является комбинация трех значений:
 * ID станции отправления, ID станции прибытия, дата в москомском часовом поясе
 *
 * Единицей хранения является список поездов - {@link TimetableEntry}.
 */

public class TimetableCache {

    @NonNull
    private final Context context;

    /**
     * Версия модели данных, с которой работает кэщ.
     */
    @DataSchemeVersion
    private final int version;

    private final TimetableDatabaseHelper tDBHelper;
    private final SimpleDateFormat dateFormat;
    private final SimpleDateFormat dateTimeFormat;

    /**
     * Создает экземпляр кэша с указанной версией модели данных.
     *
     * Может вызываться на лююбом (в том числе UI потоке). Может быть создано несколько инстансов
     * {@link TimetableCache} -- все они должны потокобезопасно работать с одним физическим кэшом.
     */
    @AnyThread
    public TimetableCache(@NonNull Context context,
                          @DataSchemeVersion int version) {
        this.context = context.getApplicationContext();
        this.version = version;

        this.tDBHelper = new TimetableDatabaseHelper(this.context, this.version);
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.CANADA);
        this.dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.CANADA);
    }

    /**
     * Берет из кэша расписание - список всех поездов, следующих по указанному маршруту с
     * отправлением в указанную дату.
     *
     * @param fromStationId ID станции отправления
     * @param toStationId   ID станции прибытия
     * @param dateMsk       дата в московском часовом поясе
     *
     * @return - список {@link TimetableEntry}
     *
     * @throws FileNotFoundException - если в кэше отсуствуют запрашиваемые данные.
     */
    @WorkerThread
    @NonNull
    public List<TimetableEntry> get(@NonNull String fromStationId,
                                    @NonNull String toStationId,
                                    @NonNull Calendar dateMsk)
            throws FileNotFoundException {

        SQLiteDatabase db = tDBHelper.getReadableDatabase();
        Cursor cursor = db.query(
                TimetableContract.Timetable.TABLE_NAME,
                ((version == DataSchemeVersion.V2)
                        ?
                        new String[]{
                                TimetableContract.Timetable.COLUMN_NAME_DEPARTURE_STATION_ID,
                                TimetableContract.Timetable.COLUMN_NAME_DEPARTURE_STATION_NAME,
                                TimetableContract.Timetable.COLUMN_NAME_DEPARTURE_TIME,
                                TimetableContract.Timetable.COLUMN_NAME_ARRIVAL_STATION_ID,
                                TimetableContract.Timetable.COLUMN_NAME_ARRIVAL_STATION_NAME,
                                TimetableContract.Timetable.COLUMN_NAME_ARRIVAL_TIME,
                                TimetableContract.Timetable.COLUMN_NAME_TRAIN_ROUTE_ID,
                                TimetableContract.Timetable.COLUMN_NAME_TRAIN_NAME,
                                TimetableContract.Timetable.COLUMN_NAME_ROUTE_START_STATION_NAME,
                                TimetableContract.Timetable.COLUMN_NAME_ROUTE_END_STATION_NAME
                        }
                        :
                        new String[]{
                                TimetableContract.Timetable.COLUMN_NAME_DEPARTURE_STATION_ID,
                                TimetableContract.Timetable.COLUMN_NAME_DEPARTURE_STATION_NAME,
                                TimetableContract.Timetable.COLUMN_NAME_DEPARTURE_TIME,
                                TimetableContract.Timetable.COLUMN_NAME_ARRIVAL_STATION_ID,
                                TimetableContract.Timetable.COLUMN_NAME_ARRIVAL_STATION_NAME,
                                TimetableContract.Timetable.COLUMN_NAME_ARRIVAL_TIME,
                                TimetableContract.Timetable.COLUMN_NAME_TRAIN_ROUTE_ID,
                                TimetableContract.Timetable.COLUMN_NAME_ROUTE_START_STATION_NAME,
                                TimetableContract.Timetable.COLUMN_NAME_ROUTE_END_STATION_NAME
                        }
                ),
                TimetableContract.Timetable.COLUMN_NAME_DATE + "=? AND "
                        + TimetableContract.Timetable.COLUMN_NAME_DEPARTURE_STATION_ID + "=? AND "
                        + TimetableContract.Timetable.COLUMN_NAME_ARRIVAL_STATION_ID + "=?",
                new String[]{
                        dateFormat.format(dateMsk.getTime()),
                        fromStationId,
                        toStationId
                },
                null, null, null, null
        );

        try {
            if (cursor != null && cursor.moveToFirst()) {
                List<TimetableEntry> resultTimetable = new ArrayList<>();
                do {
                    TimetableEntry entry;
                    try {
                        int count = 0;
                        entry = new TimetableEntry(
                                cursor.getString(count++),
                                cursor.getString(count++),
                                parseCalendar(cursor.getString(count++)),
                                cursor.getString(count++),
                                cursor.getString(count++),
                                parseCalendar(cursor.getString(count++)),
                                cursor.getString(count++),
                                ((version == DataSchemeVersion.V2) ? cursor.getString(count++) : null),
                                cursor.getString(count++),
                                cursor.getString(count)
                        );
                    } catch (ParseException e) {
                        e.printStackTrace();
                        continue;
                    }

                    resultTimetable.add(entry);
                } while (cursor.moveToNext());

                return resultTimetable;
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

    @WorkerThread
    public void put(@NonNull String fromStationId,
                    @NonNull String toStationId,
                    @NonNull Calendar dateMsk,
                    @NonNull List<TimetableEntry> timetable) {
        SQLiteDatabase db = tDBHelper.getWritableDatabase();

        SQLiteStatement prepared = db.compileStatement(
                "INSERT INTO " + TimetableContract.Timetable.TABLE_NAME + " ("
                        + TimetableContract.Timetable.COLUMN_NAME_DATE + ","
                        + TimetableContract.Timetable.COLUMN_NAME_DEPARTURE_STATION_ID + ","
                        + TimetableContract.Timetable.COLUMN_NAME_DEPARTURE_STATION_NAME + ","
                        + TimetableContract.Timetable.COLUMN_NAME_DEPARTURE_TIME + ","
                        + TimetableContract.Timetable.COLUMN_NAME_ARRIVAL_STATION_ID + ","
                        + TimetableContract.Timetable.COLUMN_NAME_ARRIVAL_STATION_NAME + ","
                        + TimetableContract.Timetable.COLUMN_NAME_ARRIVAL_TIME + ","
                        + TimetableContract.Timetable.COLUMN_NAME_TRAIN_ROUTE_ID + ","
                        + ((version == DataSchemeVersion.V2)
                        ? TimetableContract.Timetable.COLUMN_NAME_TRAIN_NAME + "," : "")
                        + TimetableContract.Timetable.COLUMN_NAME_ROUTE_START_STATION_NAME + ","
                        + TimetableContract.Timetable.COLUMN_NAME_ROUTE_END_STATION_NAME
                        + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?"
                        + ((version == DataSchemeVersion.V2) ? ", ?" : "")
                        + ")"
        );

        db.beginTransaction();

        try {
            for (TimetableEntry entry : timetable) {
                int count = 1;

                prepared.bindString(count++, dateFormat.format(dateMsk.getTime()));
                prepared.bindString(count++, entry.departureStationId);
                prepared.bindString(count++, entry.departureStationName);
                prepared.bindString(count++, dateTimeFormat.format(entry.departureTime.getTime()));
                prepared.bindString(count++, entry.arrivalStationId);
                prepared.bindString(count++, entry.arrivalStationName);
                prepared.bindString(count++, dateTimeFormat.format(entry.arrivalTime.getTime()));
                prepared.bindString(count++, entry.trainRouteId);

                if (version == DataSchemeVersion.V2) {
                    if (entry.trainName == null) {
                        prepared.bindNull(count++);
                    } else {
                        prepared.bindString(count++, entry.trainName);
                    }
                }

                prepared.bindString(count++, entry.routeStartStationName);
                prepared.bindString(count, entry.routeEndStationName);

                prepared.executeInsert();
            }

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        db.close();
    }

    private Calendar parseCalendar(String timeData) throws ParseException {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(dateTimeFormat.parse(timeData));
        calendar.setTimeZone(TimeUtils.getMskTimeZone());
        return calendar;
    }

    private static final class TimetableContract {
        private TimetableContract() {}

        private static final class Timetable implements BaseColumns {
            static final String TABLE_NAME = "timetable";

            static final String COLUMN_NAME_DATE = "date";

            static final String COLUMN_NAME_DEPARTURE_STATION_ID = "departure_station_id";
            static final String COLUMN_NAME_DEPARTURE_STATION_NAME = "departure_station_name";
            static final String COLUMN_NAME_DEPARTURE_TIME = "departure_time";
            static final String COLUMN_NAME_ARRIVAL_STATION_ID = "arrival_station_id";
            static final String COLUMN_NAME_ARRIVAL_STATION_NAME = "arrival_station_name";
            static final String COLUMN_NAME_ARRIVAL_TIME = "arrival_time";
            static final String COLUMN_NAME_TRAIN_ROUTE_ID = "train_route_id";
            static final String COLUMN_NAME_TRAIN_NAME = "train_name";
            static final String COLUMN_NAME_ROUTE_START_STATION_NAME = "route_start_station_name";
            static final String COLUMN_NAME_ROUTE_END_STATION_NAME = "route_end_station_name";
        }
    }

    private static class TimetableDatabaseHelper extends SQLiteOpenHelper {
        private static final String DATABASE_NAME = "timetable.db";

        @DataSchemeVersion
        private int version;

        TimetableDatabaseHelper(Context context, @DataSchemeVersion int version) {
            super(context, DATABASE_NAME, null, version);
            this.version = version;
        }

        private void createTable(SQLiteDatabase db, @DataSchemeVersion int version) {
            db.execSQL(
                    "CREATE TABLE " + TimetableContract.Timetable.TABLE_NAME + " ("
                            + TimetableContract.Timetable.COLUMN_NAME_DATE + " TEXT" + ","
                            + TimetableContract.Timetable.COLUMN_NAME_DEPARTURE_STATION_ID + " TEXT" + ","
                            + TimetableContract.Timetable.COLUMN_NAME_DEPARTURE_STATION_NAME + " TEXT" + ","
                            + TimetableContract.Timetable.COLUMN_NAME_DEPARTURE_TIME + " TEXT" + ","
                            + TimetableContract.Timetable.COLUMN_NAME_ARRIVAL_STATION_ID + " TEXT" + ","
                            + TimetableContract.Timetable.COLUMN_NAME_ARRIVAL_STATION_NAME + " TEXT" + ","
                            + TimetableContract.Timetable.COLUMN_NAME_ARRIVAL_TIME + " TEXT" + ","
                            + TimetableContract.Timetable.COLUMN_NAME_TRAIN_ROUTE_ID + " TEXT" + ","
                            + ((version == DataSchemeVersion.V2)
                            ? TimetableContract.Timetable.COLUMN_NAME_TRAIN_NAME + " TEXT" + "," : "")
                            + TimetableContract.Timetable.COLUMN_NAME_ROUTE_START_STATION_NAME + " TEXT" + ","
                            + TimetableContract.Timetable.COLUMN_NAME_ROUTE_END_STATION_NAME + " TEXT"
                            + ")"
            );
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            createTable(db, version);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL(
                    "ALTER TABLE " + TimetableContract.Timetable.TABLE_NAME + " " + "ADD COLUMN " +
                            TimetableContract.Timetable.COLUMN_NAME_TRAIN_NAME + " TEXT");
        }

        @Override
        public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            final String TEMP_TABLE_NAME = TimetableContract.Timetable.TABLE_NAME + "_downgrade";

            db.execSQL(
                    "ALTER TABLE " + TimetableContract.Timetable.TABLE_NAME + " " +
                            "RENAME TO " + TEMP_TABLE_NAME
            );

            createTable(db, DataSchemeVersion.V1);

            String columns = ""
                    + TimetableContract.Timetable.COLUMN_NAME_DATE + ","
                    + TimetableContract.Timetable.COLUMN_NAME_DEPARTURE_STATION_ID + ","
                    + TimetableContract.Timetable.COLUMN_NAME_DEPARTURE_STATION_NAME + ","
                    + TimetableContract.Timetable.COLUMN_NAME_DEPARTURE_TIME + ","
                    + TimetableContract.Timetable.COLUMN_NAME_ARRIVAL_STATION_ID + ","
                    + TimetableContract.Timetable.COLUMN_NAME_ARRIVAL_STATION_NAME + ","
                    + TimetableContract.Timetable.COLUMN_NAME_ARRIVAL_TIME + ","
                    + TimetableContract.Timetable.COLUMN_NAME_TRAIN_ROUTE_ID + ","
                    + TimetableContract.Timetable.COLUMN_NAME_ROUTE_START_STATION_NAME + ","
                    + TimetableContract.Timetable.COLUMN_NAME_ROUTE_END_STATION_NAME;

            db.execSQL(
                    "INSERT INTO " + TimetableContract.Timetable.TABLE_NAME +
                            " (" + columns + ") " + "SELECT " + columns +
                            " " + "FROM " + TEMP_TABLE_NAME
            );
            db.execSQL("DROP TABLE " + TEMP_TABLE_NAME);
        }
    }
}
