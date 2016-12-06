package ru.ifmo.droid2016.rzddemo.cache;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.ContactsContract;
import android.support.annotation.AnyThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.util.Log;

import java.io.FileNotFoundException;
import java.sql.Date;
import java.text.SimpleDateFormat;
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


        Log.d(LOG_TAG, "get: ");

        SQLiteDatabase db = null;

        try {
            db = mySQLHelper.getReadableDatabase();

            Cursor cursor = null;

            try {
                cursor = db.query(
                        MySQLiteOpenHelper.TABLE_NAME,
                        new String[]{
                                MySQLiteOpenHelper.DEPARTURES_STATION_ID,
                                MySQLiteOpenHelper.DEPARTURES_STATION_NAME,
                                MySQLiteOpenHelper.DEPARTURES_TIME,

                                MySQLiteOpenHelper.ARRIVAL_STATION_ID,
                                MySQLiteOpenHelper.ARRIVAL_STATION_NAME,
                                MySQLiteOpenHelper.ARRIVAL_TIME,
                        },
                        MySQLiteOpenHelper.DEPARTURES_STATION_ID + "=?",
                        new String[]{String.valueOf(fromStationId)},
                        null, null, null
                );

                if ((cursor != null) && (cursor.moveToFirst())) {
                    SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.ENGLISH);

                    for (; !cursor.isAfterLast(); cursor.moveToNext()) {

                        final String departureStationId = cursor.getString(0);
                        final String departureStationName = cursor.getString(1);

                        final Calendar departureTime = Calendar.getInstance();
                        departureTime.setTime(sdf.parse(cursor.getString(2)));

                        final String arrivalStationId = cursor.getString(3);
                        final String arrivalStationName = cursor.getString(4);

                        final Calendar arrivalTime = Calendar.getInstance();
                        arrivalTime.setTime(sdf.parse(cursor.getString(5)));

                        final String trainRouteId;
                        final String trainName;
                        final String routeStartStationName;
                        final String routeEndStationName;


                        Log.d(LOG_TAG, "res: \n"
                                + departureStationId + "\n"
                                + departureStationName + "\n"
                                + departureTime.getTime() + "\n"

                                + arrivalStationId + "\n"
                                + arrivalStationName + "\n"
                                + arrivalTime.getTime() + "\n"
                        );


                    }
                }

            } catch (Exception ex) {



            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }



        } finally {
            if (db != null) {
                db.close();
            }
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

        SQLiteDatabase db = null;

        try {
            db = mySQLHelper.getWritableDatabase();

            //TODO : Явная транзакция + SQLiteStatement

            for (TimetableEntry e : timetable) {
                ContentValues values = new ContentValues();

                values.put(MySQLiteOpenHelper.DEPARTURES_STATION_ID, e.departureStationId);
                values.put(MySQLiteOpenHelper.DEPARTURES_STATION_NAME, e.routeEndStationName);
                values.put(MySQLiteOpenHelper.DEPARTURES_TIME, String.valueOf(e.departureTime.getTime()));

                values.put(MySQLiteOpenHelper.ARRIVAL_STATION_ID, e.arrivalStationId);
                values.put(MySQLiteOpenHelper.ARRIVAL_STATION_NAME, e.arrivalStationName);
                values.put(MySQLiteOpenHelper.ARRIVAL_TIME, String.valueOf(e.arrivalTime.getTime()));




                db.insert(
                        MySQLiteOpenHelper.TABLE_NAME,
                        null,
                        values
                );

                Log.d(LOG_TAG, "insert");
            }

        } finally {
            if (db != null) {
                db.close();
            }
        }

        // TODO: ДЗ - реализовать кэш на основе базы данных SQLite с учетом версии модели данных
    }
}
