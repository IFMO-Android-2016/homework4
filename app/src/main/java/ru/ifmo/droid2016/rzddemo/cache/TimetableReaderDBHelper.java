package ru.ifmo.droid2016.rzddemo.cache;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;


/**
 * @author d1v1nation (catanaut@gmail.com)
 *         <p>
 *         07.12.16 of sql | ru.ifmo.droid2016.rzddemo.cache
 */

public class TimetableReaderDBHelper extends SQLiteOpenHelper {

    private static final String TAG = "TRDBH";

    public static class Scheme implements BaseColumns {
        private Scheme() {};

        public static final String DB_NAME = "Timetable.db";

        public static final String TABLE_NAME = "timetable";
        // ID : 1
        public static final String COLUMN_NAME_DEP_ST_ID = "departureStationId"; // 2
        public static final String COLUMN_NAME_DEP_ST_NAME = "departureStationName"; // 3
        public static final String COLUMN_NAME_DEP_TIME = "departureTime"; // 4
        public static final String COLUMN_NAME_ARR_ST_ID = "arrivalStationId"; // 5
        public static final String COLUMN_NAME_ARR_ST_NAME = "arrivalStationName"; // 6
        public static final String COLUMN_NAME_ARR_ST_TIME = "arrivalTime"; // 7
        public static final String COLUMN_NAME_TR_ROUTE_ID = "trainRouteId"; // 8
        public static final String COLUMN_NAME_R_START = "routeStartStationName"; // 9
        public static final String COLUMN_NAME_R_END = "routeEndStationName"; // 10
        public static final String COLUMN_NAME_DATE_ST = "staticDate"; // 11
        public static final String COLUMN_NAME_FSTID = "fromStationId"; // 12
        public static final String COLUMN_NAME_TSTID = "toStationId"; // 13
        public static final String COLUMN_NAME_TR_NAME = "trainName"; // 14

        public static final String[] TT_V1 = {COLUMN_NAME_DEP_ST_ID, COLUMN_NAME_DEP_ST_NAME, COLUMN_NAME_DEP_TIME,
            COLUMN_NAME_ARR_ST_ID, COLUMN_NAME_ARR_ST_NAME, COLUMN_NAME_ARR_ST_TIME, COLUMN_NAME_TR_ROUTE_ID,
                COLUMN_NAME_R_START, COLUMN_NAME_R_END, COLUMN_NAME_DATE_ST, COLUMN_NAME_FSTID, COLUMN_NAME_TSTID};

        public static final String[] TT_V2 = {COLUMN_NAME_DEP_ST_ID, COLUMN_NAME_DEP_ST_NAME, COLUMN_NAME_DEP_TIME,
                COLUMN_NAME_ARR_ST_ID, COLUMN_NAME_ARR_ST_NAME, COLUMN_NAME_ARR_ST_TIME, COLUMN_NAME_TR_ROUTE_ID,
                COLUMN_NAME_R_START, COLUMN_NAME_R_END, COLUMN_NAME_DATE_ST, COLUMN_NAME_FSTID, COLUMN_NAME_TSTID, COLUMN_NAME_TR_NAME};

        public static String select(int ver) {
            String[] s = (ver == 1) ? TT_V1 : TT_V2;

            // i wish i could Stream::collect

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < s.length; i++) {
                sb.append(s[i]);
                if (i != s.length - 1)
                    sb.append(", ");
            }
            return sb.toString();
        }
    }

    private static final String TEXT_TYPE = " TEXT";
    private static final String INT_TYPE = " INT";
    private static final String COMMA_SEP = ",";
    private static final String CREATE_V1 = "CREATE TABLE " + Scheme.TABLE_NAME + " ( " +
            Scheme._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
            Scheme.COLUMN_NAME_DEP_ST_ID + TEXT_TYPE + COMMA_SEP +
            Scheme.COLUMN_NAME_DEP_ST_NAME + TEXT_TYPE + COMMA_SEP +
            Scheme.COLUMN_NAME_DEP_TIME + INT_TYPE + COMMA_SEP +
            Scheme.COLUMN_NAME_ARR_ST_ID + TEXT_TYPE + COMMA_SEP +
            Scheme.COLUMN_NAME_ARR_ST_NAME + TEXT_TYPE + COMMA_SEP +
            Scheme.COLUMN_NAME_ARR_ST_TIME + INT_TYPE + COMMA_SEP +
            Scheme.COLUMN_NAME_TR_ROUTE_ID + TEXT_TYPE + COMMA_SEP +
            Scheme.COLUMN_NAME_R_START + TEXT_TYPE + COMMA_SEP +
            Scheme.COLUMN_NAME_R_END + TEXT_TYPE + COMMA_SEP +
            Scheme.COLUMN_NAME_DATE_ST + TEXT_TYPE + COMMA_SEP +
            Scheme.COLUMN_NAME_FSTID + TEXT_TYPE + COMMA_SEP +
            Scheme.COLUMN_NAME_TSTID + TEXT_TYPE +
            " ) ";

    private static final String CREATE_V2 = "CREATE TABLE " + Scheme.TABLE_NAME + " ( " +
            Scheme._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
            Scheme.COLUMN_NAME_DEP_ST_ID + TEXT_TYPE + COMMA_SEP +
            Scheme.COLUMN_NAME_DEP_ST_NAME + TEXT_TYPE + COMMA_SEP +
            Scheme.COLUMN_NAME_DEP_TIME + TEXT_TYPE + COMMA_SEP +
            Scheme.COLUMN_NAME_ARR_ST_ID + TEXT_TYPE + COMMA_SEP +
            Scheme.COLUMN_NAME_ARR_ST_NAME + TEXT_TYPE + COMMA_SEP +
            Scheme.COLUMN_NAME_ARR_ST_TIME + TEXT_TYPE + COMMA_SEP +
            Scheme.COLUMN_NAME_TR_ROUTE_ID + TEXT_TYPE + COMMA_SEP +
            Scheme.COLUMN_NAME_R_START + TEXT_TYPE + COMMA_SEP +
            Scheme.COLUMN_NAME_R_END + TEXT_TYPE + COMMA_SEP +
            Scheme.COLUMN_NAME_DATE_ST + TEXT_TYPE + COMMA_SEP +
            Scheme.COLUMN_NAME_FSTID + TEXT_TYPE + COMMA_SEP +
            Scheme.COLUMN_NAME_TSTID + TEXT_TYPE + COMMA_SEP +
            Scheme.COLUMN_NAME_TR_NAME + TEXT_TYPE +
            " ) ";

    private static final String DELETE = "DROP TABLE IF EXISTS " + Scheme.TABLE_NAME;

    private static final String ONE_TO_TWO_UPGRADE = "ALTER TABLE " + Scheme.TABLE_NAME +
            " ADD COLUMN " + Scheme.COLUMN_NAME_TR_NAME + TEXT_TYPE;

    private int ver;
    private Context ctx;
    private static TimetableReaderDBHelper instance = null;
    private TimetableReaderDBHelper(Context ctx, int ver) {
        super(ctx, Scheme.DB_NAME, null, ver);
        this.ver = ver;
        this.ctx = ctx;
    }

    public static TimetableReaderDBHelper getInstance(Context ctx, int ver) {
        if (instance == null) {
            synchronized (TimetableReaderDBHelper.class) {
                if (instance == null) {
                    instance = new TimetableReaderDBHelper(ctx, ver);
                }
            }
        }

        return instance;
    }


    @Override
    public void onCreate(SQLiteDatabase db) {
        final String q = ver == 1 ? CREATE_V1 : CREATE_V2;
        Log.d(TAG, "onCreate: " + q);
        db.beginTransaction();
        try {
            db.execSQL(q);

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        final String q = ONE_TO_TWO_UPGRADE;
        Log.d(TAG, "onUpgrade: " + q);

        ver = newVersion;

        db.beginTransaction();
        try {
            try {
                db.execSQL(q);
            } catch (SQLiteException e) {
                if (e.getMessage().contains("duplicate")) {
                    // purely cool
                } else {
                    throw e;
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // two to one conversion
        ver = oldVersion;

        db.beginTransaction();
        try {
            final String al = "ALTER TABLE " + Scheme.TABLE_NAME + " RENAME TO " +
                    (Scheme.TABLE_NAME + "_OLD");
            Log.d(TAG, "onDowngrade: " + al);
            db.execSQL(al);
            onCreate(db);

            String select = Scheme.select(ver);

            String sqlIns = "INSERT INTO " + Scheme.TABLE_NAME + " ( " + select + " ) SELECT " + select + " FROM " + Scheme.TABLE_NAME + "_OLD";
            Log.d(TAG, "onDowngrade: " + sqlIns);
            db.execSQL(sqlIns);

            String sqlDrop = "DROP TABLE " + Scheme.TABLE_NAME + "_OLD";
            Log.d(TAG, "onDowngrade: " + sqlDrop);
            db.execSQL(sqlDrop);

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }
}
