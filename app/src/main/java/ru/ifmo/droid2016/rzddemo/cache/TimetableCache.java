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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;// pramp
import java.util.List;
import java.util.Objects;

import ru.ifmo.droid2016.rzddemo.model.TimetableEntry;
import ru.ifmo.droid2016.rzddemo.utils.TimeUtils;

import static ru.ifmo.droid2016.rzddemo.Constants.LOG_DATE_FORMAT;
import static ru.ifmo.droid2016.rzddemo.cache.TimetableCacheContract.Timetable.*;

public class TimetableCache {

    @NonNull
    private final Context context;

    @DataSchemeVersion
    private final int version;

    private void getEntry(SQLiteStatement insert, TimetableEntry entry, Calendar date) {
        insert.bindLong(1, getDate(date));
        insert.bindString(2, entry.departureStationId);
        insert.bindString(3, entry.departureStationName);
        insert.bindLong(4, entry.departureTime.getTimeInMillis());
        insert.bindString(5, entry.arrivalStationId);
        insert.bindString(6, entry.arrivalStationName);
        insert.bindLong(7, entry.arrivalTime.getTimeInMillis());
        insert.bindString(8, entry.trainRouteId);
        insert.bindString(9, entry.routeStartStationName);
        insert.bindString(10, entry.routeEndStationName);
        if (version == DataSchemeVersion.V2) {
            if (entry.trainName != null) {
                insert.bindString(11, entry.trainName);
            } else {
                insert.bindNull(11);
            }
        }
    }

    @AnyThread
    public TimetableCache(@NonNull Context context,
                          @DataSchemeVersion int version) {
        this.context = context.getApplicationContext();
        this.version = version;
    }

    @WorkerThread
    @NonNull
    public List<TimetableEntry> get(@NonNull String fromStationId,
                                    @NonNull String toStationId,
                                    @NonNull Calendar dateMsk)
            throws FileNotFoundException {
        SQLiteDatabase db = TimetableDBHelper.getInstance(context, version).getReadableDatabase();
        String[] projection = (version == DataSchemeVersion.V1) ?
                V1_COMPONENTS :
                V2_COMPONENTS;
        List<TimetableEntry> timetable = new ArrayList<>();

        String selection = DEPARTURE_STATION_ID + "=? AND "
                + ARRIVAL_STATION_ID + "=? AND "
                + DEPARTURE_DATE + "=?";
        try (Cursor cursor = db.query(
                TABLE,
                projection,
                selection,
                new String[]{fromStationId, toStationId, Objects.toString(getDate(dateMsk))},
                null,
                null,
                null)) {
            if (cursor != null && cursor.moveToFirst()) {
                for (; !cursor.isAfterLast(); cursor.moveToNext()) {
                    String departureStationId = cursor.getString(0);
                    String departureStationName = cursor.getString(1);
                    Calendar departureTime = getCurrentTime(cursor.getLong(2));
                    String arrivalStationId = cursor.getString(3);
                    String arrivalStationName = cursor.getString(4);
                    Calendar arrivalTime = getCurrentTime(cursor.getLong(5));
                    String trainRouteId = cursor.getString(6);
                    String routeStartStationName = cursor.getString(7);
                    String routeEndStationName = cursor.getString(8);
                    String trainName;
                    trainName = (version == DataSchemeVersion.V2) ? cursor.getString(9) : null;
                    timetable.add(new TimetableEntry(departureStationId, departureStationName, departureTime,
                            arrivalStationId, arrivalStationName, arrivalTime, trainRouteId, trainName,
                            routeStartStationName, routeEndStationName));
                }
            } else {
                throw new FileNotFoundException("No data in timetable cache for: fromStationId="
                        + fromStationId + ", toStationId=" + toStationId
                        + ", dateMsk=" + LOG_DATE_FORMAT.format(dateMsk.getTime()) + " VERSION " + version);
            }
        } catch (SQLiteException e) {
            Log.wtf(TAG, "Query error: ", e);
            throw new FileNotFoundException("No data in timetable cache for: fromStationId="
                    + fromStationId + ", toStationId=" + toStationId
                    + ", dateMsk=" + LOG_DATE_FORMAT.format(dateMsk.getTime()));
        }
        return timetable;
    }

    @WorkerThread
    public void put(@NonNull Calendar dateMsk,
                    @NonNull List<TimetableEntry> timetable) {
        SQLiteDatabase db = TimetableDBHelper.getInstance(context, version).getWritableDatabase();
        db.beginTransaction();
        String insertion = "INSERT INTO " + TABLE + " ("
                + DEPARTURE_DATE + ", "
                + DEPARTURE_STATION_ID + ", "
                + DEPARTURE_STATION_NAME + ", "
                + DEPARTURE_TIME + ", "
                + ARRIVAL_STATION_ID + ", "
                + ARRIVAL_STATION_NAME + ", "
                + ARRIVAL_TIME + ", "
                + TRAIN_ROUTE_ID + ", "
                + ROUTE_START_STATION_NAME + ", "
                + ROUTE_END_STATION_NAME;

        insertion += version == DataSchemeVersion.V2 ?
                ", " + TRAIN_NAME + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)" :
                ") VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (SQLiteStatement insert = db.compileStatement(insertion)) {
            for (TimetableEntry entry : timetable) {
                getEntry(insert, entry, dateMsk);
                insert.executeInsert();
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private Calendar getCurrentTime(long time) {
        Calendar calendar = Calendar.getInstance(TimeUtils.getMskTimeZone());
        calendar.setTime(new Date(time));
        return calendar;
    }

    private long getDate(Calendar date) {
        return date.get(Calendar.DAY_OF_YEAR) + date.get(Calendar.YEAR) * 500;
    }

    private static final String TAG = "TimetableCache";
}
