package ru.ifmo.droid2016.rzddemo.cache;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
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

import ru.ifmo.droid2016.rzddemo.model.QueryString;
import ru.ifmo.droid2016.rzddemo.model.TimetableEntry;

import static ru.ifmo.droid2016.rzddemo.model.DatabaseContract.Columns.*;
import static ru.ifmo.droid2016.rzddemo.utils.TimeUtils.getTime;


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
    private Database dbptr;
    SimpleDateFormat schemaDate, schemaDateTime;
    public String TAG = "TimetableCache";

    /**
     * Версия модели данных, с которой работает кэщ.
     */
    @DataSchemeVersion
    private final int ver;

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
        this.ver = version;
        dbptr = Database.getInstance(this.context, this.ver);
        schemaDate = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
        schemaDateTime = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.ENGLISH);
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
        Log.d(TAG, "get");
        Log.d(TAG, NAME + QueryString.getColumns(ver).toString() + DATE + "=? AND " + DEPARTURE_STATION_ID + "=? AND " + ARRIVAL_STATION_ID + "=?" + schemaDate.format(dateMsk.getTime()) + fromStationId + toStationId);
        SQLiteDatabase db = dbptr.getReadableDatabase();
        Cursor cursor = db.query(
                NAME,
                QueryString.getColumns(ver),
                DATE + "=? AND " + DEPARTURE_STATION_ID + "=? AND " + ARRIVAL_STATION_ID + "=?",
                new String[]{schemaDate.format(dateMsk.getTime()),
                        fromStationId,
                        toStationId},
                null,
                null,
                null,
                null);
        Log.d(TAG, "After query");
        try {
            if (cursor != null && cursor.moveToFirst()) {
                List<TimetableEntry> timetable = new ArrayList<>();
                do {
                    TimetableEntry entry;
                    try {
                        int querykey = 0;
                        entry = new TimetableEntry(
                                cursor.getString(querykey++),
                                cursor.getString(querykey++),
                                getTime(cursor.getString(querykey++), schemaDateTime),
                                cursor.getString(querykey++),
                                cursor.getString(querykey++),
                                getTime(cursor.getString(querykey++), schemaDateTime),
                                cursor.getString(querykey++),
                                ((ver == DataSchemeVersion.V2) ? cursor.getString(querykey++) : null),
                                cursor.getString(querykey++),
                                cursor.getString(querykey));
                    } catch (ParseException e) {
                        e.printStackTrace();
                        continue;
                    }
                    timetable.add(entry);
                } while (cursor.moveToNext());
                return timetable;
            }
            throw new FileNotFoundException("Not found cached table for given params");
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

    }

    public void close() {
        dbptr.close();
    }


    @WorkerThread
    public void put(@NonNull String fromStationId,
                    @NonNull String toStationId,
                    @NonNull Calendar dateMsk,
                    @NonNull List<TimetableEntry> timetable) {
        Log.d(TAG, "put");
        SQLiteDatabase db = dbptr.getWritableDatabase();
        SQLiteStatement statement = db.compileStatement(
                "INSERT INTO " + NAME+ " ("
                        + DATE + "," + DEPARTURE_STATION_ID + ","
                        + DEPARTURE_STATION_NAME + "," + DEPARTURE_TIME + ","
                        + ARRIVAL_STATION_ID + "," + ARRIVAL_STATION_NAME + ","
                        + ARRIVAL_TIME + "," + TRAIN_ROUTE_ID + ","
                        + ((ver == DataSchemeVersion.V2) ? TRAIN_NAME + "," : "")
                        + ROUTE_START_STATION_NAME + "," + ROUTE_END_STATION_NAME
                        + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?"
                        + ((ver == DataSchemeVersion.V2) ? ", ?" : "" + ")"));
        db.beginTransaction();
        try {
            for (TimetableEntry entry : timetable) {
                int findkey = 1;
                statement.bindString(findkey++, schemaDate.format(dateMsk.getTime()));
                statement.bindString(findkey++, entry.departureStationId);
                statement.bindString(findkey++, entry.departureStationName);
                statement.bindString(findkey++, schemaDateTime.format(entry.departureTime.getTime()));
                statement.bindString(findkey++, entry.arrivalStationId);
                statement.bindString(findkey++, entry.arrivalStationName);
                statement.bindString(findkey++, schemaDateTime.format(entry.arrivalTime.getTime()));
                statement.bindString(findkey++, entry.trainRouteId);
                if (ver == DataSchemeVersion.V2) {
                    if (entry.trainName == null) {
                        statement.bindNull(findkey++);
                    } else {
                        statement.bindString(findkey++, entry.trainName);
                    }
                }
                statement.bindString(findkey++, entry.routeStartStationName);
                statement.bindString(findkey, entry.routeEndStationName);
                statement.executeInsert();
            }

            db.setTransactionSuccessful();

        } catch (SQLiteException e) {
            e.printStackTrace();
        } finally {
            if (statement != null)
                try {
                    statement.close();
                } catch (SQLiteException e) {
                    e.printStackTrace();
                }
            db.endTransaction();
        }
    }

}
