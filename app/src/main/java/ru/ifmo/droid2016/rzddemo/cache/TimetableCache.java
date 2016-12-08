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

/**
 * Кэш расписания поездов.
 * <p>
 * Ключом является комбинация трех значений:
 * ID станции отправления, ID станции прибытия, дата в москомском часовом поясе
 * <p>
 * Единицей хранения является список поездов - {@link TimetableEntry}.
 */

public class TimetableCache {
    @NonNull
    private final Context context;

    /**
     * Версия модели данных, с которой работает кэш.
     */
    @DataSchemeVersion
    private final int version;

    /**
     * Database handler with methods for timetable table
     */
    private final TimetableDatabaseHandler dbHandler;
    private final SimpleDateFormat dateFormat;
    private final SimpleDateFormat dateFormatWithTime;

    /**
     * Создает экземпляр кэша с указанной версией модели данных.
     * <p>
     * Может вызываться на лююбом (в том числе UI потоке). Может быть создано несколько инстансов
     * {@link TimetableCache} -- все они должны потокобезопасно работать с одним физическим кэшом.
     */
    @AnyThread
    public TimetableCache(@NonNull Context context,
                          @DataSchemeVersion int version) {
        this.context = context.getApplicationContext();
        this.version = version;

        this.dbHandler = new TimetableDatabaseHandler(this.context, version);
        this.dateFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH);
        this.dateFormatWithTime = new SimpleDateFormat("dd-MM-yyyy'T'HH:mm", Locale.ENGLISH);
    }

    /**
     * Берет из кэша расписание - список всех поездов, следующих по указанному маршруту с
     * отправлением в указанную дату.
     *
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

        SQLiteDatabase sqLiteDatabase = dbHandler.getReadableDatabase();

        Cursor cursor = sqLiteDatabase.query(
                TrainScheduleContract.TABLE_NAME,
                ((version == DataSchemeVersion.V2)
                        ?
                        new String[]{
                                TrainScheduleContract.DEPARTURE_STATION_ID,
                                TrainScheduleContract.DEPARTURE_STATION_NAME,
                                TrainScheduleContract.DEPARTURE_TIME,
                                TrainScheduleContract.ARRIVAL_STATION_ID,
                                TrainScheduleContract.ARRIVAL_STATION_NAME,
                                TrainScheduleContract.ARRIVAL_TIME,
                                TrainScheduleContract.TRAIN_ROUTE_ID,
                                TrainScheduleContract.TRAIN_NAME,
                                TrainScheduleContract.ROUTE_START_STATION_NAME,
                                TrainScheduleContract.ROUTE_END_STATION_NAME
                        }
                        :
                        new String[]{
                                TrainScheduleContract.DEPARTURE_STATION_ID,
                                TrainScheduleContract.DEPARTURE_STATION_NAME,
                                TrainScheduleContract.DEPARTURE_TIME,
                                TrainScheduleContract.ARRIVAL_STATION_ID,
                                TrainScheduleContract.ARRIVAL_STATION_NAME,
                                TrainScheduleContract.ARRIVAL_TIME,
                                TrainScheduleContract.TRAIN_ROUTE_ID,
                                TrainScheduleContract.ROUTE_START_STATION_NAME,
                                TrainScheduleContract.ROUTE_END_STATION_NAME
                        }
                ),
                TrainScheduleContract.DATE + "=? AND "
                        + TrainScheduleContract.DEPARTURE_STATION_ID + "=? AND "
                        + TrainScheduleContract.ARRIVAL_STATION_ID + "=?",
                new String[]{
                        dateFormat.format(dateMsk.getTime()),
                        fromStationId,
                        toStationId
                },
                null,
                null,
                null,
                null
        );

        try {
            if (cursor != null && cursor.moveToFirst()) {
                List<TimetableEntry> schedule = new ArrayList<>();

                while(true){
                    TimetableEntry timetableEntry;

                    try {
                        int pos = 0;

                        timetableEntry = new TimetableEntry(
                                cursor.getString(pos++),
                                cursor.getString(pos++),
                                parseDate(cursor.getString(pos++)),
                                cursor.getString(pos++),
                                cursor.getString(pos++),
                                parseDate(cursor.getString(pos++)),
                                cursor.getString(pos++),
                                ((version == DataSchemeVersion.V2) ? cursor.getString(pos++) : null),
                                cursor.getString(pos++),
                                cursor.getString(pos)
                        );
                    } catch (ParseException e) {
                        e.printStackTrace();
                        continue;
                    }

                    schedule.add(timetableEntry);

                    if(!cursor.moveToNext()) break;
                }

                return schedule;
            }

            throw new FileNotFoundException("No data in timetable cache for: fromStationId="
                    + fromStationId + ", toStationId=" + toStationId
                    + ", dateMsk=" + LOG_DATE_FORMAT.format(dateMsk.getTime()));
        } finally {
            if (cursor != null) {
                cursor.close();
            }

            sqLiteDatabase.close();
        }
    }

    @WorkerThread
    public void put(@NonNull String fromStationId,
                    @NonNull String toStationId,
                    @NonNull Calendar dateMsk,
                    @NonNull List<TimetableEntry> timetable) {
        SQLiteDatabase sqLiteDatabase = dbHandler.getWritableDatabase();

        String query = "INSERT INTO " + TrainScheduleContract.TABLE_NAME + " ("
                + TrainScheduleContract.DATE + ","
                + TrainScheduleContract.DEPARTURE_STATION_ID + ","
                + TrainScheduleContract.DEPARTURE_STATION_NAME + ","
                + TrainScheduleContract.DEPARTURE_TIME + ","
                + TrainScheduleContract.ARRIVAL_STATION_ID + ","
                + TrainScheduleContract.ARRIVAL_STATION_NAME + ","
                + TrainScheduleContract.ARRIVAL_TIME + ","
                + TrainScheduleContract.TRAIN_ROUTE_ID + ",";

        if (version == DataSchemeVersion.V2) query += TrainScheduleContract.TRAIN_NAME + ",";

        query += TrainScheduleContract.ROUTE_START_STATION_NAME + ","
                + TrainScheduleContract.ROUTE_END_STATION_NAME
                + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?";

        if(version == DataSchemeVersion.V2) query += ", ?";

        query += ")";




        SQLiteStatement statement = sqLiteDatabase.compileStatement(query);

        sqLiteDatabase.beginTransaction();

        try {
            for (TimetableEntry timetableEntry : timetable) {
                int pos = 1;

                statement.bindString(pos++, dateFormat.format(dateMsk.getTime()));
                statement.bindString(pos++, timetableEntry.departureStationId);
                statement.bindString(pos++, timetableEntry.departureStationName);
                statement.bindString(pos++, dateFormatWithTime.format(timetableEntry.departureTime.getTime()));
                statement.bindString(pos++, timetableEntry.arrivalStationId);
                statement.bindString(pos++, timetableEntry.arrivalStationName);
                statement.bindString(pos++, dateFormatWithTime.format(timetableEntry.arrivalTime.getTime()));
                statement.bindString(pos++, timetableEntry.trainRouteId);


                if(version == DataSchemeVersion.V2 && timetableEntry.trainName == null)
                    statement.bindNull(pos++);

                if(version == DataSchemeVersion.V2 && timetableEntry.trainName != null)
                    statement.bindString(pos++, timetableEntry.trainName);


                statement.bindString(pos++, timetableEntry.routeStartStationName);
                statement.bindString(pos++, timetableEntry.routeEndStationName);

                statement.executeInsert();
            }

            sqLiteDatabase.setTransactionSuccessful();
        } finally {
            sqLiteDatabase.endTransaction();
        }

        sqLiteDatabase.close();
    }

    private Calendar parseDate(String date) throws ParseException {
        Calendar calendar = Calendar.getInstance();

        calendar.setTime(dateFormatWithTime.parse(date));
        calendar.setTimeZone(TimeUtils.getMskTimeZone());

        return calendar;
    }

    private static class TimetableDatabaseHandler extends SQLiteOpenHelper {
        private static final String DATABASE_NAME = "train_schedule_rzd";

        @DataSchemeVersion
        private int version;

        TimetableDatabaseHandler(Context context, @DataSchemeVersion int version) {
            super(context, DATABASE_NAME, null, version);
            this.version = version;
        }

        private void createTable(SQLiteDatabase db, @DataSchemeVersion int version) {
            db.execSQL(
                    "CREATE TABLE " + TrainScheduleContract.TABLE_NAME + " ("
                            + TrainScheduleContract.ID + " INTEGER PRIMARY KEY,"
                            + TrainScheduleContract.DATE + " TEXT,"
                            + TrainScheduleContract.DEPARTURE_STATION_ID + " TEXT,"
                            + TrainScheduleContract.DEPARTURE_STATION_NAME + " TEXT,"
                            + TrainScheduleContract.DEPARTURE_TIME + " TEXT,"
                            + TrainScheduleContract.ARRIVAL_STATION_ID + " TEXT,"
                            + TrainScheduleContract.ARRIVAL_STATION_NAME + " TEXT,"
                            + TrainScheduleContract.ARRIVAL_TIME + " TEXT,"
                            + TrainScheduleContract.TRAIN_ROUTE_ID + " TEXT,"
                            + ((version == DataSchemeVersion.V2)
                            ? TrainScheduleContract.TRAIN_NAME + " TEXT,": "")
                            + TrainScheduleContract.ROUTE_START_STATION_NAME + " TEXT,"
                            + TrainScheduleContract.ROUTE_END_STATION_NAME + " TEXT)"
            );
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            createTable(db, version);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL(
                    "ALTER TABLE " + TrainScheduleContract.TABLE_NAME + " "
                            + "ADD COLUMN " + TrainScheduleContract.TRAIN_NAME + " TEXT");
        }

        @Override
        public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            final String DOWNGRADE_TABLE_NAME = "downgraded_" + TrainScheduleContract.TABLE_NAME ;

            db.execSQL(
                    "ALTER TABLE " + TrainScheduleContract.TABLE_NAME + " " +
                            "RENAME TO " + DOWNGRADE_TABLE_NAME
            );

            createTable(db, DataSchemeVersion.V1);

            String columns = TrainScheduleContract.ID + ","
                    + TrainScheduleContract.DATE + ","
                    + TrainScheduleContract.DEPARTURE_STATION_ID + ","
                    + TrainScheduleContract.DEPARTURE_STATION_NAME + ","
                    + TrainScheduleContract.DEPARTURE_TIME + ","
                    + TrainScheduleContract.ARRIVAL_STATION_ID + ","
                    + TrainScheduleContract.ARRIVAL_STATION_NAME + ","
                    + TrainScheduleContract.ARRIVAL_TIME + ","
                    + TrainScheduleContract.TRAIN_ROUTE_ID + ","
                    + TrainScheduleContract.ROUTE_START_STATION_NAME + ","
                    + TrainScheduleContract.ROUTE_END_STATION_NAME;

            db.execSQL(
                    "INSERT INTO " + TrainScheduleContract.TABLE_NAME + " (" + columns + ") " +
                            "SELECT " + columns + " " +
                            "FROM " + DOWNGRADE_TABLE_NAME
            );

            db.execSQL("DROP TABLE " + DOWNGRADE_TABLE_NAME);
        }
    }
}
