package ru.ifmo.droid2016.rzddemo.cache;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.support.annotation.AnyThread;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;
import android.util.Log;

import java.io.FileNotFoundException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import ru.ifmo.droid2016.rzddemo.model.TimetableEntry;

import static ru.ifmo.droid2016.rzddemo.Constants.LOG_DATE_FORMAT;

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
    private static final SimpleDateFormat date = LOG_DATE_FORMAT;

    @NonNull
    private static final SimpleDateFormat time =
            new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());

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
        SQLiteDatabase db = SQLiteHelper.getInstance(context, version).getReadableDatabase();

        Cursor cursor = null;

        try {
            cursor = db.query(SQLiteHelper.TABLE_NAME,
                    version == DataSchemeVersion.V1 ?
                            new String[]{
                                    SQLiteHelper.DATE,
                                    SQLiteHelper.DEPARTURE_STATION_ID,
                                    SQLiteHelper.DEPARTURE_STATION_NAME,
                                    SQLiteHelper.DEPARTURE_TIME,
                                    SQLiteHelper.ARRIVAL_STATION_ID,
                                    SQLiteHelper.ARRIVAL_STATION_NAME,
                                    SQLiteHelper.ARRIVAL_TIME,
                                    SQLiteHelper.TRAIN_ROUTE_ID,
                                    SQLiteHelper.ROUTE_START_STATION_NAME,
                                    SQLiteHelper.ROUTE_END_STATION_NAME,
                            } :
                            new String[]{
                                    SQLiteHelper.DATE,
                                    SQLiteHelper.DEPARTURE_STATION_ID,
                                    SQLiteHelper.DEPARTURE_STATION_NAME,
                                    SQLiteHelper.DEPARTURE_TIME,
                                    SQLiteHelper.ARRIVAL_STATION_ID,
                                    SQLiteHelper.ARRIVAL_STATION_NAME,
                                    SQLiteHelper.ARRIVAL_TIME,
                                    SQLiteHelper.TRAIN_ROUTE_ID,
                                    SQLiteHelper.ROUTE_START_STATION_NAME,
                                    SQLiteHelper.ROUTE_END_STATION_NAME,
                                    SQLiteHelper.TRAIN_NAME
                            },
                    SQLiteHelper.DATE + "=? AND " +
                            SQLiteHelper.DEPARTURE_STATION_ID + "=? AND " +
                            SQLiteHelper.ARRIVAL_STATION_ID + "=?",
                    new String[]{
                            date.format(dateMsk.getTime()),
                            fromStationId,
                            toStationId
                    },
                    null,
                    null,
                    null);

            if (cursor != null && cursor.moveToFirst()) {
                List<TimetableEntry> result = new ArrayList<>();
                for (; !cursor.isAfterLast(); cursor.moveToNext()) {
                    try {
                        result.add(new TimetableEntry(
                                cursor.getString(1),
                                cursor.getString(2),
                                getCalendar(cursor.getString(3)),
                                cursor.getString(4),
                                cursor.getString(5),
                                getCalendar(cursor.getString(6)),
                                cursor.getString(7),
                                version == DataSchemeVersion.V2 ?
                                        cursor.getString(10) :
                                        null,
                                cursor.getString(8),
                                cursor.getString(9)
                        ));
                    } catch (ParseException e) {
                        e.printStackTrace();
                        Log.wtf("TimetableCache", "Can't parse " + cursor.getString(3) + "/" +
                                cursor.getString(6));
                    }
                }
                return result;
            }
        } finally {
            if (cursor != null) {
                try {
                    cursor.close();
                } catch (Exception x) {
                    x.printStackTrace();
                }
            }
        }

        // TODO: ДЗ - реализовать кэш на основе базы данных SQLite с учетом версии модели данных
        throw new FileNotFoundException("No data in timetable cache for: fromStationId="
                + fromStationId + ", toStationId=" + toStationId
                + ", dateMsk=" + LOG_DATE_FORMAT.format(dateMsk.getTime()));
    }

    private Calendar getCalendar(String timeString) throws ParseException {
        Calendar cal = Calendar.getInstance();
        cal.setTime(time.parse(timeString));
        return cal;
    }

    @WorkerThread
    public void put(@NonNull String fromStationId,
                    @NonNull String toStationId,
                    @NonNull Calendar dateMsk,
                    @NonNull List<TimetableEntry> timetable) {
        SQLiteDatabase db = SQLiteHelper.getInstance(context, version).getWritableDatabase();

        SQLiteStatement insert = db.compileStatement("INSERT INTO " + SQLiteHelper.TABLE_NAME + " (" +
                SQLiteHelper.DATE + ", " +
                SQLiteHelper.DEPARTURE_STATION_ID + ", " +
                SQLiteHelper.DEPARTURE_STATION_NAME + ", " +
                SQLiteHelper.DEPARTURE_TIME + ", " +
                SQLiteHelper.ARRIVAL_STATION_ID + ", " +
                SQLiteHelper.ARRIVAL_STATION_NAME + ", " +
                SQLiteHelper.ARRIVAL_TIME + ", " +
                SQLiteHelper.TRAIN_ROUTE_ID + ", " +
                SQLiteHelper.ROUTE_START_STATION_NAME + ", " +
                SQLiteHelper.ROUTE_END_STATION_NAME +
                ((version == DataSchemeVersion.V2) ?
                        ", " + SQLiteHelper.TRAIN_NAME :
                        "") +
                ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?" +
                ((version == DataSchemeVersion.V2) ?
                        ", ?)" :
                        ")"));

        db.beginTransaction();
        try {
            for (TimetableEntry entry : timetable) {
                insert.bindString(1, date.format(dateMsk.getTime()));
                insert.bindString(2, entry.departureStationId);
                insert.bindString(3, entry.departureStationName);
                insert.bindString(4, time.format(entry.departureTime.getTime()));
                insert.bindString(5, entry.arrivalStationId);
                insert.bindString(6, entry.arrivalStationName);
                insert.bindString(7, time.format(entry.arrivalTime.getTime()));
                insert.bindString(8, entry.trainRouteId);
                insert.bindString(9, entry.routeStartStationName);
                insert.bindString(10, entry.routeEndStationName);

                if (version == DataSchemeVersion.V2) {
                    if (entry.trainName == null) {
                        insert.bindNull(11);
                    } else {
                        insert.bindString(11, entry.trainName);
                    }
                }

                insert.executeInsert();
            }

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }
}
