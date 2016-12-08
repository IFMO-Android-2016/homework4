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
        SQLiteDatabase db = TimetableDBHelper.getInstance(context, version).getReadableDatabase();
        String[] columns;
        if (version == DataSchemeVersion.V1) {
            columns = TimetableContract.Timetable.NAME_OF_ARGUMENTS_V1;
        } else {
            columns = TimetableContract.Timetable.NAME_OF_ARGUMENTS_V2;
        }

        List<TimetableEntry> timetable = new ArrayList<>();

        String selection = TimetableContract.Timetable.DEPARTURE_STATION_ID + "=? AND "
                + TimetableContract.Timetable.ARRIVAL_STATION_ID + "=? AND "
                + TimetableContract.Timetable.DEPARTURE_DATE + "=?";
        String[] arguments = new String[]{fromStationId, toStationId, Long.toString(getDate(dateMsk))};
        Cursor cursor = null;
        try {
            cursor = db.query(TimetableContract.Timetable.TABLE, columns, selection, arguments, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                for (; !cursor.isAfterLast(); cursor.moveToNext()) {
                    int i = 0;
                    String departureStationId = cursor.getString(i++);
                    String departureStationName = cursor.getString(i++);
                    Calendar departureTime = getTime(cursor.getLong(i++));
                    String arrivalStationId = cursor.getString(i++);
                    String arrivalStationName = cursor.getString(i++);
                    Calendar arrivalTime = getTime(cursor.getLong(i++));
                    String trainRouteId = cursor.getString(i++);
                    String routeStartStationName = cursor.getString(i++);
                    String routeEndStationName = cursor.getString(i++);
                    String trainName = null;
                    if (version == DataSchemeVersion.V2) {
                        trainName = cursor.getString(i);
                    }
                    TimetableEntry entry = new TimetableEntry(departureStationId,
                            departureStationName,
                            departureTime,
                            arrivalStationId,
                            arrivalStationName,
                            arrivalTime,
                            trainRouteId,
                            trainName,
                            routeStartStationName,
                            routeEndStationName);
                    timetable.add(entry);
                }
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

    private long getDate(@NonNull Calendar dateMsk) {
        return dateMsk.get(Calendar.DAY_OF_YEAR) + dateMsk.get(Calendar.YEAR) * 500;
    }

    private Calendar getTime(long dateMsk) {
        Calendar calendar = Calendar.getInstance(TimeUtils.getMskTimeZone());
        calendar.setTime(new Date(dateMsk));
        return calendar;
    }


    @WorkerThread
    public void put(@NonNull String fromStationId,
                    @NonNull String toStationId,
                    @NonNull Calendar dateMsk,
                    @NonNull List<TimetableEntry> timetable) {
        SQLiteDatabase db = TimetableDBHelper.getInstance(context, version).getWritableDatabase();
        db.beginTransaction();
        String toInsert = "INSERT INTO " + TimetableContract.Timetable.TABLE + " ("
                + TimetableContract.Timetable.DEPARTURE_DATE + ", "
                + TimetableContract.Timetable.DEPARTURE_STATION_ID + ", "
                + TimetableContract.Timetable.DEPATURE_STATION_NAME + ", "
                + TimetableContract.Timetable.DEPATURE_TIME + ", "
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

        SQLiteStatement insert = null;

        try {
            insert = db.compileStatement(toInsert);
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
