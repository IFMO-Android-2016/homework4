package ru.ifmo.droid2016.rzddemo.cache;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
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
import java.util.Locale;

import ru.ifmo.droid2016.rzddemo.DBHelper;
import ru.ifmo.droid2016.rzddemo.model.TimetableEntry;

import static ru.ifmo.droid2016.rzddemo.Constants.LOG_DATE_FORMAT;

/**
 * Кэш расписания поездов.
 * <p/>
 * Ключом является комбинация трех значений:
 * ID станции отправления, ID станции прибытия, дата в москомском часовом поясе
 * <p/>
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
    private DBHelper dbHelper;
    private String dbName = "rzhd5";
    private SimpleDateFormat requestDateFormat;
    /**
     * Создает экземпляр кэша с указанной версией модели данных.
     * <p/>
     * Может вызываться на лююбом (в том числе UI потоке). Может быть создано несколько инстансов
     * {@link TimetableCache} -- все они должны потокобезопасно работать с одним физическим кэшом.
     */
    private String[] selectionColumns;

    @AnyThread
    public TimetableCache(@NonNull Context context,
                          @DataSchemeVersion int version) {
        this.context = context.getApplicationContext();
        this.version = version;
        dbHelper = new DBHelper(context, dbName, version);
        requestDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        if (version == DataSchemeVersion.V1) {
            selectionColumns = new String[]{
                    "departureStationId",
                    "departureStationName",
                    "departureTime",
                    "arrivalStationId",
                    "arrivalStationName",
                    "arrivalTime",
                    "trainRouteId",
                    "routeStartStationName",
                    "routeEndStationName"
            };
        } else {
            selectionColumns = new String[]{
                    "departureStationId",
                    "departureStationName",
                    "departureTime",
                    "arrivalStationId",
                    "arrivalStationName",
                    "arrivalTime",
                    "trainRouteId",
                    "trainName",
                    "routeStartStationName",
                    "routeEndStationName"
            };
        }
    }

    private Calendar convertUnixTimeToCalendar(long time) {
        Calendar calendar = GregorianCalendar.getInstance();
        calendar.setTimeInMillis(time);
        return calendar;
    }

    private String selection = "departureStationId=? AND arrivalStationId=? AND requestTime=?";

    @WorkerThread
    @NonNull
    public List<TimetableEntry> get(@NonNull String fromStationId,
                                    @NonNull String toStationId,
                                    @NonNull Calendar dateMsk)
            throws FileNotFoundException {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String[] selectionArgs = {fromStationId, toStationId, requestDateFormat.format(dateMsk.getTime())};

        List<TimetableEntry> result = new ArrayList<>();

        Cursor cursor = null;
        try {
            cursor = db.query(dbName, selectionColumns, selection, selectionArgs, null, null, null);
            if (cursor == null) {
                throw new FileNotFoundException("No data in timetable cache for: fromStationId="
                        + fromStationId + ", toStationId=" + toStationId
                        + ", dateMsk=" + LOG_DATE_FORMAT.format(dateMsk.getTime()));
            }
            while (cursor.moveToNext()) {
                String trainName = null;
                String routeStartStationName = cursor.getString(7);
                String routeEndStationName = cursor.getString(8);
                if (version == DataSchemeVersion.V2) {
                    trainName = cursor.getString(7);
                    routeStartStationName = cursor.getString(8);
                    routeEndStationName = cursor.getString(9);
                }
                TimetableEntry toAdd = new TimetableEntry(cursor.getString(0),
                        cursor.getString(1),
                        convertUnixTimeToCalendar(cursor.getLong(2)),
                        cursor.getString(3),
                        cursor.getString(4),
                        convertUnixTimeToCalendar(cursor.getLong(5)),
                        cursor.getString(6),
                        trainName,
                        routeStartStationName,
                        routeEndStationName);
                result.add(toAdd);
            }
        } finally {
            if (cursor != null)
                cursor.close();
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

        dbHelper.insert(fromStationId, toStationId, dateMsk, timetable);
    }
}
