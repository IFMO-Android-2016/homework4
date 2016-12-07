package ru.ifmo.droid2016.rzddemo.cache;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteStatement;
import android.os.Build;
import android.support.annotation.AnyThread;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.annotation.WorkerThread;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import ru.ifmo.droid2016.rzddemo.model.TimetableEntry;
import ru.ifmo.droid2016.rzddemo.utils.TimeUtils;

import static ru.ifmo.droid2016.rzddemo.cache.Helper.*;

/**
 * Кэш расписания поездов.
 * <p>
 * Ключом является комбинация трех значений:
 * ID станции отправления, ID станции прибытия, дата в москомском часовом поясе
 * <p>
 * Единицей хранения является список поездов - {@link TimetableEntry}.
 */

/**
 * Modified by ivantrofimov on 07.12.2016
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
//    @AnyThread
//    public TimetableCache(@NonNull Context context,
//                          @DataSchemeVersion int version) {
//        this.context = context.getApplicationContext();
//        this.version = version;
//    }
    private void bindEntry(SQLiteStatement insert, TimetableEntry entry, Calendar date) {
        int i = 0;
        insert.bindLong(++i, date.get(Calendar.DAY_OF_YEAR) + date.get(Calendar.YEAR) * 500);
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
    }

    private Calendar setCalendar(long time) {
        Calendar calendar = Calendar.getInstance(TimeUtils.getMskTimeZone());
        calendar.setTime(new Date(time));
        return calendar;
    }

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
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @WorkerThread
    @NonNull
    public List<TimetableEntry> get(@NonNull String fromStationId,
                                    @NonNull String toStationId,
                                    @NonNull Calendar dateMsk)
            throws FileNotFoundException {
        SQLiteDatabase db = getInstance(context, version).getReadableDatabase();
        String[] projection;

        projection = version == DataSchemeVersion.V1 ? Contracts.Timetable.COLUMNS_V1 :
                Contracts.Timetable.COLUMNS_V2;

        List<TimetableEntry> timetable = new ArrayList<>();

        String selection = Contracts.Timetable.DEPARTURE_STATION_ID + "=? AND "
                + Contracts.Timetable.ARRIVAL_STATION_ID + "=? AND "
                + Contracts.Timetable.DEPARTURE_DATE + "=?";
        try (Cursor cursor = db.query(
                Contracts.Timetable.TABLE,
                projection,
                selection,
                new String[]{fromStationId, toStationId,
                        Objects.toString(dateMsk.get(Calendar.DAY_OF_YEAR) + dateMsk.get(Calendar.YEAR) * 500)},
                null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                for (; !cursor.isAfterLast(); cursor.moveToNext()) {
                    String departureStationId = cursor.getString(0);
                    String departureStationName = cursor.getString(1);
                    Calendar departureTime = setCalendar(cursor.getLong(2));
                    String arrivalStationId = cursor.getString(3);
                    String arrivalStationName = cursor.getString(4);
                    Calendar arrivalTime = setCalendar(cursor.getLong(5));
                    String trainRouteId = cursor.getString(6);
                    String routeStartStationName = cursor.getString(7);
                    String routeEndStationName = cursor.getString(8);
                    String trainName;
                    trainName = version == DataSchemeVersion.V1 ? null : cursor.getString(9);
                    TimetableEntry entry = new TimetableEntry(departureStationId, departureStationName,
                            departureTime, arrivalStationId, arrivalStationName, arrivalTime, trainRouteId,
                            trainName, routeStartStationName, routeEndStationName);
                    timetable.add(entry);
                }
            } else {
                throw new FileNotFoundException("smt was wrong");
            }
        } catch (SQLiteException e) {
            throw new FileNotFoundException("smt was wrong");
        }

        return timetable;
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @WorkerThread
    public void put(@NonNull String fromStationId,
                    @NonNull String toStationId,
                    @NonNull Calendar dateMsk,
                    @NonNull List<TimetableEntry> timetable) {
        SQLiteDatabase db = getInstance(context, version).getWritableDatabase();
        db.beginTransaction();
        String insertion = String.format("INSERT INTO %s (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s",
                Contracts.Timetable.TABLE,
                Contracts.Timetable.DEPARTURE_DATE, Contracts.Timetable.DEPARTURE_STATION_ID,
                Contracts.Timetable.DEPARTURE_STATION_NAME, Contracts.Timetable.DEPARTURE_TIME,
                Contracts.Timetable.ARRIVAL_STATION_ID, Contracts.Timetable.ARRIVAL_STATION_NAME,
                Contracts.Timetable.ARRIVAL_TIME, Contracts.Timetable.TRAIN_ROUTE_ID,
                Contracts.Timetable.ROUTE_START_STATION_NAME, Contracts.Timetable.ROUTE_END_STATION_NAME);

        insertion += version == DataSchemeVersion.V1 ? ") VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)" :
                ", " + Contracts.Timetable.TRAIN_NAME + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (SQLiteStatement insert = db.compileStatement(insertion)) {
            for (TimetableEntry entry : timetable) {
                bindEntry(insert, entry, dateMsk);
                insert.executeInsert();
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }
}
