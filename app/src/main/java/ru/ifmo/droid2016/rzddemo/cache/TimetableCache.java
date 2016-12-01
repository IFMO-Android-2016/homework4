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
import java.util.List;

import ru.ifmo.droid2016.rzddemo.cache.factory.TimetableEntryCursorFactory;
import ru.ifmo.droid2016.rzddemo.cache.factory.TimetableEntryFactoryV1;
import ru.ifmo.droid2016.rzddemo.cache.factory.TimetableEntryFactoryV2;
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

    private static final String TAG = "TtCache";

    @NonNull
    private final Context context;

    /**
     * Версия модели данных, с которой работает кэщ.
     */
    @DataSchemeVersion
    private final int version;

    /**
     * Фабрика TimetableEntry из текущего столбца переданного Cursor
     */
    @NonNull
    private final TimetableEntryCursorFactory factory;

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
        switch (version){
            case DataSchemeVersion.V1:
                factory = new TimetableEntryFactoryV1();
                break;
            case DataSchemeVersion.V2:
                factory = new TimetableEntryFactoryV2();
                break;
            default:
                throw new IllegalStateException("No TimetableEntryFactory for version " + Integer.toString(version));
        }
        Log.d(TAG, "Request to initialize " + DatabaseHelper.class.getSimpleName()
                + " with version " + Integer.toString(version));
        DatabaseHelper.initializeInstance(context.getApplicationContext(), version);
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
        SQLiteDatabase db = DatabaseHelper.getInstance().getWritableDatabase();
        long dayStart = dateMsk.getTimeInMillis();
        long dayEnd = TimeUtils.getNextDay(dateMsk).getTimeInMillis();
        String [] selectionArgs = {fromStationId, toStationId, Long.toString(dayStart), Long.toString(dayEnd)};
        String selector = "departureStationId=? AND arrivalStationId=? AND departureTime>=? AND departureTime<?";
        Log.d(TAG,"Selecting from table " + DatabaseHelper.TABLE_NAME);
        Cursor result = db.query(DatabaseHelper.TABLE_NAME,null,selector,selectionArgs,null,null,"departureTime");
        Log.d(TAG,"Selected from table " + DatabaseHelper.TABLE_NAME);
        if(result.getCount() == 0){
            Log.d(TAG,"Cache miss");
            result.close();
            DatabaseHelper.getInstance().close();
            throw new FileNotFoundException("No data in timetable cache for: fromStationId="
                    + fromStationId + ", toStationId=" + toStationId
                    + ", dateMsk=" + LOG_DATE_FORMAT.format(dateMsk.getTime()));
        } else {
            Log.d(TAG,"Cache hit");
            List<TimetableEntry> data = new ArrayList<>();
            while(result.moveToNext()){
                data.add(factory.createEntryFromCurrentRow(result));
            }
            result.close();
            return data;
        }
    }

    @WorkerThread
    public void put(@NonNull String fromStationId,
                    @NonNull String toStationId,
                    @NonNull Calendar dateMsk,
                    @NonNull List<TimetableEntry> timetable) {
        SQLiteDatabase db = DatabaseHelper.getInstance().getWritableDatabase();
        Log.d(TAG,"Writing to table " + DatabaseHelper.TABLE_NAME);
        String insertStatement = "INSERT OR REPLACE INTO " + DatabaseHelper.TABLE_NAME + " VALUES ";
        switch (version){
            case DataSchemeVersion.V1:
                insertStatement += "(?,?,?,?,?,?,?,?,?)";
                break;
            case DataSchemeVersion.V2:
                insertStatement += "(?,?,?,?,?,?,?,?,?,?)";
                break;
            default:
                throw new IllegalStateException("No insert query for version " + Integer.toString(version));
        }
        SQLiteStatement ins = db.compileStatement(insertStatement);
        db.beginTransactionNonExclusive();
        try{
            for (TimetableEntry entry: timetable) {
                ins.bindString(1, entry.departureStationId);
                ins.bindString(2, entry.departureStationName);
                ins.bindLong(3, entry.departureTime.getTimeInMillis());
                ins.bindString(4, entry.arrivalStationId);
                ins.bindString(5, entry.arrivalStationName);
                ins.bindLong(6, entry.arrivalTime.getTimeInMillis());
                ins.bindString(7, entry.trainRouteId);
                ins.bindString(8, entry.routeStartStationName);
                ins.bindString(9, entry.routeEndStationName);
                if(version == DataSchemeVersion.V2){
                    if(entry.trainName == null){ // Да, у поезда может не быть имени
                        ins.bindNull(10);
                    } else {
                        ins.bindString(10, entry.trainName);
                    }
                }
                ins.executeInsert();
            }
            Log.d(TAG,"Writing to table " + DatabaseHelper.TABLE_NAME + " successful");
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }
}
