package ru.ifmo.droid2016.rzddemo.cache;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.support.annotation.AnyThread;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;

import java.io.FileNotFoundException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

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

    private TimeTableSQLiteOpenHelper mTimeTableHelper;

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

        mTimeTableHelper = TimeTableSQLiteOpenHelper.getInstance(context, version);
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
        return mTimeTableHelper.get(fromStationId, toStationId, dateMsk);
    }

    @WorkerThread
    public void put(@NonNull String fromStationId,
                    @NonNull String toStationId,
                    @NonNull Calendar dateMsk,
                    @NonNull List<TimetableEntry> timetable) {
        mTimeTableHelper.put(fromStationId, toStationId, dateMsk, timetable);
    }

    private static class TimeTableSQLiteOpenHelper extends SQLiteOpenHelper {

        private static String DATABASE_NAME = "CacheDatabase";
        private static String TABLE_NAME = "TimeTableCache";

        private static String ID_COLUMN = "id";
        private static String REQUEST_TIME_COLUMN = "requestTime";
        private static String DEPARTURE_STATION_ID_COLUMN = "departureStationId";
        private static String DEPARTURE_STATION_NAME_COLUMN = "departureStationName";
        private static String DEPARTURE_TIME_COLUMN = "departureTime";
        private static String ARRIVAL_STATION_ID_COLUMN = "arrivalStationId";
        private static String ARRIVAL_STATION_NAME_COLUMN = "arrivalStationName";
        private static String ARRIVAL_TIME_COLUMN = "arrivalTime";
        private static String TRAIN_NAME_COLUMN = "trainName";
        private static String TRAIN_ROUTE_ID_COLUMN = "trainRouteId";
        private static String ROUTE_START_STATION_NAME_COLUMN = "routeStartStationName";
        private static String ROUTE_END_STATION_NAME_COLUMN = "routeEndStationName";

        private static String DROP_TABLE_QUERY = "DROP TABLE IF EXISTS " + TABLE_NAME + ";";

        private static String CREATE_TABLE_QUERY_V1 = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (\n" +
                "  " + ID_COLUMN + " integer primary key, \n" +
                "  " + REQUEST_TIME_COLUMN + " date, \n" +
                "  " + DEPARTURE_STATION_ID_COLUMN + " text,\n" +
                "  " + DEPARTURE_STATION_NAME_COLUMN + " text,\n" +
                "  " + DEPARTURE_TIME_COLUMN + " date,\n" +
                "  " + ARRIVAL_STATION_ID_COLUMN + " text,\n" +
                "  " + ARRIVAL_STATION_NAME_COLUMN + " text,\n" +
                "  " + ARRIVAL_TIME_COLUMN + " text,\n" +
                "  " + TRAIN_ROUTE_ID_COLUMN + " text,\n" +
                "  " + ROUTE_START_STATION_NAME_COLUMN + " text,\n" +
                "  " + ROUTE_END_STATION_NAME_COLUMN + " text\n" +
                ");";

        private static String CREATE_TABLE_QUERY_V2 = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (\n" +
                "  " + ID_COLUMN + " integer primary key, \n" +
                "  " + REQUEST_TIME_COLUMN + " date, \n" +
                "  " + DEPARTURE_STATION_ID_COLUMN + " text,\n" +
                "  " + DEPARTURE_STATION_NAME_COLUMN + " text,\n" +
                "  " + DEPARTURE_TIME_COLUMN + " date,\n" +
                "  " + ARRIVAL_STATION_ID_COLUMN + " text,\n" +
                "  " + ARRIVAL_STATION_NAME_COLUMN + " text,\n" +
                "  " + ARRIVAL_TIME_COLUMN + " text,\n" +
                "  " + TRAIN_ROUTE_ID_COLUMN + " text,\n" +
                "  " + TRAIN_NAME_COLUMN + " text,\n" +
                "  " + ROUTE_START_STATION_NAME_COLUMN + " text,\n" +
                "  " + ROUTE_END_STATION_NAME_COLUMN + " text\n" +
                ");";

        private static String INSERT_VALUES_QUERY_V1 = "INSERT INTO " + TABLE_NAME + " (" +
                REQUEST_TIME_COLUMN + ", " +
                DEPARTURE_STATION_ID_COLUMN + ", " +
                DEPARTURE_STATION_NAME_COLUMN + ", " +
                DEPARTURE_TIME_COLUMN + ", " +
                ARRIVAL_STATION_ID_COLUMN + ", " +
                ARRIVAL_STATION_NAME_COLUMN + ", " +
                ARRIVAL_TIME_COLUMN + ", " +
                TRAIN_ROUTE_ID_COLUMN + ", " +
                ROUTE_START_STATION_NAME_COLUMN + ", " +
                ROUTE_END_STATION_NAME_COLUMN + ") " +
                " VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";

        private static String INSERT_VALUES_QUERY_V2 = "INSERT INTO " + TABLE_NAME + " (" +
                REQUEST_TIME_COLUMN + ", " +
                DEPARTURE_STATION_ID_COLUMN + ", " +
                DEPARTURE_STATION_NAME_COLUMN + ", " +
                DEPARTURE_TIME_COLUMN + ", " +
                ARRIVAL_STATION_ID_COLUMN + ", " +
                ARRIVAL_STATION_NAME_COLUMN + ", " +
                ARRIVAL_TIME_COLUMN + ", " +
                TRAIN_ROUTE_ID_COLUMN + ", " +
                TRAIN_NAME_COLUMN + ", " +
                ROUTE_START_STATION_NAME_COLUMN + ", " +
                ROUTE_END_STATION_NAME_COLUMN + ") " +
                " VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";

        private final int mVersion;

        private final DateFormat requestDateFormat;

        private static volatile TimeTableSQLiteOpenHelper instance;

        public static TimeTableSQLiteOpenHelper getInstance(Context context, int version) {
            TimeTableSQLiteOpenHelper localInstance = instance;
            if (localInstance == null) {
                synchronized (TimeTableSQLiteOpenHelper.class) {
                    localInstance = instance;
                    if (localInstance == null) {
                        instance = localInstance =  new TimeTableSQLiteOpenHelper(context, TimeTableSQLiteOpenHelper.DATABASE_NAME, null, version);
                    }
                }
            }
            return localInstance;
        }

        private TimeTableSQLiteOpenHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
            super(context, name, factory, version);
            this.mVersion = version;

            if (mVersion == DataSchemeVersion.V1) {
                selectionColumns = new String[]{
                        DEPARTURE_STATION_ID_COLUMN,
                        DEPARTURE_STATION_NAME_COLUMN,
                        DEPARTURE_TIME_COLUMN,
                        ARRIVAL_STATION_ID_COLUMN,
                        ARRIVAL_STATION_NAME_COLUMN,
                        ARRIVAL_TIME_COLUMN,
                        TRAIN_ROUTE_ID_COLUMN,
                        ROUTE_START_STATION_NAME_COLUMN,
                        ROUTE_END_STATION_NAME_COLUMN
                };
            } else {
                selectionColumns = new String[]{
                        DEPARTURE_STATION_ID_COLUMN,
                        DEPARTURE_STATION_NAME_COLUMN,
                        DEPARTURE_TIME_COLUMN,
                        ARRIVAL_STATION_ID_COLUMN,
                        ARRIVAL_STATION_NAME_COLUMN,
                        ARRIVAL_TIME_COLUMN,
                        TRAIN_ROUTE_ID_COLUMN,
                        TRAIN_NAME_COLUMN,
                        ROUTE_START_STATION_NAME_COLUMN,
                        ROUTE_END_STATION_NAME_COLUMN
                };
            }

            requestDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        }

        @Override
        public void onCreate(SQLiteDatabase sqLiteDatabase) {
            if (mVersion == DataSchemeVersion.V1) {
                sqLiteDatabase.execSQL(CREATE_TABLE_QUERY_V1);
            } else {
                sqLiteDatabase.execSQL(CREATE_TABLE_QUERY_V2);
            }
        }

        @Override
        public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
            if (oldVersion == DataSchemeVersion.V1 && newVersion == DataSchemeVersion.V2) {
                sqLiteDatabase.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN " + TRAIN_NAME_COLUMN + ";");
            }
        }

        @Override
        public void onDowngrade(SQLiteDatabase database, int oldVersion, int newVersion) {
            if (oldVersion == DataSchemeVersion.V2 && newVersion == DataSchemeVersion.V1) {
                String tempTable = "temporaryTable";
                database.execSQL("ALTER TABLE " + TABLE_NAME + " RENAME TO " + tempTable);
                database.execSQL(CREATE_TABLE_QUERY_V1);
                String columns =
                        REQUEST_TIME_COLUMN + ", " +
                                DEPARTURE_STATION_ID_COLUMN + ", " +
                                DEPARTURE_STATION_NAME_COLUMN + ", " +
                                DEPARTURE_TIME_COLUMN + ", " +
                                ARRIVAL_STATION_ID_COLUMN + ", " +
                                ARRIVAL_STATION_NAME_COLUMN + ", " +
                                ARRIVAL_TIME_COLUMN + ", " +
                                TRAIN_ROUTE_ID_COLUMN + ", " +
                                ROUTE_START_STATION_NAME_COLUMN + ", " +
                                ROUTE_END_STATION_NAME_COLUMN;
                database.execSQL("INSERT INTO " + TABLE_NAME + " (" + columns + ") SELECT " + columns + " FROM " + tempTable);
                database.execSQL("DROP TABLE " + tempTable);
            }
        }

        private final String[] selectionColumns;
        private String selection = DEPARTURE_STATION_ID_COLUMN + "=? AND " + ARRIVAL_STATION_ID_COLUMN + "=? AND " + REQUEST_TIME_COLUMN + "=?";

        @NonNull
        List<TimetableEntry> get(@NonNull String fromStationId,
                                 @NonNull String toStationId,
                                 @NonNull Calendar dateMsk)
                throws FileNotFoundException {
            SQLiteDatabase database = getReadableDatabase();


            String[] selectionArgs = {fromStationId, toStationId, requestDateFormat.format(dateMsk.getTime())};

            List<TimetableEntry> result = new ArrayList<>();

            Cursor cursor = null;
            try {
                cursor = database.query(TABLE_NAME, selectionColumns, selection, selectionArgs, null, null, null);
                if (cursor == null) {
                    throw new FileNotFoundException("No data in timetable cache for: fromStationId="
                            + fromStationId + ", toStationId=" + toStationId
                            + ", dateMsk=" + LOG_DATE_FORMAT.format(dateMsk.getTime()));
                }
                while (cursor.moveToNext()) {
                    if (mVersion == DataSchemeVersion.V1) {
                        result.add(new TimetableEntry(
                                cursor.getString(0),
                                cursor.getString(1),
                                convertUnixTimeToCalendar(cursor.getLong(2)),
                                cursor.getString(3),
                                cursor.getString(4),
                                convertUnixTimeToCalendar(cursor.getLong(5)),
                                cursor.getString(6),
                                null,
                                cursor.getString(7),
                                cursor.getString(8)
                        ));
                    } else {
                        result.add(new TimetableEntry(
                                cursor.getString(0),
                                cursor.getString(1),
                                convertUnixTimeToCalendar(cursor.getLong(2)),
                                cursor.getString(3),
                                cursor.getString(4),
                                convertUnixTimeToCalendar(cursor.getLong(5)),
                                cursor.getString(6),
                                cursor.getString(7),
                                cursor.getString(8),
                                cursor.getString(9)
                        ));
                    }
                }
            } finally {
                if (cursor != null)
                    cursor.close();
            }
            if (result.size() == 0) {
                throw new FileNotFoundException("No data in timetable cache for: fromStationId="
                        + fromStationId + ", toStationId=" + toStationId
                        + ", dateMsk=" + LOG_DATE_FORMAT.format(dateMsk.getTime()));
            }
            return result;
        }

        void put(@NonNull String fromStationId,
                 @NonNull String toStationId,
                 @NonNull Calendar dateMsk,
                 @NonNull List<TimetableEntry> timetable) {
            SQLiteDatabase database = getWritableDatabase();
            SQLiteStatement statement = null;
            if (mVersion == DataSchemeVersion.V1) {
                statement = database.compileStatement(INSERT_VALUES_QUERY_V1);
            } else {
                statement = database.compileStatement(INSERT_VALUES_QUERY_V2);
            }
            database.beginTransaction();
            try {
                for (TimetableEntry entry : timetable) {
                    if (mVersion == DataSchemeVersion.V1) {
                        statement.bindString(1, requestDateFormat.format(dateMsk.getTime()));
                        statement.bindString(2, entry.departureStationId);
                        statement.bindString(3, entry.departureStationName);
                        statement.bindLong(4, entry.departureTime.getTimeInMillis());
                        statement.bindString(5, entry.arrivalStationId);
                        statement.bindString(6, entry.arrivalStationName);
                        statement.bindLong(7, entry.arrivalTime.getTimeInMillis());
                        statement.bindString(8, entry.trainRouteId);
                        statement.bindString(9, entry.routeStartStationName);
                        statement.bindString(10, entry.routeEndStationName);
                    } else {
                        statement.bindString(1, requestDateFormat.format(dateMsk.getTime()));
                        statement.bindString(2, entry.departureStationId);
                        statement.bindString(3, entry.departureStationName);
                        statement.bindLong(4, entry.departureTime.getTimeInMillis());
                        statement.bindString(5, entry.arrivalStationId);
                        statement.bindString(6, entry.arrivalStationName);
                        statement.bindLong(7, entry.arrivalTime.getTimeInMillis());
                        statement.bindString(8, entry.trainRouteId);
                        if (entry.trainName != null)
                            statement.bindString(9, entry.trainName);
                        else
                            statement.bindNull(9);
                        statement.bindString(10, entry.routeStartStationName);
                        statement.bindString(11, entry.routeEndStationName);
                    }
                    statement.execute();
                }
                database.setTransactionSuccessful();
            } finally {
                database.endTransaction();
            }
        }

        private Calendar convertUnixTimeToCalendar(long time) {
            Calendar calendar = GregorianCalendar.getInstance();
            calendar.setTimeInMillis(time);
            return calendar;
        }
    }
}
