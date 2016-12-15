package ru.ifmo.droid2016.rzddemo.cache;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.provider.BaseColumns;
import android.text.TextUtils;


import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import ru.ifmo.droid2016.rzddemo.model.TimetableEntry;
import ru.ifmo.droid2016.rzddemo.utils.TimeUtils;


public class DataBaseHelper extends SQLiteOpenHelper {

    private static DataBaseHelper instance;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd", Locale.US);
    private SimpleDateFormat timeFormat = new SimpleDateFormat("yyyy.MM.dd G 'at' HH:mm:ss z", Locale.US);

    @DataSchemeVersion
    private final int version;

    private DataBaseHelper(Context context, @DataSchemeVersion int version) {
        super(context, CacheContract.DATABASE, null, version);
        this.version = version;
    }


    private static final class CacheContract {

        static final String DATABASE = "rzddemo.db";

        static abstract class Timetable implements BaseColumns {

            private static final String TABLE_NAME = "timetable";
            private static final String DATE = "date";

            private static final String DEPARTURE_STATION_ID = "departure_station_id";
            private static final String DEPARTURE_STATION_NAME = "departure_station_name";
            private static final String DEPARTURE_TIME = "departure_time";

            private static final String ARRIVAL_STATION_ID = "arrival_station_id";
            private static final String ARRIVAL_STATION_NAME = "arrival_station_name";
            private static final String ARRIVAL_TIME = "arrival_time";

            private static final String TRAIN_ROUTE_ID = "train_route_id";
            private static final String ROUTE_START_STATION_NAME = "route_start_station_name";
            private static final String ROUTE_END_STATION_NAME = "route_end_station_name";
            private static final String TRAIN_NAME = "train_name";

            private static final String[] COLL_V1 = {
                    Timetable.DATE,
                    Timetable.DEPARTURE_STATION_ID,
                    Timetable.DEPARTURE_STATION_NAME,
                    Timetable.DEPARTURE_TIME,
                    Timetable.ARRIVAL_STATION_ID,
                    Timetable.ARRIVAL_STATION_NAME,
                    Timetable.ARRIVAL_TIME,
                    Timetable.TRAIN_ROUTE_ID,
                    Timetable.ROUTE_START_STATION_NAME,
                    Timetable.ROUTE_END_STATION_NAME
            };

            private static final String[] COLL_V2 = {
                    Timetable.DATE,
                    Timetable.DEPARTURE_STATION_ID,
                    Timetable.DEPARTURE_STATION_NAME,
                    Timetable.DEPARTURE_TIME,
                    Timetable.ARRIVAL_STATION_ID,
                    Timetable.ARRIVAL_STATION_NAME,
                    Timetable.ARRIVAL_TIME,
                    Timetable.TRAIN_ROUTE_ID,
                    Timetable.TRAIN_NAME,
                    Timetable.ROUTE_START_STATION_NAME,
                    Timetable.ROUTE_END_STATION_NAME
            };
        }

        private CacheContract() {
        }
    }


    private Calendar parseCalendar(String date) throws ParseException {

        Calendar currentCalendar = Calendar.getInstance();
        currentCalendar.setTimeZone(TimeUtils.getMskTimeZone());
        currentCalendar.setTime(timeFormat.parse(date));
        return currentCalendar;
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        db.execSQL("ALTER TABLE " + CacheContract.Timetable.TABLE_NAME
                + " ADD COLUMN " + CacheContract.Timetable.TRAIN_NAME + " TEXT NULL");
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        final String CREATE_TABLE = "CREATE TABLE " + CacheContract.Timetable.TABLE_NAME + " (" +
                TextUtils.join(" TEXT, ", version == DataSchemeVersion.V2 ? CacheContract.Timetable.COLL_V2
                        : CacheContract.Timetable.COLL_V1)
                + " TEXT)";
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
   

        final String DOWNGRADE_TABLE_NAME = CacheContract.Timetable.TABLE_NAME + "_downgrade";

        db.execSQL("ALTER TABLE " + CacheContract.Timetable.TABLE_NAME + " RENAME TO " + DOWNGRADE_TABLE_NAME);
        onCreate(db);
        final String columns = TextUtils.join(",", version == DataSchemeVersion.V2 ? CacheContract.Timetable.COLL_V2
                : CacheContract.Timetable.COLL_V1);
        db.execSQL("INSERT INTO " + CacheContract.Timetable.TABLE_NAME + " (" + columns + ") "
                + "SELECT " + columns + " " + "FROM " + DOWNGRADE_TABLE_NAME);
        db.execSQL("DROP TABLE " + DOWNGRADE_TABLE_NAME);
    }




    public void putTimetable(Calendar dateMsk, List<TimetableEntry> timetable) {
  
        final String INSERT_QUERY = "INSERT INTO " + CacheContract.Timetable.TABLE_NAME + "(" +
                TextUtils.join(", ", version == DataSchemeVersion.V2 ? CacheContract.Timetable.COLL_V2
                        : CacheContract.Timetable.COLL_V1)
                + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?"
                + (version == DataSchemeVersion.V2 ? ", ?" : "") + ")";

        SQLiteDatabase db = getWritableDatabase();

        try {
            SQLiteStatement prepared = db.compileStatement(INSERT_QUERY);
            db.beginTransaction();

            for (TimetableEntry entry : timetable) {
                int i = 1;
                prepared.bindString(i++, dateFormat.format(dateMsk.getTime()));
                prepared.bindString(i++, entry.departureStationId);
                prepared.bindString(i++, entry.departureStationName);
                prepared.bindString(i++, timeFormat.format(entry.departureTime.getTime()));
                prepared.bindString(i++, entry.arrivalStationId);
                prepared.bindString(i++, entry.arrivalStationName);
                prepared.bindString(i++, timeFormat.format(entry.arrivalTime.getTime()));
                prepared.bindString(i++, entry.trainRouteId);
                if (version == DataSchemeVersion.V2) {
                    if (entry.trainName == null) {
                        prepared.bindNull(i++);
                    } else {
                        prepared.bindString(i++, entry.trainName);
                    }
                }
                prepared.bindString(i++, entry.routeStartStationName);
                prepared.bindString(i, entry.routeEndStationName);
                prepared.executeInsert();
                prepared.clearBindings();
            }

            db.setTransactionSuccessful();
        } catch (Exception e) {
        
        } finally {
            db.endTransaction();
        }
    }

    public List<TimetableEntry> getTimetable(String fromStationId, String toStationId, Calendar dateMsk) {
    

        SQLiteDatabase db = getReadableDatabase();
        List<TimetableEntry> result = new ArrayList<>();
        Cursor cursor = null;

        try {
            cursor = db.query(
                    CacheContract.Timetable.TABLE_NAME,
                    version == DataSchemeVersion.V2 ? CacheContract.Timetable.COLL_V2
                            : CacheContract.Timetable.COLL_V1,
                    CacheContract.Timetable.DATE + "=? AND " +
                            CacheContract.Timetable.DEPARTURE_STATION_ID + "=? AND " +
                            CacheContract.Timetable.ARRIVAL_STATION_ID + "=?",

                    new String[]{dateFormat.format(dateMsk.getTime()), fromStationId, toStationId},
                    null,
                    null,
                    null
            );

            if (cursor != null && cursor.moveToFirst()) {
                for (; !cursor.isAfterLast(); cursor.moveToNext()) {
                    TimetableEntry entry;
                    try {
                        int i = 1;
                        entry = new TimetableEntry(
                                cursor.getString(i++),
                                cursor.getString(i++),
                                parseCalendar(cursor.getString(i++)),
                                cursor.getString(i++),
                                cursor.getString(i++),
                                parseCalendar(cursor.getString(i++)),
                                cursor.getString(i++),
                                (version == DataSchemeVersion.V2 ? cursor.getString(i++) : null),
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

    public static DataBaseHelper getInstance(Context context, @DataSchemeVersion int version) {
       
        if (instance == null) {
            synchronized (DataBaseHelper.class) {
                if (instance == null) {
                    instance = new DataBaseHelper(context.getApplicationContext(), version);
                }
            }
        }
        return instance;
    }

}