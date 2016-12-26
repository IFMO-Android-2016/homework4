package ru.ifmo.droid2016.rzddemo.cache;

import android.content.Context;
import android.support.annotation.AnyThread;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;
import android.util.Log;

import java.io.FileNotFoundException;
<<<<<<< HEAD
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.LinkedList;
=======
import java.text.SimpleDateFormat;
import java.util.Calendar;
>>>>>>> parent of f47511d... rdy
import java.util.List;

import ru.ifmo.droid2016.rzddemo.model.TimetableEntry;

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
<<<<<<< HEAD
        final DBHelper helper = DBHelper.getOrCreateInstance(context, version);
        final SQLiteDatabase db = helper.getReadableDatabase();
        List<TimetableEntry> timetableEntryList = new LinkedList<>();
        Cursor c = null;
        long day = DBHelper.currentDay(dateMsk);
        Log.wtf("RZDDBG", Long.toString(day));
        db.beginTransaction();
        try {
            c = db.rawQuery("SELECT * FROM " + DBHelper.TABLE_NAME + " WHERE " +
                    DBHelper.DEPARTURE_STATION_ID + " = " + fromStationId + " AND " +
                    DBHelper.ARRIVAL_STATION_ID + " = " + toStationId + " AND " +
                    DBHelper.DAY + " = " + day, new String[0]);
            int ciDep = c.getColumnIndex(DBHelper.DEPARTURE_TIME);
            int ciArr = c.getColumnIndex(DBHelper.ARRIVAL_TIME);
            int ciDSN = c.getColumnIndex(DBHelper.DEPARTURE_STATION_NAME);
            int ciASN = c.getColumnIndex(DBHelper.ARRIVAL_STATION_NAME);
            int ciTRI = c.getColumnIndex(DBHelper.TRAIN_ROUTE_ID);
            int ciTI = c.getColumnIndex(DBHelper.TRAIN_NAME);
            int ciRSN = c.getColumnIndex(DBHelper.ROUTE_START_STATION_NAME);
            int ciREN = c.getColumnIndex(DBHelper.ROUTE_END_STATION_NAME);
            Log.d("RZDDB", String.valueOf(c.getPosition()));
            Log.d("RZDDB", String.valueOf(c.getCount()));
            for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
                Log.d("RZDDB", String.valueOf(c.getPosition()));
                Log.d("RZDDB", String.valueOf(c.getLong(c.getColumnIndex(BaseColumns._ID))));
                Calendar depTime = new GregorianCalendar();
                Calendar arrTime = new GregorianCalendar();
                depTime.setTimeInMillis(c.getLong(ciDep));
                arrTime.setTimeInMillis(c.getLong(ciArr));
                timetableEntryList.add(new TimetableEntry(
                        fromStationId,
                        c.getString(ciDSN),
                        depTime,
                        toStationId,
                        c.getString(ciASN),
                        arrTime,
                        c.getString(ciTRI),
                        (version == DataSchemeVersion.V2 ? c.getString(ciTI) : null),
                        c.getString(ciRSN),
                        c.getString(ciREN)));
            }
            db.setTransactionSuccessful();
        } catch (Throwable e) {
            Log.e("RZDDB", e.getMessage());
            e.printStackTrace();
        } finally {
            if (c != null) c.close();
            db.endTransaction();
        }
        if (timetableEntryList.size() == 0)
            throw new FileNotFoundException("No data in timetable cache for: fromStationId="
                    + fromStationId + ", toStationId=" + toStationId
                    + ", dateMsk=" + LOG_DATE_FORMAT.format(dateMsk.getTime()));
        return timetableEntryList;
=======
        // TODO: ДЗ - реализовать кэш на основе базы данных SQLite с учетом версии модели данных
        throw new FileNotFoundException("No data in timetable cache for: fromStationId="
                + fromStationId + ", toStationId=" + toStationId
                + ", dateMsk=" + LOG_DATE_FORMAT.format(dateMsk.getTime()));
>>>>>>> parent of f47511d... rdy
    }

    @WorkerThread
    public void put(@NonNull String fromStationId,
                    @NonNull String toStationId,
                    @NonNull Calendar dateMsk,
                    @NonNull List<TimetableEntry> timetable) {
        // TODO: ДЗ - реализовать кэш на основе базы данных SQLite с учетом версии модели данных
    }
}
