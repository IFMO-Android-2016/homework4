package ru.ifmo.droid2016.rzddemo.cache;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteStatement;
import android.support.annotation.AnyThread;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;
import android.widget.ProgressBar;

import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import ru.ifmo.droid2016.rzddemo.Constants;
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

    private Calendar getTime(long time) {
        Calendar calendar = Calendar.getInstance(TimeUtils.getMskTimeZone());
        calendar.setTime(new Date(time));
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

        SQLiteDatabase database = TimetableDatabaseHelper.getInstance(context, version).getReadableDatabase();
        String[] projection = (version == DataSchemeVersion.V1) ?
                Constants.V1_COMPONENTS : Constants.V2_COMPONENTS;

        long date = dateMsk.get(Calendar.DAY_OF_YEAR) + dateMsk.get(Calendar.YEAR) * 500;

        List<TimetableEntry> timetable = new ArrayList<>();

        String selection =
                Constants.DEPARTURE_STATION_ID  + "=? AND " +
                Constants.ARRIVAL_STATION_ID    + "=? AND " +
                Constants.DEPARTURE_DATE        + "=?";
        try (Cursor cursor = database.query(
                Constants.TIMETABLE_TABLE_NAME,
                projection,
                selection,
                new String[]{fromStationId, toStationId, Objects.toString(date)},
                null, null, null)) {

            if (cursor != null && cursor.moveToFirst()) {
                while (!cursor.isAfterLast()) {
                    String departureStationId = cursor.getString(0);
                    String departureStationName = cursor.getString(1);
                    Calendar departureTime = getTime(cursor.getLong(2));
                    String arrivalStationId = cursor.getString(3);
                    String arrivalstationName = cursor.getString(4);
                    Calendar arrivalTime = getTime(cursor.getLong(5));
                    String trainRouteId = cursor.getString(6);
                    String routeStartStationName = cursor.getString(7);
                    String routeEndStationName = cursor.getString(8);

                    String trainName = (version == DataSchemeVersion.V1) ? null : cursor.getString(9);
                    timetable.add(new TimetableEntry(departureStationId, departureStationName, departureTime,
                            arrivalStationId, arrivalstationName, arrivalTime,
                            trainRouteId, trainName,
                            routeStartStationName, routeEndStationName));

                    cursor.moveToNext();
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

    private  void bindEntry(SQLiteStatement insert, TimetableEntry timetableEntry, Calendar date) {
        insert.bindLong(1, date.get(Calendar.DAY_OF_YEAR) + date.get(Calendar.YEAR) * 500);
        insert.bindString(2, timetableEntry.departureStationId);
        insert.bindString(3, timetableEntry.departureStationName);
        insert.bindLong(4, timetableEntry.departureTime.getTimeInMillis());
        insert.bindString(5, timetableEntry.arrivalStationId);
        insert.bindString(6, timetableEntry.arrivalStationName);
        insert.bindLong(7, timetableEntry.arrivalTime.getTimeInMillis());
        insert.bindString(8, timetableEntry.trainRouteId);
        insert.bindString(9, timetableEntry.routeStartStationName);
        insert.bindString(10, timetableEntry.routeEndStationName);

        if (version == DataSchemeVersion.V2) {
            if (timetableEntry.trainName != null) {
                insert.bindString(11, timetableEntry.trainName);
            } else {
                insert.bindNull(11);
            }
        }

    }

    @WorkerThread
    public void put(@NonNull String fromStationId,
                    @NonNull String toStationId,
                    @NonNull Calendar dateMsk,
                    @NonNull List<TimetableEntry> timetable) {
        SQLiteDatabase database = TimetableDatabaseHelper.getInstance(context, version).getWritableDatabase();
        database.beginTransaction();

        String insertion = "INSERT INTO " + Constants.TIMETABLE_TABLE_NAME + " (" +
                Constants.DEPARTURE_DATE + ", " +
                Constants.DEPARTURE_STATION_ID + ", " +
                Constants.DEPARTURE_STATION_NAME + ", " +
                Constants.DEPARTURE_TIME + ", " +
                Constants.ARRIVAL_STATION_ID + ", " +
                Constants.ARRIVAL_STATION_NAME + ", " +
                Constants.ARRIVAL_TIME + ", " +
                Constants.TRAIN_ROUTE_ID + ", " +
                Constants.ROUTE_START_STATION_NAME + ", " +
                Constants.ROUTE_END_STATION_NAME;

        if (version == DataSchemeVersion.V1) {
            insertion += ") VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        } else {
            insertion += ", " + Constants.TRAIN_NAME + ") VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        }

        try (SQLiteStatement insert = database.compileStatement(insertion)) {
            for (TimetableEntry timetableEntry : timetable) {
                bindEntry(insert, timetableEntry, dateMsk);
                insert.executeInsert();
            }
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
    }
}
