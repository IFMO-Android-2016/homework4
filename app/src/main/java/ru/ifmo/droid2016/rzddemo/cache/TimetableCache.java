package ru.ifmo.droid2016.rzddemo.cache;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.hardware.camera2.CameraConstrainedHighSpeedCaptureSession;
import android.support.annotation.AnyThread;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;
import android.util.Log;

import java.io.CharConversionException;
import java.io.FileNotFoundException;
import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import javax.security.auth.callback.CallbackHandler;

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

    private static final String LOG_TAG = TimetableCache.class.getSimpleName();
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

        DBHelper helper = DBHelper.getInstance(context, version);
        SQLiteDatabase db = helper.getReadableDatabase();

        Cursor cursor = null;
        String[] proj;

        Log.d(LOG_TAG, Long.toString(dateMsk.getTimeInMillis()));
        Log.d(LOG_TAG, dateMsk.getTime().toString());
        if (version == DataSchemeVersion.V1) proj = CacheContract.ARGS_V1;
        else proj = CacheContract.ARGS_V2;
        ArrayList<TimetableEntry> answer = new ArrayList<>();
        try {
            cursor = db.query(
                    CacheContract.TABLE_NAME,
                    proj,
                    CacheContract.FROM_STATION_ID + "=? AND " +
                            CacheContract.TO_STATION_ID + "=? AND " + CacheContract.DATE_MSK + "=?",
                    new String[] { fromStationId, toStationId, LOG_DATE_FORMAT.format(dateMsk.getTime())},
                    null,
                    null,
                    null
            );


            String trainName;
            if (cursor != null && cursor.moveToFirst()) {
                for (; !cursor.isAfterLast(); cursor.moveToNext()) {
                    if (version == DataSchemeVersion.V1) {
                        trainName = null;
                    }
                    else {
                        trainName = cursor.getString(13);
                    }
                    TimetableEntry entry = new TimetableEntry(
                            cursor.getString(4),
                            cursor.getString(5),
                            new GregorianCalendar(),
                            cursor.getString(7),
                            cursor.getString(8),
                            new GregorianCalendar(),
                            cursor.getString(10),
                            trainName,
                            cursor.getString(11),
                            cursor.getString(12));
                    entry.departureTime.setTimeInMillis(cursor.getLong(6));
                    entry.arrivalTime.setTimeInMillis(cursor.getLong(9));
                    answer.add(entry);
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        if (answer.isEmpty()) {
            throw new FileNotFoundException("No data in timetable cache for: fromStationId="
                    + fromStationId + ", toStationId=" + toStationId
                    + ", dateMsk=" + LOG_DATE_FORMAT.format(dateMsk.getTime()));
        }
        Log.d(LOG_TAG, "Getting");
        return answer;
    }



    @WorkerThread
    public void put(@NonNull String fromStationId,
                    @NonNull String toStationId,
                    @NonNull Calendar dateMsk,
                    @NonNull List<TimetableEntry> timetable) {
        DBHelper helper = DBHelper.getInstance(context, version);
        SQLiteDatabase db = helper.getWritableDatabase();
        SQLiteStatement insert = null;
        db.beginTransaction();
        Log.d(LOG_TAG, "Putting");
        try {
            if (version == DataSchemeVersion.V1) {
                insert = db.compileStatement("INSERT INTO " + CacheContract.TABLE_NAME + " ("
                        + CacheContract.getArgs(version) + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
            } else {
                Log.d(LOG_TAG, "INSERT INTO " + CacheContract.TABLE_NAME + " ("
                                + CacheContract.getArgs(version) + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
                insert = db.compileStatement("INSERT INTO " + CacheContract.TABLE_NAME + " ("
                        + CacheContract.getArgs(version) + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
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