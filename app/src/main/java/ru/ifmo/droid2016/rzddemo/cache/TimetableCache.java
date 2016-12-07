package ru.ifmo.droid2016.rzddemo.cache;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteStatement;
import android.support.annotation.AnyThread;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import ru.ifmo.droid2016.rzddemo.model.TimetableEntry;
import ru.ifmo.droid2016.rzddemo.utils.TimeUtils;

import static ru.ifmo.droid2016.rzddemo.Constants.LOG_DATE_FORMAT;
import static ru.ifmo.droid2016.rzddemo.cache.TimetableContract.Timetable.CREATETABLEV1;
import static ru.ifmo.droid2016.rzddemo.cache.TimetableContract.Timetable.CREATETABLEV2;
import static ru.ifmo.droid2016.rzddemo.cache.TimetableContract.Timetable.TABLE;
import static ru.ifmo.droid2016.rzddemo.cache.TimetableContract.Timetable.TABLEV1;
import static ru.ifmo.droid2016.rzddemo.cache.TimetableContract.TimetableColumns.*;

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
     * Может вызываться на любом (в том числе UI потоке). Может быть создано несколько инстансов
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
        SQLiteDatabase db = TimetableDBHelper.getInstance(context, version).getReadableDatabase();
        String pr = DEPARTURESTATIONID + "=? AND " + ARRIVALSTATIONID + "=? AND " + DEPARTUREDATE + "=?";
        String[] args = new String[]{fromStationId, toStationId, String.valueOf(getDate(dateMsk))};
        String[] columns = new String[TABLEV1.length + (version == DataSchemeVersion.V1 ? 0 : 1)];
        System.arraycopy(TABLEV1, 0, columns, 0, TABLEV1.length);

        if (version == DataSchemeVersion.V2)
            columns[columns.length - 1] = TRAINNAME;
        Cursor c = null;
        try {
            c = db.query(TABLE, columns, pr, args, null, null, null);
            if (c != null && c.moveToFirst()) {
                ArrayList<TimetableEntry> ans = new ArrayList<>();
                while (!c.isAfterLast()) {

                    TimetableEntry cur = new TimetableEntry(
                            c.getString(1),
                            c.getString(2),
                            getTime(c.getLong(3)),
                            c.getString(4),
                            c.getString(5),
                            getTime(c.getLong(6)),
                            c.getString(7),
                            (version == DataSchemeVersion.V2 ? c.getString(10) : null),
                            c.getString(8),
                            c.getString(9));

                    ans.add(cur);
                    c.moveToNext();
                }
                return ans;
            } else {
                throw new FileNotFoundException("No data in timetable cache for: fromStationId="
                        + fromStationId + ", toStationId=" + toStationId
                        + ", dateMsk=" + LOG_DATE_FORMAT.format(dateMsk.getTime()));
            }
        } catch (SQLiteException ignored) {
            throw new FileNotFoundException("No data in timetable cache for: fromStationId="
                    + fromStationId + ", toStationId=" + toStationId
                    + ", dateMsk=" + LOG_DATE_FORMAT.format(dateMsk.getTime()));
        } finally {
            if (c != null) {
                c.close();
            }
            db.close();
        }

    }

    @WorkerThread
    public void put(@NonNull String fromStationId,
                    @NonNull String toStationId,
                    @NonNull Calendar dateMsk,
                    @NonNull List<TimetableEntry> timetable) {
        SQLiteDatabase db = TimetableDBHelper.getInstance(context, version).getWritableDatabase();
        db.beginTransaction();
        SQLiteStatement in = null;
        try {
            String statement = "INSERT INTO " + TABLE + " (";
            String requestType = ") VALUES(";
            for (int i = 0; i < TABLEV1.length; i++) {
                statement += TABLEV1[i];
                requestType += "?";
                if (i != TABLEV1.length - 1) {
                    statement += ", ";
                    requestType += ", ";
                }
            }
            if (version == DataSchemeVersion.V2) {
                statement += ", " + TRAINNAME;
                requestType += ", ?";
            }
            in = db.compileStatement(statement + requestType + ")");
            for (TimetableEntry time : timetable) {
                in.bindLong(1, getDate(dateMsk));
                in.bindString(2, time.departureStationId);
                in.bindString(3, time.departureStationName);
                in.bindLong(4, time.departureTime.getTimeInMillis());
                in.bindString(5, time.arrivalStationId);
                in.bindString(6, time.arrivalStationName);
                in.bindLong(7, time.arrivalTime.getTimeInMillis());
                in.bindString(8, time.trainRouteId);
                in.bindString(9, time.routeStartStationName);
                in.bindString(10, time.routeEndStationName);
                if (version == DataSchemeVersion.V2) {
                    if (time.trainName == null) {
                        in.bindNull(11);
                    } else {
                        in.bindString(11, time.trainName);
                    }
                }
                in.executeInsert();
            }
            db.setTransactionSuccessful();
        } finally {
            try {
                if (in != null)
                    in.close();
            } catch (Exception ignored) {
            }
            db.endTransaction();
        }
        db.close();
    }

    private Calendar getTime(long aLong) {
        Calendar c = Calendar.getInstance(TimeUtils.getMskTimeZone());
        c.setTime(new Date(aLong));
        return c;
    }

    private long getDate(@NonNull Calendar dateMSK) {
        return dateMSK.get(Calendar.DAY_OF_YEAR) + dateMSK.get(Calendar.YEAR) * 500;
    }
}
