package ru.ifmo.droid2016.rzddemo.cache;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.io.File;

import ru.ifmo.droid2016.rzddemo.utils.DBHandler;
import solid.collectors.ToJoinedString;
import solid.stream.Stream;

import static ru.ifmo.droid2016.rzddemo.cache.TimetableCacheContract.Timetable.*;

public class TimetableDBHelper extends SQLiteOpenHelper {

    private static final String DB_FILE_NAME = "timetable.db";

    private static int DB_VERS;

    private static volatile TimetableDBHelper inst;

    public static TimetableDBHelper getInstance(Context context, @DataSchemeVersion int vers) {

        if (inst == null) {
            synchronized (TimetableDBHelper.class) {
                if (inst == null) {
                    inst = new TimetableDBHelper(context, vers);
                }
            }
        }
        return inst;
    }

    private Context context;

    public TimetableDBHelper(Context context, @DataSchemeVersion int vers) {
        super(context, DB_FILE_NAME, null /*factory*/, vers,
                new DBHandler(context, DB_FILE_NAME));
        DB_VERS = vers;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String newTable = DB_VERS == DataSchemeVersion.V1 ?
                V1_TABLE : V2_TABLE;
        db.execSQL(newTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("ALTER TABLE " + TABLE +
                " ADD COLUMN " + TRAIN_NAME);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        String tempTableName = TABLE + "Temp";
        db.execSQL("ALTER TABLE " + TABLE + " RENAME TO " + tempTableName);
        db.execSQL(V1_TABLE);
        String allColumns = Stream.of(DEPARTURE_DATE)
                .merge(Stream.stream(V1_COMPONENTS))
                .collect(ToJoinedString.toJoinedString(", "));
        db.execSQL("INSERT INTO " + TABLE + " (" + allColumns + ")" +
                " SELECT " + allColumns +
                " FROM " + tempTableName);
        db.execSQL("DROP TABLE " + tempTableName);
    }


    public void dropDb() {
        SQLiteDatabase db = getWritableDatabase();
        if (db.isOpen()) {
            try {
                db.close();
            } catch (Exception e) {
                Log.w(LOG_TAG, "Failed to close DB");
            }
        }
        final File dbFile = context.getDatabasePath(DB_FILE_NAME);
        try {
            Log.d(LOG_TAG, "deleting the database file: " + dbFile.getPath());
            if (!dbFile.delete()) {
                Log.w(LOG_TAG, "Failed to delete database file: " + dbFile);
            }
            Log.d(LOG_TAG, "Deleted DB file: " + dbFile);
        } catch (Exception e) {
            Log.w(LOG_TAG, "Failed to delete database file: " + dbFile, e);
        }
    }

    private static final String LOG_TAG = "TimetableDBHelper";
}
