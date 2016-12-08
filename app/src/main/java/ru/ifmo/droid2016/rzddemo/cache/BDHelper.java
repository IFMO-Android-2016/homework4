package ru.ifmo.droid2016.rzddemo.cache;

import android.content.Context;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.facebook.stetho.inspector.database.DatabaseConnectionProvider;

import ru.ifmo.droid2016.rzddemo.model.TimetableEntry;

/**
 * Created by appolinariya on 08.12.16.
 */

public class BDHelper extends SQLiteOpenHelper {

    public final String TABLE_NAME;
    public final int DB_VERSION;

    public final String DEPARTURES_ID = "departuresID";
    public final String DEPARTURES_NAME = "departuresName";
    public final String DEPARTURES_TIME = "departuresTime";

    public final String ARRIVAL_ID = "arrivalID";
    public final String ARRIVAL_NAME = "arrivalName";
    public final String ARRIVAL_TIME = "arrivalTime";

    public final String TRAIN_ID = "trainID";
    public final String TRAIN_NAME = "trainName";
    public final String START_NAME = "startName";
    public final String FINISH_NAME = "finishName";

    private static volatile BDHelper instance;

    private BDHelper(Context context, String name, int version) {
        super(context, name, null, version);
        TABLE_NAME = name;
        DB_VERSION = version;
    }

    public static BDHelper getInstance(Context context, String name, int version) {
        if (instance == null) {
            synchronized (BDHelper.class) {
                if (instance == null) {
                    instance = new BDHelper(context, name, version);
                }
            }
        }
        return instance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createDB = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME
                + " (" + DEPARTURES_ID + " TEXT, "
                + DEPARTURES_NAME + " TEXT, "
                + DEPARTURES_TIME + " TEXT, "
                + ARRIVAL_ID + " TEXT, "
                + ARRIVAL_NAME + " TEXT, "
                + ARRIVAL_TIME + " TEXT, "
                + TRAIN_ID + " TEXT, "
                + START_NAME + " TEXT, "
                + FINISH_NAME + " TEXT";
        if (DB_VERSION == 2) {
            createDB = createDB + ", " + TRAIN_NAME + " TEXT);";
        } else {
            createDB = createDB + ");";
        }
        db.execSQL(createDB);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        assert (oldVersion == 2 && newVersion == 1);
        String createDB = "CREATE TABLE " + "fictitious "
                + "(" + DEPARTURES_ID + " TEXT, "
                + DEPARTURES_NAME + " TEXT, "
                + DEPARTURES_TIME + " TEXT, "
                + ARRIVAL_ID + " TEXT, "
                + ARRIVAL_NAME + " TEXT, "
                + ARRIVAL_TIME + " TEXT, "
                + TRAIN_ID + " TEXT, "
                + START_NAME + " TEXT, "
                + FINISH_NAME + " TEXT);";
        db.execSQL(createDB);

        String adding = "INSERT INTO fictitious "
                + "(" + DEPARTURES_ID + ", "
                + DEPARTURES_ID + ", "
                + DEPARTURES_NAME + ", "
                + DEPARTURES_TIME + ", "
                + ARRIVAL_ID + ", "
                + ARRIVAL_NAME + ", "
                + ARRIVAL_TIME + ", "
                + TRAIN_ID + ", "
                + START_NAME + ", "
                + FINISH_NAME + ") "
                + "FROM " + TABLE_NAME;
        db.execSQL(adding);

        db.execSQL("DROP TABLE " + TABLE_NAME);
        db.execSQL("ALTER TABLE fictitious RENAME TO " + TABLE_NAME);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        assert (oldVersion == 1 && newVersion == 2);
        db.execSQL("ALTER " + TABLE_NAME + " ADD " + TABLE_NAME + " TEXT");
    }

}
