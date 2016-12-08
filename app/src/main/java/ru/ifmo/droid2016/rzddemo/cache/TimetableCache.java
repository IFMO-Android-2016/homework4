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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
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
    private static final String EMPTY = "REAL_EMPTY_TRAIN_NAME";

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

        SQLiteDatabase database = TimetableOpenHelper.getInstance(context, version).getReadableDatabase();

        Cursor cursor = null;
        ArrayList<TimetableEntry> result = null;
        try {
            String[] columns = (version == DataSchemeVersion.V1) ?
                    TimetableContract.COLUMNS_VERSION_1 :
                    TimetableContract.COLUMNS_VERSION_2;

            cursor = database.query(TimetableContract.TimetableConst.TABLE_NAME,
                    columns,
                    TimetableContract.getPrefs(),
                    new String[]{fromStationId, toStationId, LOG_DATE_FORMAT.format(dateMsk.getTime())},
                    null, null, null
            );

            result = new ArrayList<>();
            if (cursor.moveToFirst()) {
                for (; !cursor.isAfterLast(); cursor.moveToNext()) {
                    GregorianCalendar firstDate = new GregorianCalendar();
                    firstDate.setTimeInMillis(cursor.getLong(6));

                    GregorianCalendar secondDate = new GregorianCalendar();
                    secondDate.setTimeInMillis(cursor.getLong(9));

                    int place = cursor.getColumnIndex(
                            TimetableContract.TimetableColumns.TRAIN_NAME
                    );
                    result.add(new TimetableEntry(
                            cursor.getString(cursor.getColumnIndex(
                                    TimetableContract.TimetableColumns.DEPARTURE_STATION_ID)
                            ),
                            cursor.getString(cursor.getColumnIndex(
                                    TimetableContract.TimetableColumns.DEPARTURE_STATION_NAME)
                            ),
                            firstDate,
                            cursor.getString(cursor.getColumnIndex(
                                    TimetableContract.TimetableColumns.ARRIVAL_STATION_ID)
                            ),
                            cursor.getString(cursor.getColumnIndex(
                                    TimetableContract.TimetableColumns.ARRIVAL_STATION_NAME)
                            ),
                            secondDate,
                            cursor.getString(cursor.getColumnIndex(
                                    TimetableContract.TimetableColumns.TRAIN_ROUTE_ID)
                            ),
                            place == -1 ? null : (
                                    cursor.getString(place).equals(EMPTY) ?
                                            null : cursor.getString(place)
                            ),
                            cursor.getString(cursor.getColumnIndex(
                                    TimetableContract.TimetableColumns.ROUTE_START_STATION_NAME)
                            ),
                            cursor.getString(cursor.getColumnIndex(
                                    TimetableContract.TimetableColumns.ROUTE_END_STATION_NAME)
                            )
                    ));
                }
            }
        } finally {
            if (cursor != null) {
                try {
                    cursor.close();
                } catch (NullPointerException ex) {
                    Log.d("Bad response", ex.getMessage());
                }
            }
        }

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
            query = database.compileStatement(TimetableContract.insertToTable(version));

            for (TimetableEntry entry : timetable) {
                int index = 2;
                query.bindString(index++, fromStationId);
                query.bindString(index++, toStationId);
                query.bindString(index++, LOG_DATE_FORMAT.format(dateMsk.getTime()));
                query.bindString(index++, entry.departureStationId);
                query.bindString(index++, entry.departureStationName);
                query.bindLong(index++, entry.departureTime.getTimeInMillis());
                query.bindString(index++, entry.arrivalStationId);
                query.bindString(index++, entry.arrivalStationName);
                query.bindLong(index++, entry.arrivalTime.getTimeInMillis());
                query.bindString(index++, entry.trainRouteId);
                if (version == DataSchemeVersion.V2) {
                    if (entry.trainName != null) {
                        query.bindString(index++, entry.trainName);
                    } else {
                        query.bindString(index++, EMPTY);
                    }
                }
                query.bindString(index++, entry.routeStartStationName);
                query.bindString(index, entry.routeEndStationName);
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
    }
}
