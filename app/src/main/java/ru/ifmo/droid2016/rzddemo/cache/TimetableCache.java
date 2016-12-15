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
import java.text.DateFormat;
import java.text.Format;
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
 *
 * Ключом является комбинация трех значений:
 * ID станции отправления, ID станции прибытия, дата в москомском часовом поясе
 *
 * Единицей хранения является список поездов - {@link TimetableEntry}.
 */

public class TimetableCache {

    private static final String TAG = TimetableCache.class.getSimpleName();

    private static final String RZD_TIMETABLE_DATABASE = "RZD_TIMETABLE_DATABASE";
    private static final String RZD_TIMETABLE_DATABASE_TABLE_NAME = "RZD_TIMETABLE_DATABASE_TABLE_NAME";

    private static final Format DATE_FORMAT = new SimpleDateFormat("yyyyMMdd", Locale.US);
    private static final DateFormat DATE_TIME_FORMAT = new SimpleDateFormat("yyyyMMdd HH:mm:ss", Locale.US);

    private static final TimetableEntry.Key[] ALL_COLUMNS = TimetableEntry.Key.values();

    /**
     * Версия модели данных, с которой работает кэщ.
     */
    @DataSchemeVersion
    private final int version;

    private final SQLiteOpenHelper timetableDatabaseHelper;

    /**
     * Создает экземпляр кэша с указанной версией модели данных.
     *
     * Может вызываться на лююбом (в том числе UI потоке). Может быть создано несколько инстансов
     * {@link TimetableCache} -- все они должны потокобезопасно работать с одним физическим кэшом.
     */
    @AnyThread
    public TimetableCache(@NonNull Context context,
                          @DataSchemeVersion int version) {
        this.version = version;
        timetableDatabaseHelper = TimetableDatabaseHelper.getInstance(context.getApplicationContext(), version);
    }

    private static Calendar getDateTime(String dateTime) {
        Calendar calendar = null;

        try {
            calendar = Calendar.getInstance(TimeUtils.getMskTimeZone());
            calendar.setTime(DATE_TIME_FORMAT.parse(dateTime));
        } catch (ParseException e) {
            Log.wtf(TAG, "Error occurred while parsing datetime", e);
        }

        return calendar;
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

        SQLiteDatabase timetableDatabase = timetableDatabaseHelper.getReadableDatabase();

        Cursor timetableCursor = timetableDatabase.query(
                RZD_TIMETABLE_DATABASE_TABLE_NAME,
                TimetableDatabaseHelper.getColumnsByVersion(version),
                TimetableDatabaseHelper.getSelection(),
                TimetableDatabaseHelper.getSelectionArgs(fromStationId, toStationId, dateMsk),
                null,
                null,
                null);

        List<TimetableEntry> entries = new ArrayList<>();

        if (timetableCursor != null) {
            try {
                if (timetableCursor.moveToFirst()) {
                    do {
                        int columnIndex = 1; // First column is for date
                        entries.add(new TimetableEntry(
                                timetableCursor.getString(columnIndex++),
                                timetableCursor.getString(columnIndex++),
                                getDateTime(timetableCursor.getString(columnIndex++)),
                                timetableCursor.getString(columnIndex++),
                                timetableCursor.getString(columnIndex++),
                                getDateTime(timetableCursor.getString(columnIndex++)),
                                timetableCursor.getString(columnIndex++),
                                version == DataSchemeVersion.V1 ? null : timetableCursor.getString(columnIndex++),
                                timetableCursor.getString(columnIndex++),
                                timetableCursor.getString(columnIndex)));
                    } while (timetableCursor.moveToNext());
                }
            } finally {
                timetableCursor.close();
            }
        }

        timetableDatabase.close();

        if (!entries.isEmpty()) {
            return entries;
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

        SQLiteDatabase timetableDatabase = timetableDatabaseHelper.getWritableDatabase();
        StringBuilder statementBuilder = new StringBuilder("INSERT INTO " + RZD_TIMETABLE_DATABASE_TABLE_NAME + " (");
        String[] columns = TimetableDatabaseHelper.getColumnsByVersion(version);

        for (int i = 0; i < columns.length; i++) {
            statementBuilder.append(columns[i]);

            if (i != columns.length - 1) {
                statementBuilder.append(", ");
            }
        }

        statementBuilder.append(") VALUES (");

        for (int i = 0; i < columns.length - 1; i++) {
            statementBuilder.append("?, ");
        }

        statementBuilder.append("?)");

        SQLiteStatement timetableStatement = timetableDatabase.compileStatement(statementBuilder.toString());
        timetableDatabase.beginTransaction();

        try {
            for (TimetableEntry entry : timetable) {
                int columnIndex = 1;
                timetableStatement.bindString(columnIndex++, DATE_FORMAT.format(dateMsk.getTime()));
                timetableStatement.bindString(columnIndex++, entry.departureStationId);
                timetableStatement.bindString(columnIndex++, entry.departureStationName);
                timetableStatement.bindString(columnIndex++, DATE_TIME_FORMAT.format(entry.departureTime.getTime()));
                timetableStatement.bindString(columnIndex++, entry.arrivalStationId);
                timetableStatement.bindString(columnIndex++, entry.arrivalStationName);
                timetableStatement.bindString(columnIndex++, DATE_TIME_FORMAT.format(entry.arrivalTime.getTime()));
                timetableStatement.bindString(columnIndex++, entry.trainRouteId);

                if (version == DataSchemeVersion.V2) {
                    if (entry.trainName == null) {
                        timetableStatement.bindNull(columnIndex++);
                    } else {
                        timetableStatement.bindString(columnIndex++, entry.trainName);
                    }
                }

                timetableStatement.bindString(columnIndex++, entry.routeStartStationName);
                timetableStatement.bindString(columnIndex, entry.routeEndStationName);
                timetableStatement.executeInsert();
            }

            timetableDatabase.setTransactionSuccessful();
        } finally {
            timetableStatement.close();
            timetableDatabase.endTransaction();
            timetableDatabase.close();
        }
    }

    private static class TimetableDatabaseHelper extends SQLiteOpenHelper {

        private static final String USABLE_DATE = "USABLE_DATE";

        private static final String[] COLUMNS_V1;
        private static final String[] COLUMNS_V2;

        private static volatile TimetableDatabaseHelper instance;

        @DataSchemeVersion
        private final int version;

        private TimetableDatabaseHelper(Context context, @DataSchemeVersion int version) {
            super(context, RZD_TIMETABLE_DATABASE, null, version);
            this.version = version;
        }

        public static TimetableDatabaseHelper getInstance(Context context, @DataSchemeVersion int version) {
            if (instance == null) {
                synchronized (TimetableDatabaseHelper.class) {
                    return instance = new TimetableDatabaseHelper(context, version);
                }
            }

            return instance;
        }

        private static void createTable(SQLiteDatabase database, String tableName, @DataSchemeVersion int version) {
            StringBuilder statementBuilder = new StringBuilder("CREATE TABLE ").append(tableName).append(" (");
            String[] columns = getColumnsByVersion(version);

            for (int i = 0; i < columns.length; i++) {
                statementBuilder.append(columns[i]).append(" TEXT");

                if (i != columns.length - 1) {
                    statementBuilder.append(", ");
                } else {
                    statementBuilder.append(')');
                }
            }

            database.execSQL(statementBuilder.toString());
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            createTable(db, RZD_TIMETABLE_DATABASE_TABLE_NAME, version);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            String statement = "ALTER TABLE " + RZD_TIMETABLE_DATABASE_TABLE_NAME +
                    " ADD COLUMN " +
                    TimetableEntry.Key.TRAIN_NAME.name() +
                    " TEXT";

            db.execSQL(statement);
        }

        @Override
        public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            String tmpTableName = RZD_TIMETABLE_DATABASE_TABLE_NAME + "_TMP";
            createTable(db, tmpTableName, DataSchemeVersion.V1);
            String[] newColumns = getColumnsByVersion(DataSchemeVersion.V1);

            StringBuilder statementBuilder = new StringBuilder("INSERT INTO ").append(tmpTableName);
            statementBuilder.append(" SELECT ");

            for (int i = 0; i < newColumns.length; i++) {
                statementBuilder.append(newColumns[i]);

                if (i != newColumns.length - 1) {
                    statementBuilder.append(", ");
                }
            }

            statementBuilder.append(" FROM ").append(RZD_TIMETABLE_DATABASE_TABLE_NAME);
            db.execSQL(statementBuilder.toString());

            String statement = "DROP TABLE " + RZD_TIMETABLE_DATABASE_TABLE_NAME;
            db.execSQL(statement);

            statement = "ALTER TABLE " + tmpTableName +
                    " RENAME TO " + RZD_TIMETABLE_DATABASE_TABLE_NAME;
            db.execSQL(statement);
        }

        static {
            COLUMNS_V1 = new String[ALL_COLUMNS.length];
            COLUMNS_V2 = new String[ALL_COLUMNS.length + 1];
            COLUMNS_V1[0] = COLUMNS_V2[0] = USABLE_DATE;

            for (int i = 1, j = 1; i <= ALL_COLUMNS.length; i++) {
                if (ALL_COLUMNS[i - 1] == TimetableEntry.Key.TRAIN_NAME) {
                    COLUMNS_V2[i] = ALL_COLUMNS[i - 1].name();
                } else {
                    COLUMNS_V1[j++] = COLUMNS_V2[i] = ALL_COLUMNS[i - 1].name();
                }
            }
        }

        private static String[] getColumnsByVersion(@DataSchemeVersion int version) {
            return version == DataSchemeVersion.V1 ? COLUMNS_V1 : COLUMNS_V2;
        }

        private static String getSelection() {
            return TimetableEntry.Key.DEPARTURE_STATION_ID.name() +
                    " = ? AND " +
                    TimetableEntry.Key.ARRIVAL_STATION_ID.name() +
                    " = ? AND " +
                    USABLE_DATE + " = ?";
        }

        private static String[] getSelectionArgs(String fromId, String toId, Calendar date) {
            return new String[]{
                    fromId,
                    toId,
                    TimetableCache.DATE_FORMAT.format(date.getTime())
            };
        }
    }
}
