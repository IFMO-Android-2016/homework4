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
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.TimeZone;

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
    }

    private Calendar getTime(Long time) {
        Calendar calendar = Calendar.getInstance(TimeUtils.getMskTimeZone());
        calendar.setTimeInMillis(time);
        return calendar;
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
        // TODO: ДЗ - реализовать кэш на основе базы данных SQLite с учетом версии модели данных

        SQLiteDatabase db = TimetableDBHelper.getInstance(context, version).getReadableDatabase();
        String[] projection;
        if (version == DataSchemeVersion.V1) {
            projection = new String[] {TimetableContract.Timetable.DEPARTURE_STATION_ID,
                    TimetableContract.Timetable.DEPARTURE_STATION_NAME,
                    TimetableContract.Timetable.DEPARTURE_TIME,
                    TimetableContract.Timetable.ARRIVAL_STATION_ID,
                    TimetableContract.Timetable.ARRIVAL_STATION_NAME,
                    TimetableContract.Timetable.ARRIVAL_TIME,
                    TimetableContract.Timetable.TRAIN_ROUTE_ID,
                    TimetableContract.Timetable.ROUTE_START_STATION_NAME,
                    TimetableContract.Timetable.ROUTE_END_STATION_NAME};
        } else {
            projection = new String[] {TimetableContract.Timetable.DEPARTURE_STATION_ID,
                    TimetableContract.Timetable.DEPARTURE_STATION_NAME,
                    TimetableContract.Timetable.DEPARTURE_TIME,
                    TimetableContract.Timetable.ARRIVAL_STATION_ID,
                    TimetableContract.Timetable.ARRIVAL_STATION_NAME,
                    TimetableContract.Timetable.ARRIVAL_TIME,
                    TimetableContract.Timetable.TRAIN_ROUTE_ID,
                    TimetableContract.Timetable.ROUTE_START_STATION_NAME,
                    TimetableContract.Timetable.ROUTE_END_STATION_NAME,
                    TimetableContract.Timetable.TRAIN_NAME};
        }

        List<TimetableEntry> timetable = new ArrayList<>();

        String selection = TimetableContract.Timetable.DEPARTURE_STATION_ID + "=? AND "
                + TimetableContract.Timetable.ARRIVAL_STATION_ID + "=? AND "
                + TimetableContract.Timetable.DEPARTURE_DATE + "=?";

        Cursor cursor = null;
        try {
            cursor = db.query(TimetableContract.Timetable.TABLE, projection, selection,
                    new String[] {fromStationId, toStationId,
                            Long.toString(dateMsk.get(Calendar.YEAR) * 1000 + dateMsk.get(Calendar.DAY_OF_YEAR))},
                    null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                for (; !cursor.isAfterLast(); cursor.moveToNext()) {
                    String departureStationId = cursor.getString(0);
                    String departureStationName = cursor.getString(1);
                    Calendar departureTime = getTime(cursor.getLong(2));
                    String arrivalStationId = cursor.getString(3);
                    String arrivalStationName = cursor.getString(4);
                    Calendar arrivalTime = getTime(cursor.getLong(5));
                    String trainRouteId = cursor.getString(6);
                    String routeStartStationName = cursor.getString(7);
                    String routeEndStationName = cursor.getString(8);
                    String trainName;
                    if (version == DataSchemeVersion.V2) {
                        trainName = cursor.getString(9);
                    } else {
                        trainName = null;
                    }
                    TimetableEntry entry = new TimetableEntry(departureStationId, departureStationName, departureTime, arrivalStationId, arrivalStationName, arrivalTime, trainRouteId, trainName, routeStartStationName, routeEndStationName);
                    timetable.add(entry);
                }
            } else {
                throw new FileNotFoundException("No data in timetable cache for: fromStationId="
                        + fromStationId + ", toStationId=" + toStationId
                        + ", dateMsk=" + LOG_DATE_FORMAT.format(dateMsk.getTime()));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return timetable;
    }

    @WorkerThread
    public void put(@NonNull String fromStationId,
                    @NonNull String toStationId,
                    @NonNull Calendar dateMsk,
                    @NonNull List<TimetableEntry> timetable) {
        // TODO: ДЗ - реализовать кэш на основе базы данных SQLite с учетом версии модели данных
        SQLiteDatabase db = TimetableDBHelper.getInstance(context, version).getWritableDatabase();
        String toInsert = "INSERT INTO " + TimetableContract.Timetable.TABLE + " ("
                + TimetableContract.Timetable.DEPARTURE_DATE + ", "
                + TimetableContract.Timetable.DEPARTURE_STATION_ID + ", "
                + TimetableContract.Timetable.DEPARTURE_STATION_NAME + ", "
                + TimetableContract.Timetable.DEPARTURE_TIME + ", "
                + TimetableContract.Timetable.ARRIVAL_STATION_ID + ", "
                + TimetableContract.Timetable.ARRIVAL_STATION_NAME + ", "
                + TimetableContract.Timetable.ARRIVAL_TIME + ", "
                + TimetableContract.Timetable.TRAIN_ROUTE_ID + ", "
                + TimetableContract.Timetable.ROUTE_START_STATION_NAME + ", "
                + TimetableContract.Timetable.ROUTE_END_STATION_NAME;
        if (version == DataSchemeVersion.V1) {
            toInsert += ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        } else {
            toInsert += ", " + TimetableContract.Timetable.TRAIN_NAME + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        }

        db.beginTransaction();
        try {
            SQLiteStatement insertStatement = db.compileStatement(toInsert);
            for (TimetableEntry entry: timetable) {
                insertStatement.bindLong(1, dateMsk.get(Calendar.YEAR) * 1000 + dateMsk.get(Calendar.DAY_OF_YEAR));
                insertStatement.bindString(2, entry.departureStationId);
                insertStatement.bindString(3, entry.departureStationName);
                insertStatement.bindLong(4, entry.departureTime.getTimeInMillis());
                insertStatement.bindString(5, entry.arrivalStationId);
                insertStatement.bindString(6, entry.arrivalStationName);
                insertStatement.bindLong(7, entry.arrivalTime.getTimeInMillis());
                insertStatement.bindString(8, entry.trainRouteId);
                insertStatement.bindString(9, entry.routeStartStationName);
                insertStatement.bindString(10, entry.routeEndStationName);
                if (version == DataSchemeVersion.V2) {
                    if (entry.trainName == null) {
                        insertStatement.bindNull(11);
                    } else {
                        insertStatement.bindString(11, entry.trainName);
                    }
                }
                insertStatement.executeInsert();
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

     }
}
