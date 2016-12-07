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
import android.util.Log;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import ru.ifmo.droid2016.rzddemo.model.TimetableEntry;
import ru.ifmo.droid2016.rzddemo.utils.TimeUtils;

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

    private long getDay(Calendar date) {
        return date.get(Calendar.DAY_OF_YEAR) + date.get(Calendar.YEAR) * 500;
    }

    private Calendar getCalendar(long time) {
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
        SQLiteDatabase db = Helper.getInstance(context, version).getReadableDatabase();
        String[] projection;
        if (version == DataSchemeVersion.V1) {
            projection = Contract.Timetable.V1;
        } else {
            projection = Contract.Timetable.V2;
        }

        List<TimetableEntry> timetable = new ArrayList<>();

        String selection = Contract.Timetable.DEPARTURE_STATION_ID + "=? AND "
                + Contract.Timetable.ARRIVAL_STATION_ID + "=? AND "
                + Contract.Timetable.DEPARTURE_DATE + "=?";
        try (Cursor cursor = db.query(
                Contract.Timetable.TABLE,
                projection,
                selection,
                new String[]{fromStationId, toStationId, Objects.toString(getDay(dateMsk))},
                null,
                null,
                null)) {
            if (cursor != null && cursor.moveToFirst()) {
                for (; !cursor.isAfterLast(); cursor.moveToNext()) {
                    int i = 0;
                    String departureStationId = cursor.getString(i++);
                    String departureStationName = cursor.getString(i++);
                    Calendar departureTime = getCalendar(cursor.getLong(i++));
                    String arrivalStationId = cursor.getString(i++);
                    String arrivalStationName = cursor.getString(i++);
                    Calendar arrivalTime = getCalendar(cursor.getLong(i++));
                    String trainRouteId = cursor.getString(i++);
                    String routeStartStationName = cursor.getString(i++);
                    String routeEndStationName = cursor.getString(i++);
                    String trainName;
                    if (version == DataSchemeVersion.V2) {
                        trainName = cursor.getString(i);
                    } else {
                        trainName = null;
                    }
                    TimetableEntry entry = new TimetableEntry(departureStationId, departureStationName, departureTime, arrivalStationId, arrivalStationName, arrivalTime, trainRouteId, trainName, routeStartStationName, routeEndStationName);
                    timetable.add(entry);
                }
            } else {
                throw new FileNotFoundException("No data in timetable cache for: fromStationId="
                        + fromStationId + ", toStationId=" + toStationId
                        + ", dateMsk=" + LOG_DATE_FORMAT.format(dateMsk.getTime()));
            }
        } catch (SQLiteException e) {
            Log.wtf(TAG, "Query error: ", e);
            throw new FileNotFoundException("No data in timetable cache for: fromStationId="
                    + fromStationId + ", toStationId=" + toStationId
                    + ", dateMsk=" + LOG_DATE_FORMAT.format(dateMsk.getTime()));
        }

        return timetable;
    }

    private void lotsOfBind(Calendar d, SQLiteStatement in, TimetableEntry en) {
        int i = 0;
        in.bindLong(++i, getDay(d));
        in.bindString(++i, en.departureStationId);
        in.bindString(++i, en.departureStationName);
        in.bindLong(++i, en.departureTime.getTimeInMillis());
        in.bindString(++i, en.arrivalStationId);
        in.bindString(++i, en.arrivalStationName);
        in.bindLong(++i, en.arrivalTime.getTimeInMillis());
        in.bindString(++i, en.trainRouteId);
        in.bindString(++i, en.routeStartStationName);
        in.bindString(++i, en.routeEndStationName);
        if (version == DataSchemeVersion.V2) {
            if (en.trainName != null) {
                in.bindString(++i, en.trainName);
            } else {
                in.bindNull(++i);
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @WorkerThread
    public void put(@NonNull String fromStationId,
                    @NonNull String toStationId,
                    @NonNull Calendar dateMsk,
                    @NonNull List<TimetableEntry> timetable) {
        SQLiteDatabase db = Helper.getInstance(context, version).getWritableDatabase();
        db.beginTransaction();
        String insertion = "INSERT INTO " + Contract.Timetable.TABLE + " ("
                + Contract.Timetable.DEPARTURE_DATE + ", "
                + Contract.Timetable.DEPARTURE_STATION_ID + ", "
                + Contract.Timetable.DEPARTURE_STATION_NAME + ", "
                + Contract.Timetable.DEPARTURE_TIME + ", "
                + Contract.Timetable.ARRIVAL_STATION_ID + ", "
                + Contract.Timetable.ARRIVAL_STATION_NAME + ", "
                + Contract.Timetable.ARRIVAL_TIME + ", "
                + Contract.Timetable.TRAIN_ROUTE_ID + ", "
                + Contract.Timetable.ROUTE_START_STATION_NAME + ", "
                + Contract.Timetable.ROUTE_END_STATION_NAME;
        if (version == DataSchemeVersion.V2) {
            insertion += ", " + Contract.Timetable.TRAIN_NAME + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        } else {
            insertion += ") VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        }
        try (SQLiteStatement insert = db.compileStatement(insertion)) {
            for (TimetableEntry entry : timetable) {
                lotsOfBind(dateMsk, insert, entry);
                insert.executeInsert();
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private static final String TAG = "TimetableCache";
}
