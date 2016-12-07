package ru.ifmo.droid2016.rzddemo.cache;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.provider.ContactsContract;
import android.support.annotation.AnyThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.util.Log;
import android.widget.ProgressBar;

import java.io.FileNotFoundException;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

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
    final String LOG_TAG = "cache_log";


    private MySQLiteOpenHelper mySQLHelper;

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

        //mySQLHelper.init();

        mySQLHelper = MySQLiteOpenHelper.getInstance(context, version);
    }




    String getMask(Calendar calendar) {
        StringBuilder res = new StringBuilder(calendar.getTime().toString());
        return res.replace(11, 19, "__:__:__").toString();
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

        Log.d(LOG_TAG, "get: " + dateMsk.getTime());


        String mask = getMask(dateMsk);
        Log.d(LOG_TAG, mask);

        List<TimetableEntry> res = new ArrayList<>();
        SQLiteDatabase db = mySQLHelper.getReadableDatabase();
        Cursor cursor = null;

        Log.d(LOG_TAG, "1");

        try {
            cursor = db.query(
                    MySQLiteOpenHelper.TABLE_NAME,
                    MySQLiteOpenHelper.getColonsNames(version),

                    MySQLiteOpenHelper.DEPARTURES_STATION_ID + "=? AND "
                    + MySQLiteOpenHelper.ARRIVAL_STATION_ID + "=? AND "
                    + MySQLiteOpenHelper.DEPARTURES_TIME + " LIKE ?"
                    ,
                    new String[]{
                            String.valueOf(fromStationId),
                            String.valueOf(toStationId),
                            mask
                    },
                    null, null, null
            );

            Log.d(LOG_TAG, "2");

            if ((cursor != null) && (cursor.moveToFirst())) {
                SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.ENGLISH);

                Log.d(LOG_TAG, "3");

                for (; !cursor.isAfterLast(); cursor.moveToNext()) {

                    Log.d(LOG_TAG, "++++++++++++");

                    final String departureStationId = cursor.getString(0);
                    final String departureStationName = cursor.getString(1);

                    final Calendar departureTime = Calendar.getInstance();
                    departureTime.setTime(sdf.parse(cursor.getString(2)));

                    final String arrivalStationId = cursor.getString(3);
                    final String arrivalStationName = cursor.getString(4);

                    Log.d(LOG_TAG, "???");

                    final Calendar arrivalTime = Calendar.getInstance();
                    arrivalTime.setTime(sdf.parse(cursor.getString(5)));

                    Log.d(LOG_TAG, "???");

                    final String trainRouteId = cursor.getString(6);
                    final String routeStartStationName = cursor.getString(7);
                    final String routeEndStationName = cursor.getString(8);

                    Log.d(LOG_TAG, "???");

                    String trainName = null;
                    if (version != DataSchemeVersion.V1) {

                        trainName = cursor.getString(9);

                        if ((trainName != null) && (trainName.equals("NULL"))) {
                            trainName = null;
                        }
                    }

                    Log.d(LOG_TAG, "?!?!?!!");

                    Log.d(LOG_TAG, "res: \n"
                            + departureStationId + "\n"
                            + departureStationName + "\n"
                            + departureTime.getTime() + "\n"

                            + arrivalStationId + "\n"
                            + arrivalStationName + "\n"
                            + arrivalTime.getTime() + "\n"

                            + trainRouteId + "\n"

                            + trainName + "\n"

                            + routeStartStationName + "\n"
                            + routeEndStationName + "\n"
                    );


                    res.add(new TimetableEntry(
                            departureStationId,
                            departureStationName,
                            departureTime,

                            arrivalStationId,
                            arrivalStationName,
                            arrivalTime,

                            trainRouteId,
                            trainName,
                            routeStartStationName,
                            routeEndStationName
                    ));

                }
            }

        } catch (Exception ex) {
            Log.d(LOG_TAG, ex.toString());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        if (res.size() > 0) {
            return res;
        }

        // TODO: ДЗ - реализовать кэш на основе базы данных SQLite с учетом версии модели данных
        throw new FileNotFoundException("No data in timetable cache for: fromStationId="
                + fromStationId + ", toStationId=" + toStationId
                + ", dateMsk=" + LOG_DATE_FORMAT.format(dateMsk.getTime()));
    }

    @WorkerThread
    public void put(@NonNull String fromStationId,
                    @NonNull String toStationId,
                    @NonNull Calendar dateMsk,
                    @NonNull List<TimetableEntry> timetable) {

        Log.d(LOG_TAG, "put: ");

        SQLiteDatabase db = mySQLHelper.getWritableDatabase();
        SQLiteStatement statement = null;

        try {
            if (version == DataSchemeVersion.V1) {
                statement = db.compileStatement(
                        "INSERT INTO "
                                + MySQLiteOpenHelper.TABLE_NAME + " VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?);");
            } else {
                statement = db.compileStatement(
                        "INSERT INTO "
                                + MySQLiteOpenHelper.TABLE_NAME + " VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?);");
            }
            db.beginTransaction();

            try {

                for (TimetableEntry e : timetable) {
                    Date date;

                    statement.clearBindings();

                    statement.bindString(1, e.departureStationId);
                    statement.bindString(2, e.departureStationName);
                    date = e.departureTime.getTime();
                    date.setTime(date.getTime() + 60 * 60 * 1000 * 3);
                    statement.bindString(3, String.valueOf(date));

                    Log.d(LOG_TAG, String.valueOf(date)  + " TIME!!!");


                    statement.bindString(4, e.arrivalStationId);
                    statement.bindString(5, e.arrivalStationName);
                    date = e.arrivalTime.getTime();
                    date.setTime(date.getTime() + 60 * 60 * 1000 * 3);
                    statement.bindString(6, String.valueOf(date));

                    statement.bindString(7, e.trainRouteId);
                    statement.bindString(8, e.routeStartStationName);
                    statement.bindString(9, e.routeEndStationName);

                    if (version != DataSchemeVersion.V1) {
                        String res = e.trainName;
                        if (res == null) {
                            res = "NULL";
                        }
                        Log.d(LOG_TAG, res + " !!!!!!");
                        statement.bindString(10, res);
                    }

                    statement.execute();

                    Log.d(LOG_TAG,
                            e.departureStationId + "\n"
                            + e.departureStationName + "\n"


                            );

                }
                db.setTransactionSuccessful();

                Log.d(LOG_TAG, "OK_2");

            } finally {
                db.endTransaction();
            }

        } finally {
            if (statement != null) {
                statement.close();
            }
        }
        // TODO: ДЗ - реализовать кэш на основе базы данных SQLite с учетом версии модели данных
    }
}
