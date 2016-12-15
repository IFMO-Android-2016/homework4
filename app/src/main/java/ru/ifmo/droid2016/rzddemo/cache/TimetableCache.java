package ru.ifmo.droid2016.rzddemo.cache;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.Build;
import android.support.annotation.AnyThread;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.annotation.WorkerThread;

import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import ru.ifmo.droid2016.rzddemo.model.TimetableEntry;
import ru.ifmo.droid2016.rzddemo.utils.TimeUtils;

import static ru.ifmo.droid2016.rzddemo.Constants.LOG_DATE_FORMAT;

public class TimetableCache {

    @NonNull
    private final Context context;

    @DataSchemeVersion
    private final int version;

    @AnyThread
    public TimetableCache(@NonNull Context context,
                          @DataSchemeVersion int version) {
        this.context = context.getApplicationContext();
        this.version = version;
    }

    private Calendar getTime(Long time) {
        Calendar calendar = Calendar.getInstance(TimeUtils.getMskTimeZone());
        calendar.setTimeInMillis(time);
        return calendar;
    }

    @WorkerThread
    @NonNull
    public List<TimetableEntry> get(@NonNull String fromStationId,
                                    @NonNull String toStationId,
                                    @NonNull Calendar dateMsk)
            throws FileNotFoundException {

        SQLiteDatabase db = TimetableDatabaseHelper.getInstance(context, version).getReadableDatabase();
        String[] projection;
        if (version == DataSchemeVersion.V1) {
            projection = new String[] {TimetableContract.Timetable.DEPARTURE_STATION_ID,
                    TimetableContract.Timetable.DEPARTURE_STATION_NAME,
                    TimetableContract.Timetable.DEPARTURE_TIME,
                    TimetableContract.Timetable.ARRIVAL_STATION_ID,
                    TimetableContract.Timetable.ARRIVAL_STATION_NAME,
                    TimetableContract.Timetable.ARRIVAL_TIME,
                    TimetableContract.Timetable.TRAIN_ROUTE_ID,
                    TimetableContract.Timetable.ROUTE_START_STATION_NAME,
                    TimetableContract.Timetable.ROUTE_END_STATION_NAME};
        } else {
            projection = new String[] {TimetableContract.Timetable.DEPARTURE_STATION_ID,
                    TimetableContract.Timetable.DEPARTURE_STATION_NAME,
                    TimetableContract.Timetable.DEPARTURE_TIME,
                    TimetableContract.Timetable.ARRIVAL_STATION_ID,
                    TimetableContract.Timetable.ARRIVAL_STATION_NAME,
                    TimetableContract.Timetable.ARRIVAL_TIME,
                    TimetableContract.Timetable.TRAIN_ROUTE_ID,
                    TimetableContract.Timetable.ROUTE_START_STATION_NAME,
                    TimetableContract.Timetable.ROUTE_END_STATION_NAME,
                    TimetableContract.Timetable.TRAIN_NAME};
        }

        List<TimetableEntry> timetable = new ArrayList<>();

        String selection = TimetableContract.Timetable.DEPARTURE_STATION_ID + "=? AND "
                + TimetableContract.Timetable.ARRIVAL_STATION_ID + "=? AND "
                + TimetableContract.Timetable.DEPARTURE_DATE + "=?";

        Cursor cursor = null;
        try {
            cursor = db.query(TimetableContract.Timetable.TABLE, projection, selection,
                    new String[] {fromStationId, toStationId,
                            Long.toString(dateMsk.get(Calendar.YEAR) * 1000 + dateMsk.get(Calendar.DAY_OF_YEAR))},
                    null, null, null);
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
                    String trainName;
                    if (version == DataSchemeVersion.V2) {
                        trainName = cursor.getString(9);
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
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return timetable;
    }

    @WorkerThread
    public void put(@NonNull String fromStationId,
                    @NonNull String toStationId,
                    @NonNull Calendar dateMsk,
                    @NonNull List<TimetableEntry> timetable) {
        SQLiteDatabase db = TimetableDatabaseHelper.getInstance(context, version).getWritableDatabase();
        String insertion = "INSERT INTO " + TimetableContract.Timetable.TABLE + " ("
                + TimetableContract.Timetable.DEPARTURE_DATE + ", "
                + TimetableContract.Timetable.DEPARTURE_STATION_ID + ", "
                + TimetableContract.Timetable.DEPARTURE_STATION_NAME + ", "
                + TimetableContract.Timetable.DEPARTURE_TIME + ", "
                + TimetableContract.Timetable.ARRIVAL_STATION_ID + ", "
                + TimetableContract.Timetable.ARRIVAL_STATION_NAME + ", "
                + TimetableContract.Timetable.ARRIVAL_TIME + ", "
                + TimetableContract.Timetable.TRAIN_ROUTE_ID + ", "
                + TimetableContract.Timetable.ROUTE_START_STATION_NAME + ", "
                + TimetableContract.Timetable.ROUTE_END_STATION_NAME;
        if (version == DataSchemeVersion.V1) {
            insertion += ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        } else {
            insertion += ", " + TimetableContract.Timetable.TRAIN_NAME + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        }

        db.beginTransaction();
        try {
            SQLiteStatement insert = db.compileStatement(insertion);
            for (TimetableEntry entry : timetable) {
                bindEntry(insert, entry, dateMsk);
                insert.executeInsert();
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }
    private void bindEntry(SQLiteStatement insert, TimetableEntry entry, Calendar date) {
        int i = 0;
        insert.bindLong(++i, date.get(Calendar.DAY_OF_YEAR) + date.get(Calendar.YEAR) * 500);
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
    }

    final String TAG = "TimetableCache";
}