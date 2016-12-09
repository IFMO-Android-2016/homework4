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
        SQLiteDatabase db = TimetableDatabaseHelper.getInstance(context, version).getReadableDatabase();
        String[] projection;
        if (version == DataSchemeVersion.V1) {
            projection = new String[] {TimetableCacheContract.Timetable.DEPARTURE_STATION_ID,
                    TimetableCacheContract.Timetable.DEPARTURE_STATION_NAME,
                    TimetableCacheContract.Timetable.DEPARTURE_TIME,
                    TimetableCacheContract.Timetable.ARRIVAL_STATION_ID,
                    TimetableCacheContract.Timetable.ARRIVAL_STATION_NAME,
                    TimetableCacheContract.Timetable.ARRIVAL_TIME,
                    TimetableCacheContract.Timetable.TRAIN_ROUTE_ID,
                    TimetableCacheContract.Timetable.ROUTE_START_STATION_NAME,
                    TimetableCacheContract.Timetable.ROUTE_END_STATION_NAME};
        } else {
            projection = new String[] {TimetableCacheContract.Timetable.DEPARTURE_STATION_ID,
                    TimetableCacheContract.Timetable.DEPARTURE_STATION_NAME,
                    TimetableCacheContract.Timetable.DEPARTURE_TIME,
                    TimetableCacheContract.Timetable.ARRIVAL_STATION_ID,
                    TimetableCacheContract.Timetable.ARRIVAL_STATION_NAME,
                    TimetableCacheContract.Timetable.ARRIVAL_TIME,
                    TimetableCacheContract.Timetable.TRAIN_ROUTE_ID,
                    TimetableCacheContract.Timetable.ROUTE_START_STATION_NAME,
                    TimetableCacheContract.Timetable.ROUTE_END_STATION_NAME,
                    TimetableCacheContract.Timetable.TRAIN_NAME};
        }

        List<TimetableEntry> timetable = new ArrayList<>();

        String selection = TimetableCacheContract.Timetable.DEPARTURE_STATION_ID + "=? AND "
                + TimetableCacheContract.Timetable.ARRIVAL_STATION_ID + "=? AND "
                + TimetableCacheContract.Timetable.DEPARTURE_DATE + "=?";

        Cursor cursor = null;
        try {
            cursor = db.query(TimetableCacheContract.Timetable.TABLE, projection, selection,
                    new String[] {fromStationId, toStationId, Long.toString(getDay(dateMsk))},
                    null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                for (; !cursor.isAfterLast(); cursor.moveToNext()) {
                    String departureStationId = cursor.getString(0);
                    String departureStationName = cursor.getString(1);
                    Calendar departureTime = getCalendar(cursor.getLong(2));
                    String arrivalStationId = cursor.getString(3);
                    String arrivalStationName = cursor.getString(4);
                    Calendar arrivalTime = getCalendar(cursor.getLong(5));
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

    private Calendar getCalendar(long aLong) {
        Calendar calendar = Calendar.getInstance(TimeUtils.getMskTimeZone());
        calendar.setTimeInMillis(aLong);
        return calendar;
    }

    private long getDay(Calendar date) {
        return date.get(Calendar.YEAR) * 1000 + date.get(Calendar.DAY_OF_YEAR);
    }

    @WorkerThread
    public void put(@NonNull String fromStationId,
                    @NonNull String toStationId,
                    @NonNull Calendar dateMsk,
                    @NonNull List<TimetableEntry> timetable) {
        SQLiteDatabase db = TimetableDatabaseHelper.getInstance(context, version).getWritableDatabase();
        String toInsert = "INSERT INTO " + TimetableCacheContract.Timetable.TABLE + " ("
                + TimetableCacheContract.Timetable.DEPARTURE_DATE + ", "
                + TimetableCacheContract.Timetable.DEPARTURE_STATION_ID + ", "
                + TimetableCacheContract.Timetable.DEPARTURE_STATION_NAME + ", "
                + TimetableCacheContract.Timetable.DEPARTURE_TIME + ", "
                + TimetableCacheContract.Timetable.ARRIVAL_STATION_ID + ", "
                + TimetableCacheContract.Timetable.ARRIVAL_STATION_NAME + ", "
                + TimetableCacheContract.Timetable.ARRIVAL_TIME + ", "
                + TimetableCacheContract.Timetable.TRAIN_ROUTE_ID + ", "
                + TimetableCacheContract.Timetable.ROUTE_START_STATION_NAME + ", "
                + TimetableCacheContract.Timetable.ROUTE_END_STATION_NAME;
        if (version == DataSchemeVersion.V1) {
            toInsert += ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        } else {
            toInsert += ", " + TimetableCacheContract.Timetable.TRAIN_NAME + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        }

        db.beginTransaction();
        try {
            SQLiteStatement insertStatement = db.compileStatement(toInsert);
            for (TimetableEntry entry: timetable) {
                insertStatement.bindLong(1, getDay(dateMsk));
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
