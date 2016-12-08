package ru.ifmo.droid2016.rzddemo.cache;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.support.annotation.AnyThread;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;

import java.io.FileNotFoundException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import ru.ifmo.droid2016.rzddemo.model.TimetableEntry;
import ru.ifmo.droid2016.rzddemo.utils.TimeUtils;

import static ru.ifmo.droid2016.rzddemo.cache.TimetableContract.*;
import static ru.ifmo.droid2016.rzddemo.Constants.LOG_DATE_FORMAT;
import static ru.ifmo.droid2016.rzddemo.Constants.LOG_TIME_FORMAT;

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

    private final TimetableDBHelper timetableDBHelper;

    /**
     * Создает экземпляр кэша с указанной версией модели данных.
     *
     * Может вызываться на любом (в том числе UI потоке). Может быть создано несколько инстансов
     * {@link TimetableCache} -- все они должны потокобезопасно работать с одним физическим кэшом.
     */
    @AnyThread
    public TimetableCache(@NonNull Context context,
                          @DataSchemeVersion int version) {
        this.context = context.getApplicationContext();
        this.version = version;
        this.timetableDBHelper = TimetableDBHelper.getInstance(context, version);
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

        SQLiteDatabase sqLiteDatabase = timetableDBHelper.getReadableDatabase();
        Cursor cursor = sqLiteDatabase.query(
                Timetable.TABLE,
                Timetable.getArgsArr(version),
                Timetable.DEPARTURE_DATE + "=? AND "
                        + Timetable.DEPARTURE_STATION_ID + "=? AND "
                        + Timetable.ARRIVAL_STATION_ID + "=?",
                new String[] {LOG_DATE_FORMAT.format(dateMsk.getTime()), fromStationId, toStationId},
                null, null, null);

        ArrayList<TimetableEntry> result = new ArrayList<>();
        try {
            if (cursor != null && cursor.moveToFirst()) {
                while (true) {
                    result.add(new TimetableEntry(
                            cursor.getString(TimetableColumnsIndices.DEPARTURE_STATION_ID),
                            cursor.getString(TimetableColumnsIndices.DEPARTURE_STATION_NAME),
                            getTime(cursor.getString(TimetableColumnsIndices.DEPARTURE_TIME)),
                            cursor.getString(TimetableColumnsIndices.ARRIVAL_STATION_ID),
                            cursor.getString(TimetableColumnsIndices.ARRIVAL_STATION_NAME),
                            getTime(cursor.getString(TimetableColumnsIndices.ARRIVAL_TIME)),
                            cursor.getString(TimetableColumnsIndices.TRAIN_ROUTE_ID),
                            version == DataSchemeVersion.V1 ? null : cursor.getString(TimetableColumnsIndices.TRAIN_NAME),
                            cursor.getString(TimetableColumnsIndices.ROUTE_START_STATION_NAME),
                            cursor.getString(TimetableColumnsIndices.ROUTE_END_STATION_NAME))
                    );

                    if (!cursor.moveToNext()) {
                        break;
                    }
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        if (result.isEmpty()) {
            throw new FileNotFoundException("No data in timetable cache for: fromStationId="
                    + fromStationId + ", toStationId=" + toStationId
                    + ", dateMsk=" + LOG_DATE_FORMAT.format(dateMsk.getTime()));
        }
        return result;
    }

    @WorkerThread
    public void put(@NonNull String fromStationId,
                    @NonNull String toStationId,
                    @NonNull Calendar dateMsk,
                    @NonNull List<TimetableEntry> timetable) {

        SQLiteDatabase sqLiteDatabase = timetableDBHelper.getWritableDatabase();
        SQLiteStatement insert = sqLiteDatabase.compileStatement("INSERT INTO " + Timetable.TABLE +
                " (" + Timetable.getArgsStr(version) + ") VALUES " + getPlaceholders(version));

        sqLiteDatabase.beginTransaction();
        try {
            for (TimetableEntry entry : timetable) {
                insert.bindString(TimetableColumnsIndices.DEPARTURE_DATE + 1, LOG_DATE_FORMAT.format(dateMsk.getTime()));
                insert.bindString(TimetableColumnsIndices.DEPARTURE_STATION_ID + 1, fromStationId);
                insert.bindString(TimetableColumnsIndices.ARRIVAL_STATION_ID + 1, toStationId);
                insert.bindString(TimetableColumnsIndices.DEPARTURE_TIME + 1, LOG_TIME_FORMAT.format(entry.departureTime.getTime()));

                insert.bindString(TimetableColumnsIndices.DEPARTURE_STATION_NAME + 1, entry.departureStationName);
                insert.bindString(TimetableColumnsIndices.ARRIVAL_STATION_NAME + 1, entry.arrivalStationName);
                insert.bindString(TimetableColumnsIndices.ARRIVAL_TIME + 1, LOG_TIME_FORMAT.format(entry.arrivalTime.getTime()));
                insert.bindString(TimetableColumnsIndices.TRAIN_ROUTE_ID + 1, entry.trainRouteId);
                insert.bindString(TimetableColumnsIndices.ROUTE_START_STATION_NAME + 1, entry.routeStartStationName);
                insert.bindString(TimetableColumnsIndices.ROUTE_END_STATION_NAME + 1, entry.routeEndStationName);
                if (version == DataSchemeVersion.V2) {
                    if (entry.trainName != null) {
                        insert.bindString(TimetableColumnsIndices.TRAIN_NAME + 1, entry.trainName);
                    } else {
                        insert.bindNull(TimetableColumnsIndices.TRAIN_NAME + 1);
                    }
                }
                insert.executeInsert();
            }
            sqLiteDatabase.setTransactionSuccessful();
        } finally {
            sqLiteDatabase.endTransaction();
            if (insert != null) {
                insert.close();
            }
        }
    }

    private Calendar getTime(String time) {
        Calendar result = Calendar.getInstance(TimeUtils.getMskTimeZone());
        try {
            result.setTime(LOG_TIME_FORMAT.parse(time));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return result;
    }

    private String getPlaceholders(int version) {
        String res = "(";
        for (int i = 0; i < Timetable.getArgsArr(version).length - 1 ; i++) {
            res += "?, ";
        }
        return res + "?)";
    }

    private final String TAG = "TimetableCache";
}
