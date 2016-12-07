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

    private static final String TAG = "TTC";

    @NonNull
    private final Context ctx;

    /**
     * Версия модели данных, с которой работает кэщ.
     */
    @DataSchemeVersion
    private final int ver;

    /**
     * Создает экземпляр кэша с указанной версией модели данных.
     *
     * Может вызываться на лююбом (в том числе UI потоке). Может быть создано несколько инстансов
     * {@link TimetableCache} -- все они должны потокобезопасно работать с одним физическим кэшом.
     */
    @AnyThread
    public TimetableCache(@NonNull Context ctx,
                          @DataSchemeVersion int ver) {
        this.ctx = ctx.getApplicationContext();
        this.ver = ver;
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

        TimetableReaderDBHelper helper = TimetableReaderDBHelper.getInstance(ctx, ver);
        SQLiteDatabase db = helper.getReadableDatabase();

        Cursor ptr = null;

        String[] projection = (ver == DataSchemeVersion.V1) ? TimetableReaderDBHelper.Scheme.TT_V1 :
                TimetableReaderDBHelper.Scheme.TT_V2;
        ArrayList<TimetableEntry> list = new ArrayList<>();
        try {
            ptr = db.query(
                    TimetableReaderDBHelper.Scheme.TABLE_NAME,
                    projection,
                    TimetableReaderDBHelper.Scheme.COLUMN_NAME_DEP_ST_ID + "=? AND " +
                            TimetableReaderDBHelper.Scheme.COLUMN_NAME_ARR_ST_ID + "=? AND " + TimetableReaderDBHelper.Scheme.COLUMN_NAME_DATE_ST + "=?",
                    new String[] { fromStationId, toStationId, LOG_DATE_FORMAT.format(dateMsk.getTime())},
                    null,
                    null,
                    null
            );


            if (ptr != null && ptr.moveToFirst()) {
                for (; !ptr.isAfterLast(); ptr.moveToNext()) {
                    TimetableEntry entry = new TimetableEntry(
                            ptr.getString(0),
                            ptr.getString(1),
                            new GregorianCalendar(),
                            ptr.getString(3),
                            ptr.getString(4),
                            new GregorianCalendar(),
                            ptr.getString(6),
                            (ver == DataSchemeVersion.V1) ? null : ptr.getString(12),
                            ptr.getString(7),
                            ptr.getString(8));
                    entry.departureTime.setTimeInMillis(ptr.getLong(2));
                    entry.arrivalTime.setTimeInMillis(ptr.getLong(5));

                    list.add(entry);
                }
            }
        } finally {
            if (ptr != null) {
                ptr.close();
            }
        }
        if (list.isEmpty()) {
            throw new FileNotFoundException("No data in timetable cache for: fromStationId="
                    + fromStationId + ", toStationId=" + toStationId
                    + ", dateMsk=" + LOG_DATE_FORMAT.format(dateMsk.getTime()));
        }


        return list;
    }

    @WorkerThread
    public void put(@NonNull String fromStationId,
                    @NonNull String toStationId,
                    @NonNull Calendar dateMsk,
                    @NonNull List<TimetableEntry> timetable) {
        // TODO: ДЗ - реализовать кэш на основе базы данных SQLite с учетом версии модели данных


        TimetableReaderDBHelper helper = TimetableReaderDBHelper.getInstance(ctx, ver);
        SQLiteDatabase db = helper.getWritableDatabase();
        SQLiteStatement insStatement = null;

        Log.d(TAG, "put: init put");

        db.beginTransaction();
        try {
            if (ver == DataSchemeVersion.V1) {
                insStatement = db.compileStatement("INSERT INTO " + TimetableReaderDBHelper.Scheme.TABLE_NAME + " ("
                        + TimetableReaderDBHelper.Scheme.select(ver) + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
            } else {
                insStatement = db.compileStatement("INSERT INTO " + TimetableReaderDBHelper.Scheme.TABLE_NAME + " ("
                        + TimetableReaderDBHelper.Scheme.select(ver) + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
            }

            Log.d(TAG, "put: insStatement: " + insStatement.toString());

            insStatement.bindString(10, LOG_DATE_FORMAT.format(dateMsk.getTime()));
            insStatement.bindString(11, fromStationId);
            insStatement.bindString(12, toStationId);
            for (int i = 0; i < timetable.size(); ++i) {
                TimetableEntry e = timetable.get(i);
                insStatement.bindString(1, e.departureStationId);
                insStatement.bindString(2, e.departureStationName);
                insStatement.bindLong(3, e.departureTime.getTimeInMillis());
                insStatement.bindString(4, e.arrivalStationId);
                insStatement.bindString(5, e.arrivalStationName);
                insStatement.bindLong(6, e.arrivalTime.getTimeInMillis());
                insStatement.bindString(7, e.trainRouteId);
                insStatement.bindString(8, e.routeStartStationName);
                insStatement.bindString(9, e.routeEndStationName);
                if (ver == DataSchemeVersion.V2 && e.trainName != null) {
                    insStatement.bindString(13, e.trainName);
                }
                insStatement.executeInsert();
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();

            if (insStatement != null) {
                insStatement.close();
            }
        }
    }
}
