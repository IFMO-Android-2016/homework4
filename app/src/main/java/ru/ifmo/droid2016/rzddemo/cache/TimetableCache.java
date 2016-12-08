package ru.ifmo.droid2016.rzddemo.cache;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.provider.ContactsContract;
import android.support.annotation.AnyThread;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;

import java.io.FileNotFoundException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import ru.ifmo.droid2016.rzddemo.model.TimetableEntry;
import ru.ifmo.droid2016.rzddemo.utils.TimeUtils;

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


    private final TrainScheduleDatabaseHandler dbHandler;
    private final SimpleDateFormat dateFormat;
    private final SimpleDateFormat dateFormatWithTime;

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

        this.dbHandler = new TrainScheduleDatabaseHandler(this.context, version);
        this.dateFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH);
        this.dateFormatWithTime = new SimpleDateFormat("dd-MM-yyyy'T'HH:mm", Locale.ENGLISH);
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


        // TODO: ДЗ - реализовать кэш на основе базы данных SQLite с учетом версии модели данных

        SQLiteDatabase sqLiteDatabase = dbHandler.getReadableDatabase();

        Cursor cursor = sqLiteDatabase.query(
                TrainScheduleContract.TABLE_NAME,
                ((version == DataSchemeVersion.V2) ?
                        new String[]{
                                TrainScheduleContract.DEPARTURE_STATION_ID,
                                TrainScheduleContract.DEPARTURE_STATION_NAME,
                                TrainScheduleContract.DEPARTURE_TIME,
                                TrainScheduleContract.ARRIVAL_STATION_ID,
                                TrainScheduleContract.ARRIVAL_STATION_NAME,
                                TrainScheduleContract.ARRIVAL_TIME,
                                TrainScheduleContract.TRAIN_ROUTE_ID,
                                TrainScheduleContract.TRAIN_NAME,
                                TrainScheduleContract.ROUTE_START_STATION_NAME,
                                TrainScheduleContract.ROUTE_END_STATION_NAME
                        }
                        :
                        new String[]{
                                TrainScheduleContract.DEPARTURE_STATION_ID,
                                TrainScheduleContract.DEPARTURE_STATION_NAME,
                                TrainScheduleContract.DEPARTURE_TIME,
                                TrainScheduleContract.ARRIVAL_STATION_ID,
                                TrainScheduleContract.ARRIVAL_STATION_NAME,
                                TrainScheduleContract.ARRIVAL_TIME,
                                TrainScheduleContract.TRAIN_ROUTE_ID,
                                TrainScheduleContract.ROUTE_START_STATION_NAME,
                                TrainScheduleContract.ROUTE_END_STATION_NAME
                        }
                ), TrainScheduleContract.DATE + "=? AND "
                        + TrainScheduleContract.DEPARTURE_STATION_ID + "=? AND "
                        + TrainScheduleContract.ARRIVAL_STATION_ID + "=?",
                new String[]{
                        dateFormat.format(dateMsk.getTime()),
                        fromStationId,
                        toStationId
                },
                null,
                null,
                null,
                null);


        try{
            if(cursor != null && cursor.moveToFirst()){
                List<TimetableEntry> schedule = new ArrayList<>();


                do{
                    TimetableEntry timetableEntry;

                    try{
                        int count = 0;

                        timetableEntry = new TimetableEntry(
                                cursor.getString(count++),
                                cursor.getString(count++),
                                parseCalendar(cursor.getString(count++)),
                                cursor.getString(count++),
                                cursor.getString(count++),
                                parseCalendar(cursor.getString(count++)),
                                cursor.getString(count++),
                                ((version == DataSchemeVersion.V2) ? cursor.getString(count++) : null),
                                cursor.getString(count++),
                                cursor.getString(count)
                        );
                    }catch (ParseException e){
                        e.printStackTrace();
                        continue;
                    }

                    schedule.add(timetableEntry);

                }while(cursor.moveToNext());

            return schedule;
            }
        }finally{
            if(cursor != null){
                cursor.close();
            }

            sqLiteDatabase.close();
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
        // TODO: ДЗ - реализовать кэш на основе базы данных SQLite с учетом версии модели данных

        SQLiteDatabase sqLiteDatabase = dbHandler.getWritableDatabase();

        String query = "INSERT INTO " + TrainScheduleContract.TABLE_NAME + " ("
                + TrainScheduleContract.DATE + ","
                + TrainScheduleContract.DEPARTURE_STATION_ID + ","
                + TrainScheduleContract.DEPARTURE_STATION_NAME + ","
                + TrainScheduleContract.DEPARTURE_TIME + ","
                + TrainScheduleContract.ARRIVAL_STATION_ID + ","
                + TrainScheduleContract.ARRIVAL_STATION_NAME + ","
                + TrainScheduleContract.ARRIVAL_TIME + ","
                + TrainScheduleContract.TRAIN_ROUTE_ID + ",";

        if(version == DataSchemeVersion.V2)
            query += TrainScheduleContract.TRAIN_NAME + ",";

        query += TrainScheduleContract.ROUTE_START_STATION_NAME + ","
                + TrainScheduleContract.ROUTE_END_STATION_NAME
                + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?";

        if(version == DataSchemeVersion.V2)
            query += ", ?";


        query += ")";

        SQLiteStatement statement = sqLiteDatabase.compileStatement(query);

        sqLiteDatabase.beginTransaction();

        try{
            for(TimetableEntry timetableEntry : timetable){
                int count = 1;

                statement.bindString(count++, dateFormat.format(dateMsk.getTime()));
                statement.bindString(count++, timetableEntry.departureStationId);
                statement.bindString(count++, timetableEntry.departureStationName);
                statement.bindString(count++, dateFormatWithTime.format(timetableEntry.departureTime.getTime()));
                statement.bindString(count++, timetableEntry.arrivalStationId);
                statement.bindString(count++, timetableEntry.arrivalStationName);
                statement.bindString(count++, dateFormatWithTime.format(timetableEntry.arrivalTime.getTime()));
                statement.bindString(count++, timetableEntry.trainRouteId);

                if (version == DataSchemeVersion.V2) {
                    if (timetableEntry.trainName == null) {
                       statement.bindNull(count++);
                    } else {
                        statement.bindString(count++, timetableEntry.trainName);
                    }
                }

                statement.bindString(count++, timetableEntry.routeStartStationName);
                statement.bindString(count++, timetableEntry.routeEndStationName);

                statement.executeInsert();
            }

            sqLiteDatabase.setTransactionSuccessful();


        }finally {
            sqLiteDatabase.endTransaction();
        }

        sqLiteDatabase.close();
    }

    private Calendar parseCalendar(String s) throws ParseException{
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeZone(TimeUtils.getMskTimeZone());
        calendar.setTime(dateFormatWithTime.parse(s));

        return calendar;
    }

    private static class TrainScheduleDatabaseHandler extends SQLiteOpenHelper {
        private static final String DATABASE_NAME = "rzd.train_schedule.db";

        @DataSchemeVersion
        private int version;

        public TrainScheduleDatabaseHandler(Context context, @DataSchemeVersion int version) {
            super(context, DATABASE_NAME, null, version);
            this.version = version;
        }

        private void createTable(SQLiteDatabase sqLiteDatabase, @DataSchemeVersion int version) {
            String query = "CREATE TABLE " + TrainScheduleContract.TABLE_NAME + " ("
                    + TrainScheduleContract.ID + " INTEGER PRIMARY KEY,"
                    + TrainScheduleContract.DATE + " TEXT,"
                    + TrainScheduleContract.DEPARTURE_STATION_ID + " TEXT,"
                    + TrainScheduleContract.DEPARTURE_STATION_NAME + " TEXT,"
                    + TrainScheduleContract.DEPARTURE_TIME + " TEXT,"
                    + TrainScheduleContract.ARRIVAL_STATION_ID + " TEXT,"
                    + TrainScheduleContract.ARRIVAL_STATION_NAME + " TEXT,"
                    + TrainScheduleContract.ARRIVAL_TIME + " TEXT,"
                    + TrainScheduleContract.TRAIN_ROUTE_ID + " TEXT,";
            if (version == DataSchemeVersion.V2)
                query += TrainScheduleContract.TRAIN_NAME + " TEXT,";

            query += TrainScheduleContract.ROUTE_START_STATION_NAME + " TEXT,"
                    + TrainScheduleContract.ROUTE_END_STATION_NAME + "TEXT)";

            sqLiteDatabase.execSQL(query);

        }

        @Override
        public void onCreate(SQLiteDatabase sqLiteDatabase) {
            createTable(sqLiteDatabase, version);
        }

        @Override
        public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
            sqLiteDatabase.execSQL("ALTER TABLE " + TrainScheduleContract.TABLE_NAME + " "
                    + "ADD COLUMN " + TrainScheduleContract.TRAIN_NAME + "TEXT");
        }

        public void onDowngrade(SQLiteDatabase sqLite, int oldVersion, int newVersion) {
            final String DOWNGRADE_TABLE_NAME = "downgrade_" + TrainScheduleContract.TABLE_NAME;

            sqLite.execSQL("ALTER TABLE " + TrainScheduleContract.TABLE_NAME + " " + "RENAME TO " + DOWNGRADE_TABLE_NAME);

            createTable(sqLite, DataSchemeVersion.V1);

            String columns = TrainScheduleContract.ID + ","
                    + TrainScheduleContract.DATE + ","
                    + TrainScheduleContract.DEPARTURE_STATION_ID + ","
                    + TrainScheduleContract.DEPARTURE_STATION_NAME + ","
                    + TrainScheduleContract.DEPARTURE_TIME + ","
                    + TrainScheduleContract.ARRIVAL_STATION_ID + ","
                    + TrainScheduleContract.ARRIVAL_STATION_NAME + ","
                    + TrainScheduleContract.ARRIVAL_TIME + ","
                    + TrainScheduleContract.TRAIN_ROUTE_ID + ","
                    + TrainScheduleContract.ROUTE_START_STATION_NAME + ","
                    + TrainScheduleContract.ROUTE_END_STATION_NAME;


            sqLite.execSQL("INSERT INTO " + TrainScheduleContract.TABLE_NAME + " (" + columns + ") " +
                    "SELECT " + columns + " " + "FROM " + DOWNGRADE_TABLE_NAME);

            sqLite.execSQL("DROP TABLE " + DOWNGRADE_TABLE_NAME);

        }
    }


    private static final class TrainScheduleContract {


        static final String ID = "id";
        static final String DATE = "date";
        static final String DEPARTURE_STATION_ID = "departure_station_id";
        static final String DEPARTURE_STATION_NAME = "departure_station_name";
        static final String DEPARTURE_TIME = "departure_time";
        static final String ARRIVAL_STATION_ID = "arrival_station_id";
        static final String ARRIVAL_STATION_NAME = "arrival_station_name";
        static final String ARRIVAL_TIME = "arrival_time";
        static final String TRAIN_ROUTE_ID = "train_route_id";
        static final String TRAIN_NAME = "train_name";
        static final String ROUTE_START_STATION_NAME = "route_start_station_name";
        static final String ROUTE_END_STATION_NAME = "route_end_station_name";


        static final String TABLE_NAME = "timetable";
    }
}
