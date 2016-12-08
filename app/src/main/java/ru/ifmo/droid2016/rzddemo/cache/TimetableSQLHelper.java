package ru.ifmo.droid2016.rzddemo.cache;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import ru.ifmo.droid2016.rzddemo.model.TimetableEntry;
import ru.ifmo.droid2016.rzddemo.utils.TimeUtils;

public class TimetableSQLHelper extends SQLiteOpenHelper {

    private static final String TAG = TimetableSQLHelper.class.getSimpleName();

    private static TimetableSQLHelper instance;

    public static TimetableSQLHelper getInstance(Context context, @DataSchemeVersion int version) {
        if (instance == null) {
            synchronized (TimetableSQLHelper.class) {
                if (instance == null) {
                    instance = new TimetableSQLHelper(context.getApplicationContext(), version);
                }
            }
        }
        return instance;
    }

    @DataSchemeVersion
    private final int version;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.CANADA);
    private SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.CANADA);

    private TimetableSQLHelper(Context context, @DataSchemeVersion int version) {
        super(context, TimetableContract.DATABASE_NAME, null, version);
        this.version = version;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        final String CREATE_TABLE = "CREATE TABLE " + TimetableContract.Timetable.TABLE_NAME + " (" +
                TimetableContract.Timetable._ID + " INTEGER PRIMARY KEY, " +
                TimetableContract.Timetable.KEY_DATE + " TEXT, " +
                TimetableContract.Timetable.KEY_DEPARTURE_STATION_ID + " TEXT, " +
                TimetableContract.Timetable.KEY_DEPARTURE_STATION_NAME + " TEXT, " +
                TimetableContract.Timetable.KEY_DEPARTURE_TIME + " TEXT, " +
                TimetableContract.Timetable.KEY_ARRIVAL_STATION_ID + " TEXT, " +
                TimetableContract.Timetable.KEY_ARRIVAL_STATION_NAME + " TEXT, " +
                TimetableContract.Timetable.KEY_ARRIVAL_TIME + " TEXT, " +
                TimetableContract.Timetable.KEY_TRAIN_ROUTE_ID + " TEXT, " +
                (version == DataSchemeVersion.V2
                        ? TimetableContract.Timetable.KEY_TRAIN_NAME + " TEXT, "
                        : "") +
                TimetableContract.Timetable.KEY_ROUTE_START_STATION_NAME + " TEXT, " +
                TimetableContract.Timetable.KEY_ROUTE_END_STATION_NAME + " TEXT)";

        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("ALTER TABLE " + TimetableContract.Timetable.TABLE_NAME +
                " ADD COLUMN " + TimetableContract.Timetable.KEY_TRAIN_NAME + " TEXT NULL");
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        final String DOWNGRADE_TABLE_NAME = TimetableContract.Timetable.TABLE_NAME + "_downgrade";

        db.execSQL("ALTER TABLE " + TimetableContract.Timetable.TABLE_NAME +
                " RENAME TO " + DOWNGRADE_TABLE_NAME);

        onCreate(db);

        String columns = TextUtils.join(",", version == DataSchemeVersion.V2
                ? TimetableContract.Timetable.COLUMNS_V2
                : TimetableContract.Timetable.COLUMNS_V1);

        db.execSQL("INSERT INTO " + TimetableContract.Timetable.TABLE_NAME + " (" + columns + ") " +
                "SELECT " + columns + " " + "FROM " + DOWNGRADE_TABLE_NAME);
        db.execSQL("DROP TABLE " + DOWNGRADE_TABLE_NAME);
    }


    private Calendar parseCalendar(String iso) throws ParseException {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeZone(TimeUtils.getMskTimeZone());
        calendar.setTime(dateTimeFormat.parse(iso));
        return calendar;
    }

    public List<TimetableEntry> getTimetable(String fromStationId, String toStationId, Calendar dateMsk) {

        SQLiteDatabase db = getReadableDatabase();
        List<TimetableEntry> result = new ArrayList<>();
        Cursor cursor = null;

        try {
            cursor = db.query(
                    TimetableContract.Timetable.TABLE_NAME,
                    version == DataSchemeVersion.V2
                            ? TimetableContract.Timetable.COLUMNS_V2
                            : TimetableContract.Timetable.COLUMNS_V1,
                    TimetableContract.Timetable.KEY_DATE + "=? AND " +
                            TimetableContract.Timetable.KEY_DEPARTURE_STATION_ID + "=? AND " +
                            TimetableContract.Timetable.KEY_ARRIVAL_STATION_ID + "=?",

                    new String[]{dateFormat.format(dateMsk.getTime()), fromStationId, toStationId},
                    null,
                    null,
                    null
            );

            if (cursor != null && cursor.moveToFirst()) {
                for (; !cursor.isAfterLast(); cursor.moveToNext()) {
                    TimetableEntry entry;
                    try {
                        int i = 1; // Skip date
                        entry = new TimetableEntry(
                                cursor.getString(i++),
                                cursor.getString(i++),
                                parseCalendar(cursor.getString(i++)),
                                cursor.getString(i++),
                                cursor.getString(i++),
                                parseCalendar(cursor.getString(i++)),
                                cursor.getString(i++),
                                ((version == DataSchemeVersion.V2) ? cursor.getString(i++) : null),
                                cursor.getString(i++),
                                cursor.getString(i)
                        );
                        result.add(entry);
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
            }
        } finally {
            if (cursor != null) {
                try {
                    cursor.close();
                } catch (Exception ignored) {
                }
            }
        }

        return result;
    }

    public void putTimetable(Calendar dateMsk, List<TimetableEntry> timetable) {
        final String INSERT_QUERY = "INSERT INTO " + TimetableContract.Timetable.TABLE_NAME + "(" +
                TextUtils.join(", ", version == DataSchemeVersion.V2
                        ? TimetableContract.Timetable.COLUMNS_V2
                        : TimetableContract.Timetable.COLUMNS_V1) + ") " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?" +
                (version == DataSchemeVersion.V2 ? ", ?" : "") + ")";

        SQLiteDatabase db = getWritableDatabase();

        try {
            SQLiteStatement prepared = db.compileStatement(INSERT_QUERY);
            db.beginTransaction();

            for (TimetableEntry record : timetable) {
                int key = 1;
                prepared.bindString(key++, dateFormat.format(dateMsk.getTime()));
                prepared.bindString(key++, record.departureStationId);
                prepared.bindString(key++, record.departureStationName);
                prepared.bindString(key++, dateTimeFormat.format(record.departureTime.getTime()));
                prepared.bindString(key++, record.arrivalStationId);
                prepared.bindString(key++, record.arrivalStationName);
                prepared.bindString(key++, dateTimeFormat.format(record.arrivalTime.getTime()));
                prepared.bindString(key++, record.trainRouteId);
                if (version == DataSchemeVersion.V2) {
                    if (record.trainName == null) {
                        prepared.bindNull(key++);
                    } else {
                        prepared.bindString(key++, record.trainName);
                    }
                }
                prepared.bindString(key++, record.routeStartStationName);
                prepared.bindString(key, record.routeEndStationName);
                prepared.executeInsert();
                prepared.clearBindings();
            }

            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.d(TAG, "Error while trying to add record to database");
        } finally {
            db.endTransaction();
        }
    }

    private static final class TimetableContract {

        static final String DATABASE_NAME = "rzddemo.db";

        static abstract class Timetable implements BaseColumns {
            // Table Name
            private static final String TABLE_NAME = "timetable";

            // Table Columns
            private static final String KEY_DATE = "date";
            private static final String KEY_DEPARTURE_STATION_ID = "departure_station_id";
            private static final String KEY_DEPARTURE_STATION_NAME = "departure_station_name";
            private static final String KEY_DEPARTURE_TIME = "departure_time";
            private static final String KEY_ARRIVAL_STATION_ID = "arrival_station_id";
            private static final String KEY_ARRIVAL_STATION_NAME = "arrival_station_name";
            private static final String KEY_ARRIVAL_TIME = "arrival_time";
            private static final String KEY_TRAIN_ROUTE_ID = "train_route_id";
            private static final String KEY_TRAIN_NAME = "train_name";
            private static final String KEY_ROUTE_START_STATION_NAME = "route_start_station_name";
            private static final String KEY_ROUTE_END_STATION_NAME = "route_end_station_name";

            private static final String[] COLUMNS_V1 = {
                    Timetable.KEY_DATE,
                    Timetable.KEY_DEPARTURE_STATION_ID,
                    Timetable.KEY_DEPARTURE_STATION_NAME,
                    Timetable.KEY_DEPARTURE_TIME,
                    Timetable.KEY_ARRIVAL_STATION_ID,
                    Timetable.KEY_ARRIVAL_STATION_NAME,
                    Timetable.KEY_ARRIVAL_TIME,
                    Timetable.KEY_TRAIN_ROUTE_ID,
                    Timetable.KEY_ROUTE_START_STATION_NAME,
                    Timetable.KEY_ROUTE_END_STATION_NAME
            };

            private static final String[] COLUMNS_V2 = {
                    Timetable.KEY_DATE,
                    Timetable.KEY_DEPARTURE_STATION_ID,
                    Timetable.KEY_DEPARTURE_STATION_NAME,
                    Timetable.KEY_DEPARTURE_TIME,
                    Timetable.KEY_ARRIVAL_STATION_ID,
                    Timetable.KEY_ARRIVAL_STATION_NAME,
                    Timetable.KEY_ARRIVAL_TIME,
                    Timetable.KEY_TRAIN_ROUTE_ID,
                    Timetable.KEY_TRAIN_NAME,
                    Timetable.KEY_ROUTE_START_STATION_NAME,
                    Timetable.KEY_ROUTE_END_STATION_NAME
            };
        }

        private TimetableContract() {
        }
    }
}