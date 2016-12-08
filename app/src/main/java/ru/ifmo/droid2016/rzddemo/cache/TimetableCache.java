package ru.ifmo.droid2016.rzddemo.cache;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.support.annotation.AnyThread;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;
import android.util.Log;

import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import ru.ifmo.droid2016.rzddemo.api.BadResponseException;
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

    @NonNull
    private final Context context;
    private static final String EMPTY = "##Empty";

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

        SQLiteDatabase database = TimetableOpenHelper.getInstance(context, version).getReadableDatabase();
        System.out.println(Integer.toString(version));

        Cursor cursor = null;
        ArrayList <TimetableEntry> result = null;
        try {
            String[] that = (version == DataSchemeVersion.V1) ? TimetableContract.FOR_THE_FIRST_VERSION : TimetableContract.FOR_THE_SECOND_VERSION;
            cursor = database.query(TimetableContract.TABLE_NAME,
                    that,
                    TimetableContract.getPrefs(),
                    new String[] {fromStationId, toStationId, LOG_DATE_FORMAT.format(dateMsk.getTime())},
                    null, null, null
                    );
            Log.d("Prefs", TimetableContract.getPrefs());
            StringBuilder build = new StringBuilder();
            for (int i = 0; i < that.length; i++) {
                build.append(that[i]);
                build.append(' ');
            }
            Log.d("Everything", build.toString());
            result = new ArrayList<>();
            if (cursor.moveToFirst()) {
                for (; !cursor.isAfterLast(); cursor.moveToNext()) {
                    GregorianCalendar firstDate = new GregorianCalendar();
                    firstDate.setTimeInMillis(cursor.getLong(6));
                    GregorianCalendar secondDate = new GregorianCalendar();
                    secondDate.setTimeInMillis(cursor.getLong(9));
                    if (version == DataSchemeVersion.V1) {
                        result.add(new TimetableEntry(
                                cursor.getString(4),
                                cursor.getString(5),
                                firstDate,
                                cursor.getString(7),
                                cursor.getString(8),
                                secondDate,
                                cursor.getString(10),
                                null,
                                cursor.getString(11),
                                cursor.getString(12)));
                    } else {
                        result.add(new TimetableEntry(
                                cursor.getString(4),
                                cursor.getString(5),
                                firstDate,
                                cursor.getString(7),
                                cursor.getString(8),
                                secondDate,
                                cursor.getString(10),
                                (cursor.getString(11) == null || cursor.getString(11).equals(EMPTY)) ? null : cursor.getString(11),
                                cursor.getString(12),
                                cursor.getString(13)));
                    }
                }
            } else {
                System.out.println("Badovo");
            }
        } finally {
            if (cursor == null) {
                try {
                    cursor.close();
                } catch (NullPointerException ex) {
                    Log.d("Bad response", ex.getMessage());
                }
            }
        }

        // TODO: ДЗ - реализовать кэш на основе базы данных SQLite с учетом версии модели данных

        if (result.size() == 0) {
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

        SQLiteDatabase database = TimetableOpenHelper.getInstance(context, version).getWritableDatabase();
        database.beginTransaction();
        SQLiteStatement query = null;

        try {
            query = database.compileStatement(TimetableContract.transformToInsertQuery(version));
            for (TimetableEntry entry: timetable) {
                int nxt = 2;
                query.bindString(nxt++, fromStationId);
                query.bindString(nxt++, toStationId);
                query.bindString(nxt++, LOG_DATE_FORMAT.format(dateMsk.getTime()));
                query.bindString(nxt++, entry.departureStationId);
                query.bindString(nxt++, entry.departureStationName);
                query.bindLong(nxt++, entry.departureTime.getTimeInMillis());
                query.bindString(nxt++, entry.arrivalStationId);
                query.bindString(nxt++, entry.arrivalStationName);
                query.bindLong(nxt++, entry.arrivalTime.getTimeInMillis());
                query.bindString(nxt++, entry.trainRouteId);
                if (version == DataSchemeVersion.V2) {
                    //Log.d("trainNameValue", entry.trainName);
                    if (entry.trainName != null) {
                        query.bindString(nxt++, entry.trainName);
                    } else {
                        query.bindString(nxt++, "##Empty");
                    }
                }
                query.bindString(nxt++, entry.routeStartStationName);
                query.bindString(nxt++, entry.routeEndStationName);
                query.executeInsert();
            }
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
            if (query != null) {
                try {
                    query.close();
                } catch (NullPointerException ex) {
                    Log.d("Bad response", ex.getMessage());
                }
            }

        }

        // TODO: ДЗ - реализовать кэш на основе базы данных SQLite с учетом версии модели данных
    }


}
