package ru.ifmo.droid2016.rzddemo.cache;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.Settings;
import android.util.Log;

/**
 * Created by nikita on 07.12.16.
 */

public class TimetableOpenHelper extends SQLiteOpenHelper {

	private final int version;

	public TimetableOpenHelper(Context context, int version) {
		super(context, TimetableContract.DATABASE_NAME, null, version);
		this.version = version;
		//System.out.println(version + 100);
	}

	private static volatile TimetableOpenHelper instance;

	public static TimetableOpenHelper getInstance(Context context, int version) {
		if (instance == null) {
			synchronized (TimetableOpenHelper.class) {
				if (instance == null) {
					//System.out.println(version);
					instance = new TimetableOpenHelper(context, version);
				}
			}
		}
		return instance;
	}

	@Override
	public void onCreate(SQLiteDatabase database) {
		Log.d("Database Version", Integer.toString(database.getVersion()));
		String str = TimetableContract.getTableCreationCommand(false, version);
		Log.d("Not good", str);
		database.execSQL(str);
	}

	@Override
	public void onUpgrade(SQLiteDatabase database, int i, int i1) {
		database.execSQL(TimetableContract.getColumnInsert());
	}

	@Override
	public void onDowngrade(SQLiteDatabase database, int oldVersion, int newVersion) {
		//super.onDowngrade(database, oldVersion, newVersion);

		database.execSQL(TimetableContract.getTableCreationCommand(true, DataSchemeVersion.V2));
		database.execSQL(TimetableContract.insertion());
		database.execSQL(TimetableContract.deleteTable());
		database.execSQL(TimetableContract.renameTable());
	}
}
