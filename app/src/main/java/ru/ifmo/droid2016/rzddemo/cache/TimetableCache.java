package ru.ifmo.droid2016.rzddemo.cache;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.support.annotation.AnyThread;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;

import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import ru.ifmo.droid2016.rzddemo.database.TrainScheduleDbHelper;
import ru.ifmo.droid2016.rzddemo.model.TimetableEntry;

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
    private final TrainScheduleDbHelper trainScheduleDbHelper;

    private final SimpleDateFormat schemaDate;
    private final SimpleDateFormat schemaDateTime;

    /**
     * Версия модели данных, с которой работает кэщ.
     */
    @DataSchemeVersion
    private final int version;

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
        this.trainScheduleDbHelper = new TrainScheduleDbHelper(this.context, this.version);
        this.schemaDate = new SimpleDateFormat("yyyy-MM-dd", Locale.CANADA);
        this.schemaDateTime = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.CANADA);
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


        SQLiteDatabase db = trainScheduleDbHelper.getReadableDatabase();

        Cursor cursor = null;

        try {
            cursor = db.query(
                    TrainScheduleDbHelper.TrainScheduleContract.FeedEntry.TABLE_NAME,
                    ((version == DataSchemeVersion.V1)
                            ?
                            new String[]{
                                    TrainScheduleDbHelper.TrainScheduleContract.FeedEntry.COLUMN_NAME_DEPARTURE_STATION_ID,
                                    TrainScheduleDbHelper.TrainScheduleContract.FeedEntry.COLUMN_NAME_DEPARTURE_STATION_NAME,
                                    TrainScheduleDbHelper.TrainScheduleContract.FeedEntry.COLUMN_NAME_DEPARTURE_TIME,
                                    TrainScheduleDbHelper.TrainScheduleContract.FeedEntry.COLUMN_NAME_ARRIVAL_STATION_ID,
                                    TrainScheduleDbHelper.TrainScheduleContract.FeedEntry.COLUMN_NAME_ARRIVAL_STATION_NAME,
                                    TrainScheduleDbHelper.TrainScheduleContract.FeedEntry.COLUMN_NAME_ARRIVAL_TIME,
                                    TrainScheduleDbHelper.TrainScheduleContract.FeedEntry.COLUMN_NAME_TRAIN_ROUTE_ID,
                                    TrainScheduleDbHelper.TrainScheduleContract.FeedEntry.COLUMN_NAME_ROUTE_START_STATION_NAME,
                                    TrainScheduleDbHelper.TrainScheduleContract.FeedEntry.COLUMN_NAME_ROUTE_END_STATION_NAME
                            }
                            :
                            new String[]{
                                    TrainScheduleDbHelper.TrainScheduleContract.FeedEntry.COLUMN_NAME_DEPARTURE_STATION_ID,
                                    TrainScheduleDbHelper.TrainScheduleContract.FeedEntry.COLUMN_NAME_DEPARTURE_STATION_NAME,
                                    TrainScheduleDbHelper.TrainScheduleContract.FeedEntry.COLUMN_NAME_DEPARTURE_TIME,
                                    TrainScheduleDbHelper.TrainScheduleContract.FeedEntry.COLUMN_NAME_ARRIVAL_STATION_ID,
                                    TrainScheduleDbHelper.TrainScheduleContract.FeedEntry.COLUMN_NAME_ARRIVAL_STATION_NAME,
                                    TrainScheduleDbHelper.TrainScheduleContract.FeedEntry.COLUMN_NAME_ARRIVAL_TIME,
                                    TrainScheduleDbHelper.TrainScheduleContract.FeedEntry.COLUMN_NAME_TRAIN_ROUTE_ID,
                                    TrainScheduleDbHelper.TrainScheduleContract.FeedEntry.COLUMN_NAME_TRAIN_NAME,
                                    TrainScheduleDbHelper.TrainScheduleContract.FeedEntry.COLUMN_NAME_ROUTE_START_STATION_NAME,
                                    TrainScheduleDbHelper.TrainScheduleContract.FeedEntry.COLUMN_NAME_ROUTE_END_STATION_NAME
                            }
                    ),
                    TrainScheduleDbHelper.TrainScheduleContract.FeedEntry.COLUMN_NAME_DATE
                            + "=? AND "
                            + TrainScheduleDbHelper.TrainScheduleContract.FeedEntry.COLUMN_NAME_DEPARTURE_STATION_ID
                            + "=? AND "
                            + TrainScheduleDbHelper.TrainScheduleContract.FeedEntry.COLUMN_NAME_ARRIVAL_STATION_ID
                            + "=?",
                    new String[]{
                            LOG_DATE_FORMAT.format(dateMsk.getTime()),
                            fromStationId,
                            toStationId
                    },
                    null, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                List<TimetableEntry> timetable = new ArrayList<>();
                do {
                    int temp = 0;
                    TimetableEntry entry;
                        entry = new TimetableEntry(
                                cursor.getString(temp++),
                                cursor.getString(temp++),
                                getCalendar(cursor.getLong(temp++)),//calendar1,
                                cursor.getString(temp++),
                                cursor.getString(temp++),
                                getCalendar(cursor.getLong(temp++)),//calendar2,
                                cursor.getString(temp++),
                                ((version == DataSchemeVersion.V2) ? cursor.getString(temp++) : null),
                                cursor.getString(temp++),
                                cursor.getString(temp)
                        );
                    timetable.add(entry);
                } while (cursor.moveToNext());

                return timetable;
            } else {
                throw new FileNotFoundException("No data in timetable cache for: fromStationId="
                        + fromStationId + ", toStationId=" + toStationId
                        + ", dateMsk=" + LOG_DATE_FORMAT.format(dateMsk.getTime()));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }
    }

    private Calendar getCalendar(long aLong) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(aLong);
        return calendar;
    }

    @WorkerThread
    public void put(@NonNull String fromStationId,
                    @NonNull String toStationId,
                    @NonNull Calendar dateMsk,
                    @NonNull List<TimetableEntry> timetable) {

        SQLiteDatabase database = trainScheduleDbHelper.getWritableDatabase();
        SQLiteStatement sqlRequest = null;

        if (version == DataSchemeVersion.V1) {
            sqlRequest = database.compileStatement(
                    "INSERT INTO " + TrainScheduleDbHelper.TrainScheduleContract.FeedEntry.TABLE_NAME + " ("
                            + TrainScheduleDbHelper.TrainScheduleContract.FeedEntry.COLUMN_NAME_DATE + ","
                            + TrainScheduleDbHelper.TrainScheduleContract.FeedEntry.COLUMN_NAME_DEPARTURE_STATION_ID + ","
                            + TrainScheduleDbHelper.TrainScheduleContract.FeedEntry.COLUMN_NAME_DEPARTURE_STATION_NAME + ","
                            + TrainScheduleDbHelper.TrainScheduleContract.FeedEntry.COLUMN_NAME_DEPARTURE_TIME + ","
                            + TrainScheduleDbHelper.TrainScheduleContract.FeedEntry.COLUMN_NAME_ARRIVAL_STATION_ID + ","
                            + TrainScheduleDbHelper.TrainScheduleContract.FeedEntry.COLUMN_NAME_ARRIVAL_STATION_NAME + ","
                            + TrainScheduleDbHelper.TrainScheduleContract.FeedEntry.COLUMN_NAME_ARRIVAL_TIME + ","
                            + TrainScheduleDbHelper.TrainScheduleContract.FeedEntry.COLUMN_NAME_TRAIN_ROUTE_ID + ","
                            + TrainScheduleDbHelper.TrainScheduleContract.FeedEntry.COLUMN_NAME_ROUTE_START_STATION_NAME + ","
                            + TrainScheduleDbHelper.TrainScheduleContract.FeedEntry.COLUMN_NAME_ROUTE_END_STATION_NAME
                            + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?" + ")");
        } else if (version == DataSchemeVersion.V2) {
            sqlRequest = database.compileStatement(
                    "INSERT INTO " + TrainScheduleDbHelper.TrainScheduleContract.FeedEntry.TABLE_NAME + " ("
                            + TrainScheduleDbHelper.TrainScheduleContract.FeedEntry.COLUMN_NAME_DATE + ","
                            + TrainScheduleDbHelper.TrainScheduleContract.FeedEntry.COLUMN_NAME_DEPARTURE_STATION_ID + ","
                            + TrainScheduleDbHelper.TrainScheduleContract.FeedEntry.COLUMN_NAME_DEPARTURE_STATION_NAME + ","
                            + TrainScheduleDbHelper.TrainScheduleContract.FeedEntry.COLUMN_NAME_DEPARTURE_TIME + ","
                            + TrainScheduleDbHelper.TrainScheduleContract.FeedEntry.COLUMN_NAME_ARRIVAL_STATION_ID + ","
                            + TrainScheduleDbHelper.TrainScheduleContract.FeedEntry.COLUMN_NAME_ARRIVAL_STATION_NAME + ","
                            + TrainScheduleDbHelper.TrainScheduleContract.FeedEntry.COLUMN_NAME_ARRIVAL_TIME + ","
                            + TrainScheduleDbHelper.TrainScheduleContract.FeedEntry.COLUMN_NAME_TRAIN_ROUTE_ID + ","
                            + TrainScheduleDbHelper.TrainScheduleContract.FeedEntry.COLUMN_NAME_TRAIN_NAME + ","
                            + TrainScheduleDbHelper.TrainScheduleContract.FeedEntry.COLUMN_NAME_ROUTE_START_STATION_NAME + ","
                            + TrainScheduleDbHelper.TrainScheduleContract.FeedEntry.COLUMN_NAME_ROUTE_END_STATION_NAME
                            + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?" + ")");
        }

        database.beginTransaction();

        try {
            for (TimetableEntry entry : timetable) {

                int temp = 1;
                sqlRequest.bindString(temp++, LOG_DATE_FORMAT.format(dateMsk.getTime()));
                sqlRequest.bindString(temp++, entry.departureStationId);
                sqlRequest.bindString(temp++, entry.departureStationName);
                sqlRequest.bindString(temp++, schemaDateTime.format(entry.departureTime.getTime()));
                sqlRequest.bindString(temp++, entry.arrivalStationId);
                sqlRequest.bindString(temp++, entry.arrivalStationName);
                sqlRequest.bindString(temp++, schemaDateTime.format(entry.arrivalTime.getTime()));
                sqlRequest.bindString(temp++, entry.trainRouteId);

                if (version == DataSchemeVersion.V2) {
                    if (entry.trainName == null) {
                        sqlRequest.bindNull(temp++);
                    } else {
                        sqlRequest.bindString(temp++, entry.trainName);
                    }
                }
                sqlRequest.bindString(temp++, entry.routeStartStationName);
                sqlRequest.bindString(temp++, entry.routeEndStationName);

                sqlRequest.executeInsert();
            }
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
        database.close();
    }
}
