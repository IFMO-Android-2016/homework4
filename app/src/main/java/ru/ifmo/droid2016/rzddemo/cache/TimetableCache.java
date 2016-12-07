package ru.ifmo.droid2016.rzddemo.cache;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
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

import ru.ifmo.droid2016.rzddemo.model.TimetableEntry;
import ru.ifmo.droid2016.rzddemo.utils.TimeUtils;

import static ru.ifmo.droid2016.rzddemo.Constants.LOG_DATE_FORMAT;
import static ru.ifmo.droid2016.rzddemo.cache.TimetableCacheContract.Timetable.*;

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
        SQLiteDatabase db = TimetableHelper.getInstance(context, version).getReadableDatabase();
        String[] colNames = (version == DataSchemeVersion.V1 ?
                new String[]{DEPARTURE_STATION_ID,
                        DEPARTURE_STATION_NAME,
                        DEPARTURE_TIME,
                        ARRIVAL_STATION_ID,
                        ARRIVAL_STATION_NAME,
                        ARRIVAL_TIME,
                        TRAIN_ROUTE_ID,
                        ROUTE_START_STATION_NAME,
                        ROUTE_END_STATION_NAME} :
                new String[]{DEPARTURE_STATION_ID,
                        DEPARTURE_STATION_NAME,
                        DEPARTURE_TIME,
                        ARRIVAL_STATION_ID,
                        ARRIVAL_STATION_NAME,
                        ARRIVAL_TIME,
                        TRAIN_ROUTE_ID,
                        ROUTE_START_STATION_NAME,
                        ROUTE_END_STATION_NAME,
                        TRAIN_NAME});
        String select = DEPARTURE_STATION_ID + "=? AND "
                     + ARRIVAL_STATION_ID   + "=? AND "
                     + DEPARTURE_DATE       + "=?";
        String[] args = new String[]{fromStationId, toStationId, Long.toString(fromCalendarToLong(dateMsk))};
        List<TimetableEntry> timetable = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = db.query(TABLE, colNames, select, args, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                for (; !cursor.isAfterLast(); cursor.moveToNext()) {
                    String departureStationId = cursor.getString(0);
                    String departureStationName = cursor.getString(1);
                    Calendar departureTime = fromLongToCalendar(cursor.getLong(2));
                    String arrivalStationId = cursor.getString(3);
                    String arrivalStationName = cursor.getString(4);
                    Calendar arrivalTime = fromLongToCalendar(cursor.getLong(5));
                    String trainRouteId = cursor.getString(6);
                    String routeStartStationName = cursor.getString(7);
                    String routeEndStationName = cursor.getString(8);
                    String trainName = (version == DataSchemeVersion.V1) ? null :
                            cursor.getString(9);
                    timetable.add(new TimetableEntry(departureStationId, departureStationName,
                            departureTime, arrivalStationId, arrivalStationName, arrivalTime,
                            trainRouteId, trainName, routeStartStationName, routeEndStationName));
                }
            }
            else {
                throw new FileNotFoundException("No data in timetable cache for: fromStationId="
                        + fromStationId + ", toStationId=" + toStationId
                        + ", dateMsk=" + LOG_DATE_FORMAT.format(dateMsk.getTime()));
            }
        }
        finally {
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
        SQLiteDatabase db = TimetableHelper.getInstance(context, version).getWritableDatabase();
        String cols = DEPARTURE_DATE
                + ", " + DEPARTURE_STATION_ID
                + ", " + DEPARTURE_STATION_NAME
                + ", " + DEPARTURE_TIME
                + ", " + ARRIVAL_STATION_ID
                + ", " + ARRIVAL_STATION_NAME
                + ", " + ARRIVAL_TIME
                + ", " + TRAIN_ROUTE_ID
                + ", " + ROUTE_START_STATION_NAME
                + ", " + ROUTE_END_STATION_NAME;
        String query = "INSERT INTO " + TABLE + "( " + cols
                + (version == DataSchemeVersion.V1 ?
                ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)" :
                "," + TRAIN_NAME + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
        db.beginTransaction();
        try {
            SQLiteStatement statement = db.compileStatement(query);
            for (TimetableEntry entry : timetable) {
                statement.bindLong(1, fromCalendarToLong(dateMsk));
                statement.bindString(2, entry.departureStationId);
                statement.bindString(3, entry.departureStationName);
                statement.bindLong(4, entry.departureTime.getTimeInMillis());
                statement.bindString(5, entry.arrivalStationId);
                statement.bindString(6, entry.arrivalStationName);
                statement.bindLong(7, entry.arrivalTime.getTimeInMillis());
                statement.bindString(8, entry.trainRouteId);
                statement.bindString(9, entry.routeStartStationName);
                statement.bindString(10, entry.routeEndStationName);
                if (version == DataSchemeVersion.V2) {
                    if (entry.trainName != null) {
                        statement.bindString(11, entry.trainName);
                    }
                    else {
                        statement.bindNull(11);
                    }
                }
                statement.executeInsert();
            }
            db.setTransactionSuccessful();
        }
        finally {
            db.endTransaction();
        }
    }

    private Calendar fromLongToCalendar(Long l) {
        Calendar calendar = Calendar.getInstance(TimeUtils.getMskTimeZone());
        calendar.setTimeInMillis(l);
        return calendar;
    }

    private long fromCalendarToLong(@NonNull Calendar calendar) {
        return calendar.get(Calendar.DAY_OF_YEAR) + calendar.get(Calendar.YEAR) * 500;
    }
}
