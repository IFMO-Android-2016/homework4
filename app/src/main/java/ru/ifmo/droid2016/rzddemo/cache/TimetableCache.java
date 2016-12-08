package ru.ifmo.droid2016.rzddemo.cache;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.AnyThread;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;
import android.util.Log;

import java.io.FileNotFoundException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import ru.ifmo.droid2016.rzddemo.model.TimetableEntry;
import ru.ifmo.droid2016.rzddemo.utils.TimeUtils;

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

    private BDHelper bdHelper;

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

        bdHelper = BDHelper.getInstance(context, "trainstime", version);
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
        List<TimetableEntry> res = new ArrayList<>();
        SQLiteDatabase db = bdHelper.getReadableDatabase();
        Cursor cursor = null;
        try {
            if (bdHelper.DB_VERSION == 1) {
                cursor = db.query(
                        bdHelper.TABLE_NAME,
                        new String[]{
                                bdHelper.DEPARTURES_ID, bdHelper.DEPARTURES_NAME, bdHelper.DEPARTURES_TIME,
                                bdHelper.ARRIVAL_ID, bdHelper.ARRIVAL_NAME, bdHelper.ARRIVAL_TIME,
                                bdHelper.TRAIN_ID,
                                bdHelper.START_NAME, bdHelper.FINISH_NAME,
                        },
                        bdHelper.DEPARTURES_ID + "=? AND "
                                + bdHelper.ARRIVAL_ID + "=? AND "
                                + bdHelper.DEPARTURES_TIME + " LIKE ?",
                        new String[]{
                                String.valueOf(fromStationId),
                                String.valueOf(toStationId),
                                dateMsk.getTime().toString()
                        },
                        null, null, null
                );
            } else {
                cursor = db.query(
                        bdHelper.TABLE_NAME,
                        new String[]{
                                bdHelper.DEPARTURES_ID, bdHelper.DEPARTURES_NAME, bdHelper.DEPARTURES_TIME,
                                bdHelper.ARRIVAL_ID, bdHelper.ARRIVAL_NAME, bdHelper.ARRIVAL_TIME,
                                bdHelper.TRAIN_ID, bdHelper.START_NAME,
                                bdHelper.FINISH_NAME, bdHelper.TRAIN_NAME,
                        },
                        bdHelper.DEPARTURES_ID + "=? AND "
                            + bdHelper.ARRIVAL_ID + "=? AND "
                            + bdHelper.DEPARTURES_TIME + " LIKE ?",
                        new String[]{
                                String.valueOf(fromStationId),
                                String.valueOf(toStationId),
                                dateMsk.getTime().toString()
                        },
                        null, null, null
                );
            }
            if (cursor != null && cursor.moveToFirst())
                for (; cursor.isAfterLast(); cursor.moveToNext()) {
                    final String departureID = cursor.getString(0);
                    final String departureName = cursor.getString(1);

                    Calendar departureTime = Calendar.getInstance(TimeUtils.getMskTimeZone());
                    final String[] dTime = cursor.getString(2).split(" ");
                    final String[] time = dTime[3].split(":");
                    int dMonth = 0;
                    switch (dTime[1]) {
                        case "Jan" : dMonth = 1;
                            break;
                        case "Feb" : dMonth = 2;
                            break;
                        case "Mar" : dMonth = 3;
                            break;
                        case "Apr" : dMonth = 4;
                            break;
                        case "May" : dMonth = 5;
                            break;
                        case "Jun" : dMonth = 6;
                            break;
                        case "Jul" : dMonth = 7;
                            break;
                        case "Aug" : dMonth = 8;
                            break;
                        case "Sep" : dMonth = 9;
                            break;
                        case "Oct" : dMonth = 10;
                            break;
                        case "Nov" : dMonth = 11;
                            break;
                        case "Dec" : dMonth = 12;
                            break;
                    }
                    departureTime.set(Integer.parseInt(dTime[5]), dMonth, Integer.parseInt(dTime[2]), Integer.parseInt(time[0]), Integer.parseInt(time[1]));

                    final String arrivalID = cursor.getString(3);
                    final String arrivalName = cursor.getString(4);

                    Calendar arrivalTime = Calendar.getInstance(TimeUtils.getMskTimeZone());
                    final String[] aTime = cursor.getString(5).split(" ");
                    String[] timeA = aTime[3].split(":");
                    int aMonth = 0;
                    switch (aTime[1]) {
                        case "Jan" : aMonth = 1;
                            break;
                        case "Feb" : aMonth = 2;
                            break;
                        case "Mar" : aMonth = 3;
                            break;
                        case "Apr" : aMonth = 4;
                            break;
                        case "May" : aMonth = 5;
                            break;
                        case "Jun" : aMonth = 6;
                            break;
                        case "Jul" : aMonth = 7;
                            break;
                        case "Aug" : aMonth = 8;
                            break;
                        case "Sep" : aMonth = 9;
                            break;
                        case "Oct" : aMonth = 10;
                            break;
                        case "Nov" : aMonth = 11;
                            break;
                        case "Dec" : aMonth = 12;
                            break;
                    }
                    departureTime.set(Integer.parseInt(aTime[5]), aMonth, Integer.parseInt(aTime[2]), Integer.parseInt(timeA[0]), Integer.parseInt(timeA[1]));

                    final String trainID = cursor.getString(6);
                    final String startName = cursor.getString(7);
                    final String finishName = cursor.getString(8);

                    String trainName = null;
                    if (bdHelper.DB_VERSION == 2) {
                        trainName = cursor.getString(9);
                    }

                    res.add(
                            new TimetableEntry(departureID, departureName, departureTime,
                                    arrivalID, arrivalName, arrivalTime,
                                    trainID, trainName,
                                    startName, finishName));
                }
        } catch (Exception e) {
        } finally {
            if (cursor != null)
                cursor.close();
        }

        if (res.size() > 0) {
            return res;
        }
        throw new FileNotFoundException("No data in timetable cache for: fromStationId="
                + fromStationId + ", toStationId=" + toStationId
                + ", dateMsk=" + LOG_DATE_FORMAT.format(dateMsk.getTime()));
    }

    @WorkerThread
    public void put(@NonNull String fromStationId,
                    @NonNull String toStationId,
                    @NonNull Calendar dateMsk,
                    @NonNull List<TimetableEntry> timetable) {
        SQLiteDatabase db = bdHelper.getWritableDatabase();
        String adding;
        if (bdHelper.DB_VERSION == 1) {
            adding = "INSERT INTO " + bdHelper.TABLE_NAME
                    + "(" + bdHelper.DEPARTURES_ID + ", "
                    + bdHelper.DEPARTURES_NAME + ", "
                    + bdHelper.DEPARTURES_TIME + ", "
                    + bdHelper.ARRIVAL_ID + ", "
                    + bdHelper.ARRIVAL_NAME + ", "
                    + bdHelper.ARRIVAL_TIME + ", "
                    + bdHelper.TRAIN_ID + ", "
                    + bdHelper.START_NAME + ", "
                    + bdHelper.FINISH_NAME + ") ";
            for (TimetableEntry value : timetable) {
                db.execSQL(adding + "VALUES (\'"
                        + value.departureStationId + "\', \'" + value.departureStationName + "\', \'" + value.departureTime.getTime().toString() + "\', \'"
                        + value.arrivalStationId + "\', \'" + value.arrivalStationName + "\', \'" + value.arrivalTime.getTime().toString() + "\', \'"
                        + value.trainRouteId + "\', \'"
                        + value.routeStartStationName + "\', \'" + value.routeEndStationName + "\');"
                );
            }
        } else {
            adding = "INSERT INTO " + bdHelper.TABLE_NAME
                    + "(" + bdHelper.DEPARTURES_ID + ", "
                    + bdHelper.DEPARTURES_NAME + ", "
                    + bdHelper.DEPARTURES_TIME + ", "
                    + bdHelper.ARRIVAL_ID + ", "
                    + bdHelper.ARRIVAL_NAME + ", "
                    + bdHelper.ARRIVAL_TIME + ", "
                    + bdHelper.TRAIN_ID + ", "
                    + bdHelper.START_NAME + ", "
                    + bdHelper.FINISH_NAME + ", "
                    + bdHelper.TRAIN_NAME + ") ";


            for (TimetableEntry value : timetable) {
                db.execSQL(adding + "VALUES (\'"
                        + value.departureStationId + "\', \'" + value.departureStationName + "\', \'" + value.departureTime.getTime().toString() + "\', \'"
                        + value.arrivalStationId + "\', \'" + value.arrivalStationName + "\', \'" + value.arrivalTime.getTime().toString() + "\', \'"
                        + value.trainRouteId + "\', \'"
                        + value.routeStartStationName + "\', \'" + value.routeEndStationName + "\', \'"
                        + value.trainName + "\');"
                );
            }
        }
    }
}
