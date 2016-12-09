package ru.ifmo.droid2016.rzddemo.cache;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteStatement;
import android.os.Build;
import android.provider.ContactsContract;
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


    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @WorkerThread
    @NonNull
    public List<TimetableEntry> get(@NonNull String fromStationId,
                                    @NonNull String toStationId,
                                    @NonNull Calendar dateMsk)
            throws FileNotFoundException {
        List<TimetableEntry> result = new ArrayList<>();
        SQLiteDatabase db = TimetableCacheDbHelper.getInstance(context, version).getReadableDatabase();
        String[] everything;
        if (version == DataSchemeVersion.V1) {
            everything = TimetableCacheContract.Timetable.Everything_V1;
        }
        else {
            everything = TimetableCacheContract.Timetable.Everything_V2;
        }
        try (Cursor q = db.query(
                TimetableCacheContract.Timetable.TABLE,
                everything,
                TimetableCacheContract.Timetable.DEPARTURE_STATION_ID + "=? AND " + TimetableCacheContract.Timetable.ARRIVAL_STATION_ID + "=? AND " + TimetableCacheContract.Timetable.ROUTE_DATE + "=?",
                new String[]{fromStationId, toStationId, Long.toString(dateMsk.get(Calendar.DAY_OF_YEAR) + dateMsk.get(Calendar.YEAR) * 500)},
                null,
                null,
                null
        )) {
            if (q != null && q.moveToFirst()) {
                while (!q.isAfterLast()) {
                    int i = 0;
                    String departure_station_id = q.getString(i);
                    i++;
                    String departure_station_name = q.getString(i);
                    i++;
                    Calendar departure_time = Calendar.getInstance(TimeUtils.getMskTimeZone());
                    departure_time.setTime(new Date(q.getLong(i)));
                    i++;
                    String arrival_station_id = q.getString(i);
                    i++;
                    String arrival_station_name = q.getString(i);
                    i++;
                    Calendar arrival_time = Calendar.getInstance(TimeUtils.getMskTimeZone());
                    arrival_time.setTime(new Date(q.getLong(i)));
                    i++;
                    String train_route_id = q.getString(i);
                    i++;
                    String route_start_station_name = q.getString(i);
                    i++;
                    String route_end_station_name = q.getString(i);
                    String train_route = null;
                    if (version == DataSchemeVersion.V2) {
                        i++;
                        train_route = q.getString(i);
                    }
                    TimetableEntry neww = new TimetableEntry(departure_station_id, departure_station_name, departure_time, arrival_station_id, arrival_station_name, arrival_time, train_route_id, train_route, route_start_station_name, route_end_station_name);
                    result.add(neww);
                    q.moveToNext();
                }
            }
            else {
                throw new FileNotFoundException("No data in timetable cache for: fromStationId="
                        + fromStationId + ", toStationId=" + toStationId
                        + ", dateMsk=" + LOG_DATE_FORMAT.format(dateMsk.getTime()));
            }
        } catch (SQLiteException e) {
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
        SQLiteDatabase db = TimetableCacheDbHelper.getInstance(context, version).getWritableDatabase();
        db.beginTransaction();
        String insert = "INSERT INTO " + TimetableCacheContract.Timetable.TABLE + " ("
                + TimetableCacheContract.Timetable.ROUTE_DATE + ", "
                + TimetableCacheContract.Timetable.DEPARTURE_STATION_ID + ", "
                + TimetableCacheContract.Timetable.DEPARTURE_STATION_NAME + ", "
                + TimetableCacheContract.Timetable.DEPARTURE_TIME + ", "
                + TimetableCacheContract.Timetable.ARRIVAL_STATION_ID + ", "
                + TimetableCacheContract.Timetable.ARRIVAL_STATION_NAME + ", "
                + TimetableCacheContract.Timetable.ARRIVAL_TIME + ", "
                + TimetableCacheContract.Timetable.TRAIN_ROUTE_ID + ", "
                + TimetableCacheContract.Timetable.ROUTE_START_STATION_NAME + ", "
                + TimetableCacheContract.Timetable.ROUTE_END_STATION_NAME;
        if (version == DataSchemeVersion.V2) {
            insert += ", " + TimetableCacheContract.Timetable.TRAIN_NAME + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            Log.wtf("KAPA", insert);
        }
        else {
            insert += ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        }
        try (SQLiteStatement neww = db.compileStatement(insert)) {
            for (int i = 0; i < timetable.size(); i++) {
                int cur = 1;
                neww.bindLong(cur, dateMsk.get(Calendar.DAY_OF_YEAR) + dateMsk.get(Calendar.YEAR) * 500);
                cur++;
                neww.bindString(cur, timetable.get(i).departureStationId);
                cur++;
                neww.bindString(cur, timetable.get(i).departureStationName);
                cur++;
                neww.bindLong(cur, timetable.get(i).departureTime.getTimeInMillis());
                cur++;
                neww.bindString(cur, timetable.get(i).arrivalStationId);
                cur++;
                neww.bindString(cur, timetable.get(i).arrivalStationName);
                cur++;
                neww.bindLong(cur, timetable.get(i).arrivalTime.getTimeInMillis());
                cur++;
                neww.bindString(cur, timetable.get(i).trainRouteId);
                cur++;
                neww.bindString(cur, timetable.get(i).routeStartStationName);
                cur++;
                neww.bindString(cur, timetable.get(i).routeEndStationName);
                if (version == DataSchemeVersion.V2) {
                    cur++;
                    if (timetable.get(i).trainName != null) {
                        neww.bindString(cur, timetable.get(i).trainName);
                    } else {
                        neww.bindNull(cur);
                    }
                }
                neww.executeInsert();
            }
            db.setTransactionSuccessful();
        }
        finally {
            db.endTransaction();
        }
    }
}
