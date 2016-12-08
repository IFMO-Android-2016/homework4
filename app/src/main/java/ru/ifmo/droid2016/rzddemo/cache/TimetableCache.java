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
import java.util.GregorianCalendar;
import java.util.List;

import ru.ifmo.droid2016.rzddemo.model.TimetableEntry;

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

    public static final String LOG_TAG = TimetableCache.class.getSimpleName();

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
        // TODO: ДЗ - реализовать кэш на основе базы данных SQLite с учетом версии модели данных
        Log.d(LOG_TAG, "get command");

        SQLHelper helper = SQLHelper.getInstance(context, version);
        SQLiteDatabase db = helper.getWritableDatabase();

        Cursor cursor = null;

        String columns[];

        if (version == DataSchemeVersion.V1) {
            columns = SQLHelper.CacheContract.COLUMNS_V1;
        } else if (version == DataSchemeVersion.V2) {
            columns = SQLHelper.CacheContract.COLUMNS_V2;
        } else {
            throw new IllegalArgumentException("Illegal version");
        }

        ArrayList<TimetableEntry> result = new ArrayList<TimetableEntry>();

        try {
            cursor = db.query(
                    SQLHelper.CacheContract.TABLE_NAME,
                    columns,
                    SQLHelper.CacheContract.FROM_STATION_ID + "=? AND "
                            + SQLHelper.CacheContract.TO_STATION_ID + " =? AND "
                            + SQLHelper.CacheContract.DATE_MSK + "=?",
                    new String[] { fromStationId, toStationId, LOG_DATE_FORMAT.format(dateMsk.getTime())},
                    null, null, null
            );

            if (cursor != null && cursor.moveToFirst()) {
                for(; !cursor.isAfterLast(); cursor.moveToNext()) {
                    String trainName = null;
                    if (version == DataSchemeVersion.V2) {
                        trainName = cursor.getString(cursor.getColumnIndex(SQLHelper.CacheContract.TRAIN_NAME));
                    }
                    GregorianCalendar firstDate = new GregorianCalendar();
                    firstDate.setTimeInMillis(cursor.getLong(cursor.getColumnIndex(
                            SQLHelper.CacheContract.DEPARTURE_TIME)));

                    GregorianCalendar secondDate = new GregorianCalendar();
                    secondDate.setTimeInMillis(cursor.getLong(cursor.getColumnIndex(
                            SQLHelper.CacheContract.ARRIVAL_TIME)));
                    TimetableEntry entry = new TimetableEntry(
                            cursor.getString(cursor.getColumnIndex(
                                    SQLHelper.CacheContract.DEPARTURE_STATION_ID)),
                            cursor.getString(cursor.getColumnIndex(
                                    SQLHelper.CacheContract.DEPARTURE_STATION_NAME)),
                            firstDate,
                            cursor.getString(cursor.getColumnIndex(
                                    SQLHelper.CacheContract.ARRIVAL_STATION_ID)),
                            cursor.getString(cursor.getColumnIndex(
                                    SQLHelper.CacheContract.ARRIVAL_STATION_NAME)),
                            secondDate,
                            cursor.getString(cursor.getColumnIndex(
                                    SQLHelper.CacheContract.TRAIN_ROUTE_ID)),
                            trainName,
                            cursor.getString(cursor.getColumnIndex(
                                    SQLHelper.CacheContract.ROUTE_START_STATION_NAME)),
                            cursor.getString(cursor.getColumnIndex(
                                    SQLHelper.CacheContract.ROUTE_END_STATION_NAME))
                    );
                    result.add(entry);
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        if (result.size() == 0) {
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
        // TODO: ДЗ - реализовать кэш на основе базы данных SQLite с учетом версии модели данных
        Log.d(LOG_TAG, "put");

        SQLHelper helper = SQLHelper.getInstance(context, version);
        SQLiteDatabase db = helper.getWritableDatabase();
        SQLiteStatement insert = null;
        db.beginTransaction();
        Log.d(LOG_TAG, "Putting");
        try {
            if (version == DataSchemeVersion.V1) {
                insert = db.compileStatement("INSERT INTO " + SQLHelper.CacheContract.TABLE_NAME + " ("
                        + SQLHelper.CacheContract.getCollums(version) + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
            } else {
                Log.d(LOG_TAG, "INSERT INTO " + SQLHelper.CacheContract.TABLE_NAME + " ("
                        + SQLHelper.CacheContract.getCollums(version) + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
                insert = db.compileStatement("INSERT INTO " + SQLHelper.CacheContract.TABLE_NAME + " ("
                        + SQLHelper.CacheContract.getCollums(version) + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
            }

            insert.bindString(2, fromStationId);
            insert.bindString(3, toStationId);
            insert.bindString(4, LOG_DATE_FORMAT.format(dateMsk.getTime()));
            for (int i = 0; i < timetable.size(); ++i) {
                TimetableEntry e = timetable.get(i);
                insert.bindString(5, e.departureStationId);
                insert.bindString(6, e.departureStationName);
                insert.bindLong(7, e.departureTime.getTimeInMillis());
                insert.bindString(8, e.arrivalStationId);
                insert.bindString(9, e.arrivalStationName);
                insert.bindLong(10, e.arrivalTime.getTimeInMillis());
                insert.bindString(11, e.trainRouteId);
                insert.bindString(12, e.routeStartStationName);
                insert.bindString(13, e.routeEndStationName);
                if (version == DataSchemeVersion.V2 && e.trainName != null) {
                    insert.bindString(14, e.trainName);
                }
                insert.executeInsert();
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();

            if (insert != null) {
                insert.close();
            }
        }
    }
}
