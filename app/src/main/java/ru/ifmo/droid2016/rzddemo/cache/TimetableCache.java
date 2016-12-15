package ru.ifmo.droid2016.rzddemo.cache;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.AnyThread;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;
import android.util.Log;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import static ru.ifmo.droid2016.rzddemo.cache.Constraints.*;

import ru.ifmo.droid2016.rzddemo.model.TimetableEntry;

import static ru.ifmo.droid2016.rzddemo.Constants.LOG_DATE_FORMAT;
import static ru.ifmo.droid2016.rzddemo.cache.DataSchemeVersion.*;

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
        Log.d(TAG, "get");
        SQLiteDatabase db = TimetableDbHelper.getInstance(context,version).getReadableDatabase();
        List<TimetableEntry> result = new ArrayList<>();
        Cursor cursor = null;
        String[] toFind;
        if (version == V1)
            toFind = new String[] {
                    DEPARTURE_DATE,
                    DEPARTURE_STATION_ID,
                    DEPARTURE_STATION_NAME,
                    DEPARTURE_TIME,
                    ARRIVAL_STATION_ID,
                    ARRIVAL_STATION_NAME,
                    ARRIVAL_TIME,
                    TRAIN_ROUTE_ID,
                    ROUTE_START_STATION_NAME,
                    ROUTE_END_STATION_NAME
            };
        else
            toFind = new String[] {
                    DEPARTURE_DATE,
                    DEPARTURE_STATION_ID,
                    DEPARTURE_STATION_NAME,
                    DEPARTURE_TIME,
                    ARRIVAL_STATION_ID,
                    ARRIVAL_STATION_NAME,
                    ARRIVAL_TIME,
                    TRAIN_ROUTE_ID,
                    ROUTE_START_STATION_NAME,
                    ROUTE_END_STATION_NAME,
                    TRAIN_NAME
            };
        try {
            cursor = db.query(TABLE, toFind,
                    DEPARTURE_STATION_ID + "=? and " + ARRIVAL_STATION_ID + "=? and " + DEPARTURE_DATE + "=?",
                    new String[]{fromStationId, toStationId, LOG_DATE_FORMAT.format(dateMsk.getTime()) },
                    null, null, null);
            Log.d(TAG, "get: " + fromStationId + " " + toStationId + " " + LOG_DATE_FORMAT.format(dateMsk.getTime()));
            if (cursor.moveToFirst()) {
                for (; !cursor.isAfterLast(); cursor.moveToNext()) {

                    Calendar departureTime = Calendar.getInstance(dateMsk.getTimeZone());
                    departureTime.setTimeInMillis(Long.parseLong(cursor.getString(3)));

                    Calendar arrivalTime = Calendar.getInstance(dateMsk.getTimeZone());
                    arrivalTime.setTimeInMillis(Long.parseLong(cursor.getString(6)));

                    String trainName = null;
                    if (version == V2)
                        trainName = cursor.getString(10);
                    result.add(new TimetableEntry(cursor.getString(1), cursor.getString(2),
                            departureTime, cursor.getString(4), cursor.getString(5),
                            arrivalTime, cursor.getString(7), trainName, cursor.getString(8), cursor.getString(9)));
                }
            } else {
                throw new FileNotFoundException("No data in timetable cache for: fromStationId="
                        + fromStationId + ", toStationId=" + toStationId
                        + ", dateMsk=" + LOG_DATE_FORMAT.format(dateMsk.getTime()));
            }
        } finally {
            if (cursor != null)
                cursor.close();
            db.close();
        }
        return result;
    }

    @WorkerThread
    public void put(@NonNull String fromStationId,
                    @NonNull String toStationId,
                    @NonNull Calendar dateMsk,
                    @NonNull List<TimetableEntry> timetable) {
        Log.d(TAG, "put");

        SQLiteDatabase db = TimetableDbHelper.getInstance(context, version).getWritableDatabase();
        String list = listString1;
        if (version == V2) {
            list += ", " + TRAIN_NAME;
        }

        for (TimetableEntry entry : timetable) {
            String values = "'" + LOG_DATE_FORMAT.format(dateMsk.getTime()) + "', " + entry.toString(version);
            db.execSQL("INSERT INTO " + TABLE + " (" + list + ")" + " VALUES (" + values + ")");
        }
    }

    private String TAG = "tag";
}
