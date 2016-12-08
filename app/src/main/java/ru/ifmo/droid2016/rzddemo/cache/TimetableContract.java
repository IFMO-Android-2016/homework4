package ru.ifmo.droid2016.rzddemo.cache;

import android.util.Log;

import java.util.ArrayList;
import java.util.Calendar;

import ru.ifmo.droid2016.rzddemo.model.TimetableEntry;

import static ru.ifmo.droid2016.rzddemo.Constants.LOG_DATE_FORMAT;

/**
 * Created by nikita on 07.12.16.
 */

public final class TimetableContract {
	public static final String DATABASE_NAME = "Timetable.db";
	public static final String TABLE_NAME = "trains_timetable";
	public static final String TEMPORARY_TABLE_NAME = "tmp_trains";
	public static final String TRAIN_ID = "train_id";
	//public static final String GET_COMMAND_BEGIN = "SELECT * FROM ";
	//public static final String GET_COMMAND_MIDDLE = " WHERE ";
	public static final String PUT_COMMAND_BEGIN = "INSERT INTO ";
	public static final String LEFT_PARENTHESES = "(";
	public static final String RIGHT_PARENTHESES = ")";
	public static final String JUST_SPACE = " ";
	public static final String COMMAND_ENDING = ";";
	public static final String PUT_COMMAND_MIDDLE = RIGHT_PARENTHESES + " VALUES " + LEFT_PARENTHESES;
	public static final String CREATE_COMMAND_BEGIN = "CREATE TABLE ";
	public static final String CREATE_COMMAND_MIDDLE = JUST_SPACE + LEFT_PARENTHESES;
	public static final String FOR_ID = "INTEGER PRIMARY KEY AUTOINCREMENT";
	public static final String CREATE_PUT_COMMAND_END = RIGHT_PARENTHESES + COMMAND_ENDING;
	public static final String DEVIDER = ", ";
	public static final String EQUAL = "=?";
	public static final String QUESTION = "?";
	public static final String TEXT = "TEXT";
	public static final String INTEGER = "INTEGER";
	public static final String AND = "AND";

	public static final String FROM_STATION_ID = "from_station_id";
	public static final String TO_STATION_ID = "to_station_id";
	public static final String MOSCOW_TIME = "moscow_time";
	public static final String DEPARTURE_STATION_ID = "departure_station_id";
	public static final String DEPARTURE_STATION_NAME = "departure_station_name";
	public static final String DEPARTURE_TIME = "departure_time";
	public static final String ARRIVAL_STATION_ID = "arrival_station_id";
	public static final String ARRIVAL_STATION_NAME = "arrival_station_name";
	public static final String ARRIVAL_TIME = "arrival_time";
	public static final String TRAIN_ROUTE_ID = "train_route_id";
	public static final String TRAIN_NAME = "train_name";
	public static final String ROUTE_START_STATION_NAME = "route_start_station_name";
	public static final String ROUTE_END_STATION_NAME = "route_end_station_name";

	public static final String COLUMN_BEGIN = "ALTER TABLE ";
	public static final String COLUMN_MIDDLE = " ADD COLUMN ";
	public static final String DELETING_FRAZE = "DROP TABLE ";
	public static final String RENAME_FRAZE = " RENAME TO ";
	public static final String INSERT_FRAZE = "INSERT INTO ";
	public static final String SELECT_BEGIN = " SELECT ";
	public static final String SELECT_MIDDLE = " FROM ";

	public static final String[] FOR_THE_FIRST_VERSION = {
			TRAIN_ID, FROM_STATION_ID, TO_STATION_ID, MOSCOW_TIME, DEPARTURE_STATION_ID, DEPARTURE_STATION_NAME, DEPARTURE_TIME, ARRIVAL_STATION_ID,
			ARRIVAL_STATION_NAME, ARRIVAL_TIME, TRAIN_ROUTE_ID, ROUTE_START_STATION_NAME,
			ROUTE_END_STATION_NAME
	};

	public static final String[] TYPES_FOR_THE_FIRST_VERSION = {
			FOR_ID, TEXT, TEXT, TEXT, TEXT, TEXT, INTEGER, TEXT, TEXT, INTEGER, TEXT, TEXT, TEXT
	};

	public static final String[] FOR_THE_SECOND_VERSION = {
			TRAIN_ID, FROM_STATION_ID, TO_STATION_ID, MOSCOW_TIME, DEPARTURE_STATION_ID, DEPARTURE_STATION_NAME, DEPARTURE_TIME, ARRIVAL_STATION_ID,
			ARRIVAL_STATION_NAME, ARRIVAL_TIME, TRAIN_ROUTE_ID, TRAIN_NAME, ROUTE_START_STATION_NAME,
			ROUTE_END_STATION_NAME
	};

	public static final String[] TYPES_FOR_THE_SECOND_VERSION = {
			FOR_ID, TEXT, TEXT, TEXT, TEXT, TEXT, INTEGER, TEXT, TEXT, INTEGER, TEXT, TEXT, TEXT, TEXT
	};

	/*public static String getPutCommandBegin(int version) {
		return PUT_COMMAND_BEGIN + getDatabaseName(version) + JUST_SPACE + LEFT_PARENTHESES;
	}*/

	public static String getTableCreationCommand(boolean tmp_table, int version) {
		StringBuilder result = new StringBuilder();
		result.append(CREATE_COMMAND_BEGIN);
		Log.d("Something went wrong", String.valueOf(version));
		result.append(tmp_table ? TEMPORARY_TABLE_NAME : TABLE_NAME);
		result.append(CREATE_COMMAND_MIDDLE);
		String[] now = (version == DataSchemeVersion.V1) ? FOR_THE_FIRST_VERSION : FOR_THE_SECOND_VERSION;
		String[] nowType = (version == DataSchemeVersion.V1) ? TYPES_FOR_THE_FIRST_VERSION : TYPES_FOR_THE_SECOND_VERSION;
		for (int i = 0; i < now.length; i++) {
			result.append(now[i]);
			result.append(JUST_SPACE);
			result.append(nowType[i]);
			if (i != now.length - 1) {
				result.append(DEVIDER);
			}
		}
		result.append(CREATE_PUT_COMMAND_END);
		return result.toString();
	}

	public static String getColumnInsert() {
		return COLUMN_BEGIN + TABLE_NAME + COLUMN_MIDDLE + TRAIN_NAME + JUST_SPACE + TEXT + COMMAND_ENDING;
	}

	public static String deleteTable() {
		return DELETING_FRAZE + TABLE_NAME + COMMAND_ENDING;
	}

	public static String renameTable() {
		return COLUMN_BEGIN + TEMPORARY_TABLE_NAME + RENAME_FRAZE + TABLE_NAME + COMMAND_ENDING;
	}

	/*public static String transformToGetQuery(int version, String fromStationId, String toStationId, Calendar dateMsk) {
		StringBuilder result = new StringBuilder();
		result.append(GET_COMMAND_BEGIN);
		result.append(getDatabaseName(version));
		result.append(GET_COMMAND_MIDDLE);
		result.append(FROM_STATION_ID);
		result.append(EQUAL);
		result.append(DEVIDER);
		result.append(TO_STATION_ID);
		result.append(EQUAL);
		result.append(DEVIDER);
		result.append(MOSCOW_TIME);
		result.append(EQUAL);
		result.append(COMMAND_ENDING);
		return result.toString();
	}*/

	public static String insertion() {
		StringBuilder result = new StringBuilder();
		result.append(INSERT_FRAZE);
		result.append(TEMPORARY_TABLE_NAME);
		result.append(JUST_SPACE);
		result.append(LEFT_PARENTHESES);
		String[] now = FOR_THE_FIRST_VERSION;
		for (int i = 0; i < now.length; i++) {
			result.append(now[i]);
			if (i != now.length - 1) {
				result.append(DEVIDER);
			}
		}
		result.append(RIGHT_PARENTHESES);
		result.append(SELECT_BEGIN);
		for (int i = 0; i < now.length; i++) {
			result.append(now[i]);
			if (i != now.length - 1) {
				result.append(DEVIDER);
			}
		}
		result.append(SELECT_MIDDLE);
		result.append(TABLE_NAME);
		result.append(COMMAND_ENDING);
		return result.toString();
	}

	public static String transformToInsertQuery(int version) {
		StringBuilder result = new StringBuilder();
		result.append(PUT_COMMAND_BEGIN);
		result.append(TABLE_NAME);
		result.append(JUST_SPACE);
		result.append(LEFT_PARENTHESES);
		String[] now = (version == 1) ? FOR_THE_FIRST_VERSION : FOR_THE_SECOND_VERSION;
		for (int i = 0; i < now.length; i++) {
			result.append(now[i]);
			if (i != now.length - 1) {
				result.append(DEVIDER);
			}
		}
		result.append(PUT_COMMAND_MIDDLE);
		for (int i = 0; i < now.length; i++) {
			result.append(QUESTION);
			if (i != now.length - 1) {
				result.append(DEVIDER);
			}
		}
		result.append(CREATE_PUT_COMMAND_END);
		return result.toString();
	}

	public static String getPrefs() {
		return FROM_STATION_ID + EQUAL + JUST_SPACE + AND + JUST_SPACE +
				TO_STATION_ID + EQUAL + JUST_SPACE + AND + JUST_SPACE +
				MOSCOW_TIME + EQUAL;
	}
}
