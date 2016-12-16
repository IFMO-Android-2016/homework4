package ru.ifmo.droid2016.rzddemo.cache;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.support.annotation.AnyThread;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import ru.ifmo.droid2016.rzddemo.db.TimetableDBHelper;
import ru.ifmo.droid2016.rzddemo.model.TimetableEntry;
import ru.ifmo.droid2016.rzddemo.utils.TimeUtils;

import static ru.ifmo.droid2016.rzddemo.Constants.LOG_DATE_FORMAT;
import static ru.ifmo.droid2016.rzddemo.db.TimetableContract.Timetable.TABLE;
import static ru.ifmo.droid2016.rzddemo.db.TimetableContract.TimetableColumns.*;

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
        SQLiteDatabase db = TimetableDBHelper.getInstance(context, version).getReadableDatabase();
        String[] columns = (version == DataSchemeVersion.V1
                ?
                new String[]{DEPARTURE_STATION_ID
                        , DEPARTURE_STATION_NAME
                        , DEPARTURE_TIME
                        , ARRIVAL_STATION_ID
                        , ARRIVAL_STATION_NAME
                        , ARRIVAL_TIME
                        , TRAIN_ROUTE_ID
                        ,ROUTE_START_STATION_NAME
                        , ROUTE_END_STATION_NAME}
                :
                new String[]{DEPARTURE_STATION_ID
                        , DEPARTURE_STATION_NAME
                        , DEPARTURE_TIME
                        , ARRIVAL_STATION_ID
                        , ARRIVAL_STATION_NAME
                        , ARRIVAL_TIME
                        , TRAIN_ROUTE_ID
                        , ROUTE_START_STATION_NAME
                        , ROUTE_END_STATION_NAME
                        , TRAIN_NAME});

        String projection = DEPARTURE_STATION_ID + "=? AND "
                + ARRIVAL_STATION_ID + "=? AND "
                + DEPARTURE_DATE + "=?";

        String[] selectionArgs = new String[]{fromStationId, toStationId,
                Long.toString(getDate(dateMsk))};

        Cursor c = null;
        try {
            c = db.query(TABLE, columns, projection,
                    selectionArgs, null, null, null);

            if (c != null && c.moveToFirst()) {

                List<TimetableEntry> ret = new ArrayList<>();

                for (; !c.isAfterLast(); c.moveToNext()) {
                    int i = 0;
                    String departureStationId = c.getString(i++);
                    String departureStationName = c.getString(i++);
                    Calendar departureTime = getTime(c.getLong(i++));
                    String arrivalStationId = c.getString(i++);
                    String arrivalStationName = c.getString(i++);
                    Calendar arrivalTime = getTime(c.getLong(i++));
                    String trainRouteId = c.getString(i++);
                    String routeStartStationName = c.getString(i++);
                    String routeEndStationName = c.getString(i++);
                    String trainName = (version == DataSchemeVersion.V1
                            ? null
                            : c.getString(i));

                    ret.add(new TimetableEntry(departureStationId, departureStationName
                            , departureTime, arrivalStationId, arrivalStationName, arrivalTime
                            , trainRouteId, trainName, routeStartStationName, routeEndStationName));

                }
                return ret;
            } else {
                throw new FileNotFoundException("No data in timetable cache for: fromStationId="
                        + fromStationId + ", toStationId=" + toStationId
                        + ", dateMsk=" + LOG_DATE_FORMAT.format(dateMsk.getTime()));
            }
        } finally {
            if (c != null) {
                c.close();
            }
            db.close();
        }
    }

    private Calendar getTime(long aLong) {
        Calendar c = Calendar.getInstance(TimeUtils.getMskTimeZone());
        c.setTime(new Date(aLong));
        return c;
    }

    private long getDate(@NonNull Calendar dateMSK) {
        return dateMSK.get(Calendar.DAY_OF_YEAR) + dateMSK.get(Calendar.YEAR) * 500;
    }

    @WorkerThread
    public void put(@NonNull Calendar dateMsk,
                    @NonNull List<TimetableEntry> timetable) {
        SQLiteDatabase db = TimetableDBHelper.getInstance(context, version).getWritableDatabase();

        SQLiteStatement insert = null;
        db.beginTransaction();
        try {
            insert = db.compileStatement("INSERT INTO " + TABLE + " ("
                    + DEPARTURE_DATE + ", "
                    + DEPARTURE_STATION_ID + ", "
                    + DEPARTURE_STATION_NAME + ", "
                    + DEPARTURE_TIME + ", "
                    + ARRIVAL_STATION_ID + ", "
                    + ARRIVAL_STATION_NAME + ", "
                    + ARRIVAL_TIME + ", "
                    + TRAIN_ROUTE_ID + ", "
                    + ROUTE_START_STATION_NAME + ", "
                    + ROUTE_END_STATION_NAME
                    + (version == DataSchemeVersion.V1
                    ? ") VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
                    : ", " + TRAIN_NAME + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"));

            for (TimetableEntry entry : timetable) {
                int i = 0;
                insert.bindLong(++i, getDate(dateMsk));
                insert.bindString(++i, entry.departureStationId);
                insert.bindString(++i, entry.departureStationName);
                insert.bindLong(++i, entry.departureTime.getTimeInMillis());
                insert.bindString(++i, entry.arrivalStationId);
                insert.bindString(++i, entry.arrivalStationName);
                insert.bindLong(++i, entry.arrivalTime.getTimeInMillis());
                insert.bindString(++i, entry.trainRouteId);
                insert.bindString(++i, entry.routeStartStationName);
                insert.bindString(++i, entry.routeEndStationName);
                if (version == DataSchemeVersion.V2) {
                    if (entry.trainName != null) {
                        insert.bindString(++i, entry.trainName);
                    } else {
                        insert.bindNull(++i);
                    }
                }

                insert.executeInsert();
            }
            db.setTransactionSuccessful();
        } finally {
            if (insert != null) {
                try {
                    insert.close();
                } catch (Exception ignored) {
                }
            }
            db.endTransaction();
        }

        db.close();
    }
}