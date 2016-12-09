package ru.ifmo.droid2016.rzddemo.cache;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteStatement;
import android.support.annotation.AnyThread;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;
import android.support.v7.graphics.drawable.DrawerArrowDrawable;
import android.util.Log;

import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import ru.ifmo.droid2016.rzddemo.model.TimetableEntry;
import ru.ifmo.droid2016.rzddemo.utils.TimeUtils;

import static ru.ifmo.droid2016.rzddemo.Constants.LOG_DATE_FORMAT;
import static ru.ifmo.droid2016.rzddemo.cache.CacheContract.Caches.*;
import static ru.ifmo.droid2016.rzddemo.cache.CacheContract.CacheColumns.*;

/**
 * Кэш расписания поездов.
 *
 * Ключом является комбинация трех значений:
 * ID станции отправления, ID станции прибытия, дата в московском часовом поясе
 *
 * Единицей хранения является список поездов - {@link TimetableEntry}.
 */

public class TimetableCache {

    private static final String CACHE = "TIMETABLECACHE";
    @NonNull
    private final Context context;

    /**
     * Версия модели данных, с которой работает кэш.
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
        MyDBHelper dbHelper = MyDBHelper.getInstance(context, version);
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String[] projection;
        if (version == DataSchemeVersion.V1) {
            projection = ARGS1;
        } else {
            projection = ARGS2;
        }
        List<TimetableEntry> timetable = new ArrayList<>();

        String switcher =
                  DEPARTURE_STATION_ID + "=? AND "
                + ARRIVAL_STATION_ID + "=? AND "
                + DATE_MSK + "=?";

        try {
            Cursor cursor = db.query(
                    TABLE_NAME,
                    projection,
                    switcher,
                    new String[]{fromStationId, toStationId, Long.toString(getDate(dateMsk))},
                    null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                for (; !cursor.isAfterLast(); cursor.moveToNext()) {
                    int pos = 0;
                    String departureStationId = cursor.getString(pos++);
                    String departureStationName = cursor.getString(pos++);
                    Calendar departureTime = getTime(cursor.getLong(pos++));

                    String arrivalStationId = cursor.getString(pos++);
                    String arrivalStationName = cursor.getString(pos++);
                    Calendar arrivalTime = getTime(cursor.getLong(pos++));

                    String trainRouteId = cursor.getString(pos++);
                    String routeStartStationId = cursor.getString(pos++);
                    String routeEndStationId = cursor.getString(pos++);
                    String trainName;
                    if (version == DataSchemeVersion.V2) {
                        trainName = cursor.getString(pos++);
                    } else {
                        trainName = null;
                    }
                    TimetableEntry entry = new TimetableEntry(
                            departureStationId, departureStationName, departureTime,
                            arrivalStationId, arrivalStationName, arrivalTime,
                            trainRouteId, trainName,
                            routeStartStationId, routeEndStationId);
                    timetable.add(entry);
                }
            } else {
                throw new FileNotFoundException("No data in timetable cache for: fromStationId="
                        + fromStationId + ", toStationId=" + toStationId
                        + ", dateMsk=" + LOG_DATE_FORMAT.format(dateMsk.getTime()));
            }
        } catch (SQLiteException e) {
            throw new FileNotFoundException("No data in timetable cache for: fromStationId="
                    + fromStationId + ", toStationId=" + toStationId
                    + ", dateMsk=" + LOG_DATE_FORMAT.format(dateMsk.getTime()));
        }
        return timetable;
    }

    @WorkerThread
    public void put(@NonNull String fromStationId,
                    @NonNull String toStationId,
                    @NonNull Calendar dateMsk,
                    @NonNull List<TimetableEntry> timetable) {
        MyDBHelper dbHelper = MyDBHelper.getInstance(context, version);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        Log.d(CACHE, "Transaction started");
        SQLiteStatement insert = null;
        try {
            String statement = "INSERT INTO " + TABLE_NAME + " ("
                    + DATE_MSK + ", "
                    + DEPARTURE_STATION_ID + ", "
                    + DEPARTURE_STATION_NAME + ", "
                    + DEPARTURE_TIME + ", "
                    + ARRIVAL_STATION_ID + ", "
                    + ARRIVAL_STATION_NAME + ", "
                    + ARRIVAL_TIME + ", "
                    + TRAIN_ROUTE_ID + ", "
                    + ROUTE_START_STATION_NAME + ", "
                    + ROUTE_END_STATION_NAME;

            String request = ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?";
            if (version == DataSchemeVersion.V2) {
                statement += ", " + TRAIN_NAME ;
                request += ", ?";
            }
            insert = db.compileStatement(statement + request + ")");

            for (TimetableEntry train: timetable) {
                int pos = 0;
                insert.bindLong(++pos, getDate(dateMsk));
                insert.bindString(++pos, train.departureStationId);
                insert.bindString(++pos, train.departureStationName);
                insert.bindLong(++pos, train.departureTime.getTimeInMillis());
                insert.bindString(++pos, train.arrivalStationId);
                insert.bindString(++pos, train.arrivalStationName);
                insert.bindLong(++pos, train.arrivalTime.getTimeInMillis());
                insert.bindString(++pos, train.trainRouteId);
                insert.bindString(++pos, train.routeStartStationName);
                insert.bindString(++pos, train.routeEndStationName);
                if (version == DataSchemeVersion.V2 && train.trainName != null) {
                    insert.bindString(++pos, train.trainName);
                }
                insert.executeInsert();
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        db.close();
    }

    private long getDate(Calendar dateMsk) {
        return dateMsk.get(Calendar.DAY_OF_YEAR) + dateMsk.get(Calendar.YEAR) * 500;
    }

    private Calendar getTime(long time) {
        Calendar calendar = Calendar.getInstance(TimeUtils.getMskTimeZone());
        calendar.setTime(new Date(time));
        return calendar;
    }
}

