package ru.ifmo.droid2016.rzddemo.cache;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.support.annotation.AnyThread;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;
import android.util.Log;

import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import ru.ifmo.droid2016.rzddemo.database.TimeDBHelper;
import ru.ifmo.droid2016.rzddemo.model.TimetableEntry;
import ru.ifmo.droid2016.rzddemo.utils.TimeUtils;

import static ru.ifmo.droid2016.rzddemo.database.TimeDBHelper.TimeTableStrings.*;

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
    //private final TimeDBHelper dbHelper;

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
        //dbHelper = new TimeDBHelper(context, version);
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
        Log.e("Cache", "onGet version:"+version);
        SQLiteDatabase db = TimeDBHelper.getInstance(context,version).getReadableDatabase();//dbHelper.getReadableDatabase();
        List<TimetableEntry> returnValues = new ArrayList<>();
        Cursor cursor = null;
        //ContentValues contentValues = new ContentValues();
        String[] find;
        if (version == DataSchemeVersion.V1)
            find = new String[]{
                    DEPARTURE_TIME, ARRIVAL_TIME, ROUTE_ID, ROUTE_FROM, ROUTE_TO, DEPARTURE_STATION_ID,
                    DEPARTURE_STATION, ARRIVAL_STATION_ID, ARRIVAL_STATION
            };
            else
            find = new String[]{
                    DEPARTURE_TIME, ARRIVAL_TIME, ROUTE_ID, ROUTE_FROM, ROUTE_TO, DEPARTURE_STATION_ID,
                    DEPARTURE_STATION, ARRIVAL_STATION_ID, ARRIVAL_STATION, TRAIN_NAME
            };
        try {
            cursor = db.query(TABLE_TIMELINES,
                    find,
                    DEPARTURE_STATION_ID + "=? and " + ARRIVAL_STATION_ID + "=? and " + DATE + "=?",
                    new String[]{fromStationId, toStationId,
                            Long.toString(dateMsk.get(Calendar.DAY_OF_YEAR) + dateMsk.get(Calendar.YEAR) * 365)},
                    null, null, null);
            if (cursor.moveToFirst())
                for (; !cursor.isAfterLast(); cursor.moveToNext()) {
                    long departureTimeSup = cursor.getLong(0);
                    Calendar departureTime = Calendar.getInstance(TimeUtils.getMskTimeZone());
                    departureTime.setTime(new Date(departureTimeSup));
                    long arrivalTimeSup = cursor.getLong(1);
                    Calendar arrivalTime = Calendar.getInstance(TimeUtils.getMskTimeZone());
                    arrivalTime.setTime(new Date(arrivalTimeSup));
                    String trainRouteId = cursor.getString(2);
                    String routeStartStationName = cursor.getString(3);
                    String routeEndStationName = cursor.getString(4);
                    String departureStationId = cursor.getString(5);
                    String departureStationName = cursor.getString(6);
                    String arrivalStationId = cursor.getString(7);
                    String arrivalStationName = cursor.getString(8);
                    String trainName = null;
                    if (version == DataSchemeVersion.V2)
                        trainName = cursor.getString(9);
                    returnValues.add(new TimetableEntry(departureStationId, departureStationName,
                            departureTime,
                            arrivalStationId, arrivalStationName,
                            arrivalTime,
                            trainRouteId, trainName, routeStartStationName, routeEndStationName
                    ));
                }
            else {
                throw new FileNotFoundException("No data in timetable cache for: fromStationId="
                        + fromStationId + ", toStationId=" + toStationId
                        + ", dateMsk=" + LOG_DATE_FORMAT.format(dateMsk.getTime()));
            }
        } finally {
            if (cursor != null)
                cursor.close();
            db.close();
        }
        return returnValues;
    }

    @WorkerThread
    public void put(@NonNull String fromStationId,
                    @NonNull String toStationId,
                    @NonNull Calendar dateMsk,
                    @NonNull List<TimetableEntry> timetable) {
        Log.e("Cache", "onPut version:"+version);
        SQLiteDatabase db = TimeDBHelper.getInstance(context,version).getWritableDatabase(); //dbHelper.getWritableDatabase();
        SQLiteStatement statement = null;
        db.beginTransaction();
        try {
            statement = db.compileStatement("insert into " + TABLE_TIMELINES + " (" +
                    COUNT_SIMPLE_COLUMS +
                    (version == DataSchemeVersion.V2 ? ", " + TRAIN_NAME + ") VALUES(?, "
                            : ") VALUES(") +
                    "?, ?, ?, ?, ?, ?, ?, ?, ?, ?" + ")");

            for (TimetableEntry entry : timetable) {
                statement.bindLong(1, dateMsk.get(Calendar.DAY_OF_YEAR) + dateMsk.get(Calendar.YEAR) * 365);
                statement.bindLong(2, entry.departureTime.getTimeInMillis());
                statement.bindLong(3, entry.arrivalTime.getTimeInMillis());
                statement.bindString(4, entry.trainRouteId);
                statement.bindString(5, entry.routeStartStationName);
                statement.bindString(6, entry.routeEndStationName);
                statement.bindString(7, entry.departureStationId);
                statement.bindString(8, entry.departureStationName);
                statement.bindString(9, entry.arrivalStationId);
                statement.bindString(10, entry.arrivalStationName);
                if (version == DataSchemeVersion.V2)
                    if (entry.trainName!=null)
                        statement.bindString(11, entry.trainName);
                    else
                        statement.bindNull(11);
                statement.executeInsert();
            }
            db.setTransactionSuccessful();
        } finally {
            try{
                if (statement != null)
                    statement.close();
            } catch(Exception e){
                e.printStackTrace();
            }
            db.endTransaction();
            db.close();
        }

    }
}
