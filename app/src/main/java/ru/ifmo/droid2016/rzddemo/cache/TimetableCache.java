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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import ru.ifmo.droid2016.rzddemo.model.TimetableEntry;

import static ru.ifmo.droid2016.rzddemo.Constants.LOG_DATE_FORMAT;
import static ru.ifmo.droid2016.rzddemo.cache.DataSchemeVersion.V1;
import static ru.ifmo.droid2016.rzddemo.cache.DataSchemeVersion.V2;

/**
 * Кэш расписания поездов.
 * <p>
 * Ключом является комбинация трех значений:
 * ID станции отправления, ID станции прибытия, дата в москомском часовом поясе
 * <p>
 * Единицей хранения является список поездов - {@link TimetableEntry}.
 */

public class TimetableCache {
    private static final String LOG_TAG = "TimetableCache";

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
        Log.d(LOG_TAG, "get");
        TimetableDBHelper timetableDBHelper = TimetableDBHelper.getInstance(context, version);
        SQLiteDatabase database = timetableDBHelper.getReadableDatabase();
        Cursor cursor = null;
        ArrayList<TimetableEntry> list = new ArrayList<>();
        try {
            cursor = database.query(TimetableContract.TABLE,
                    version == V1 ? TimetableContract.argumentsForV1 : TimetableContract.argumentsForV2,
                    TimetableContract.FROM_STATION_ID + "=? AND " + TimetableContract.TO_STATION_ID + "=? AND "
                    + TimetableContract.DATE_MSK + "=?",
                    new String[] {fromStationId, toStationId, LOG_DATE_FORMAT.format(dateMsk.getTime())},
                    null,
                    null,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                for (; !cursor.isAfterLast(); cursor.moveToNext()) {
                    Calendar calendar1 = Calendar.getInstance();
                    calendar1.setTimeInMillis(cursor.getLong(6));
                    Calendar calendar2 = Calendar.getInstance();
                    calendar2.setTimeInMillis(cursor.getLong(9));
                    TimetableEntry timetableEntry = new TimetableEntry(
                            cursor.getString(4),
                            cursor.getString(5),
                            calendar1,
                            cursor.getString(7),
                            cursor.getString(8),
                            calendar2,
                            cursor.getString(10),
                            (version == V2) ? cursor.getString(13) : null,
                            cursor.getString(11),
                            cursor.getString(12)
                    );
                    list.add(timetableEntry);
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        // TODO: ДЗ - реализовать кэш на основе базы данных SQLite с учетом версии модели данных
        if (list.isEmpty()) {
            throw new FileNotFoundException("No data in timetable cache for: fromStationId="
                    + fromStationId + ", toStationId=" + toStationId
                    + ", dateMsk=" + LOG_DATE_FORMAT.format(dateMsk.getTime()));
        } else {
            return list;
        }
    }

    @WorkerThread
    public void put(@NonNull String fromStationId,
                    @NonNull String toStationId,
                    @NonNull Calendar dateMsk,
                    @NonNull List<TimetableEntry> timetable) {
        Log.d(LOG_TAG, "putting " + timetable.size());
        TimetableDBHelper timetableDBHelper = TimetableDBHelper.getInstance(context, version);
        SQLiteDatabase database = timetableDBHelper.getWritableDatabase();
        SQLiteStatement statement = null;
        database.beginTransaction();
        try {
            if (version == DataSchemeVersion.V2) {
                StringBuilder args = new StringBuilder();
                for (int i = 0; i < TimetableContract.argumentsForV2.length; ++i) {
                    args.append(TimetableContract.argumentsForV2[i]);
                    if (i != TimetableContract.argumentsForV2.length - 1) {
                        args.append(", ");
                    }
                }
                statement = database.compileStatement("INSERT INTO " + TimetableContract.TABLE + " (" +
                        args.toString() + ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
            } else {
                StringBuilder args = new StringBuilder();
                for (int i = 0; i < TimetableContract.argumentsForV1.length; ++i) {
                    args.append(TimetableContract.argumentsForV1[i]);
                    if (i != TimetableContract.argumentsForV1.length - 1) {
                        args.append(", ");
                    }
                }
                statement = database.compileStatement("INSERT INTO " + TimetableContract.TABLE + " (" +
                        args.toString() + ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)");
            }
            statement.bindString(2, fromStationId);
            statement.bindString(3, toStationId);
            statement.bindString(4, LOG_DATE_FORMAT.format(dateMsk.getTime()));
            for (TimetableEntry timetableEntry : timetable) {
                statement.bindString(5, timetableEntry.departureStationId);
                statement.bindString(6, timetableEntry.departureStationName);
                statement.bindLong(7, timetableEntry.departureTime.getTimeInMillis());
                statement.bindString(8, timetableEntry.arrivalStationId);
                statement.bindString(9, timetableEntry.arrivalStationName);
                statement.bindLong(10, timetableEntry.arrivalTime.getTimeInMillis());
                statement.bindString(11, timetableEntry.trainRouteId);
                statement.bindString(12, timetableEntry.routeStartStationName);
                statement.bindString(13, timetableEntry.routeEndStationName);
                if (version != DataSchemeVersion.V1 && timetableEntry.trainName != null) statement.bindString(14, timetableEntry.trainName);
                statement.executeInsert();
            }
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
            if (statement != null) {
                statement.close();
            }
        }

        // TODO: ДЗ - реализовать кэш на основе базы данных SQLite с учетом версии модели данных
    }
}
