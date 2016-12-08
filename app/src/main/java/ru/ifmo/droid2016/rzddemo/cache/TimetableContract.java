package ru.ifmo.droid2016.rzddemo.cache;

import android.support.annotation.IntDef;
import android.util.Log;
import android.util.Pair;

/**
 * Created by garik on 08.12.16.
 */

public class TimetableContract {
    interface TimetableColumns {
        String TRAIN_ID = "trainId";
        String FROM_STATION_ID = "fromStationId";
        String TO_STATION_ID = "toStationId";
        String TIME = "Time";
        String DEPARTURE_STATION_ID = "DepartureStationId";
        String DEPARTURE_STATION_NAME = "departureStationName";
        String DEPARTURE_TIME = "departureTime";
        String ARRIVAL_STATION_ID = "arrivalStationId";
        String ARRIVAL_STATION_NAME = "arrivalStationName";
        String ARRIVAL_TIME = "arrivalTime";
        String TRAIN_ROUTE_ID = "trainRouteId";
        String TRAIN_NAME = "trainName";
        String ROUTE_START_STATION_NAME = "routeStartStationName";
        String ROUTE_END_STATION_NAME = "routeEndStationName";
    }

    interface TypeColumns {
        String TEXT = "TEXT";
        String ID = "INTEGER PRIMARY KEY AUTOINCREMENT";
        String INTEGER = "INTEGER";
    }

    public static final String[] COLUMNS_VERSION_1 = new String[] {
            TimetableColumns.TRAIN_ID,
            TimetableColumns.FROM_STATION_ID, TimetableColumns.TO_STATION_ID,
            TimetableColumns.TIME,
            TimetableColumns.DEPARTURE_STATION_ID, TimetableColumns.DEPARTURE_STATION_NAME,
            TimetableColumns.DEPARTURE_TIME,
            TimetableColumns.ARRIVAL_STATION_ID, TimetableColumns.ARRIVAL_STATION_NAME,
            TimetableColumns.ARRIVAL_TIME,
            TimetableColumns.TRAIN_ROUTE_ID,
            TimetableColumns.ROUTE_START_STATION_NAME, TimetableColumns.ROUTE_END_STATION_NAME
    };

    public static final String[] COLUMNS_VERSION_2 = new String[] {
            TimetableColumns.TRAIN_ID,
            TimetableColumns.FROM_STATION_ID, TimetableColumns.TO_STATION_ID,
            TimetableColumns.TIME,
            TimetableColumns.DEPARTURE_STATION_ID, TimetableColumns.DEPARTURE_STATION_NAME,
            TimetableColumns.DEPARTURE_TIME,
            TimetableColumns.ARRIVAL_STATION_ID, TimetableColumns.ARRIVAL_STATION_NAME,
            TimetableColumns.ARRIVAL_TIME,
            TimetableColumns.TRAIN_ROUTE_ID, TimetableColumns.TRAIN_NAME,
            TimetableColumns.ROUTE_START_STATION_NAME, TimetableColumns.ROUTE_END_STATION_NAME
    };

    public static final String[] TYPES_COLUMNS_1 = new String[] {
            TypeColumns.ID,
            TypeColumns.TEXT, TypeColumns.TEXT,
            TypeColumns.TEXT,
            TypeColumns.TEXT, TypeColumns.TEXT,
            TypeColumns.INTEGER,
            TypeColumns.TEXT, TypeColumns.TEXT,
            TypeColumns.INTEGER,
            TypeColumns.TEXT,
            TypeColumns.TEXT, TypeColumns.TEXT
    };

    public static final String[] TYPES_COLUMNS_2 = new String[] {
            TypeColumns.ID,
            TypeColumns.TEXT, TypeColumns.TEXT,
            TypeColumns.TEXT,
            TypeColumns.TEXT, TypeColumns.TEXT,
            TypeColumns.INTEGER,
            TypeColumns.TEXT, TypeColumns.TEXT,
            TypeColumns.INTEGER,
            TypeColumns.TEXT, TypeColumns.TEXT,
            TypeColumns.TEXT, TypeColumns.TEXT
    };

    interface TimetableCommands {
        String ALTER_TABLE = "ALTER TABLE ";
        String ADD_COLUMN = " ADD COLUMN ";
        String DROP_TABLE = "DROP TABLE ";
        String RENAME_FRAZE = " RENAME TO ";
        String INSERT = "INSERT INTO ";
        String SELECT = " SELECT ";
        String FROM = " FROM ";
    }

    interface TimetableConst {
        String CREATE_COMMAND_BEGIN = "CREATE TABLE ";
        String DATABASE_NAME = "Timetable.db";
        String TABLE_NAME = "timeTable";
        String TEMPORARY_TABLE_NAME = "tempTimeTable";
        String TRAIN_ID = "train_id";
        String INSERT_INTO = "INSERT INTO ";
        String LEFT_PARENTHESES = "(";
        String RIGHT_PARENTHESES = ")";
        String JUST_SPACE = " ";
        String COMMAND_ENDING = ";";
        String VALUES_WITH_SCOPES = RIGHT_PARENTHESES + " VALUES " + LEFT_PARENTHESES;
        String FOR_ID = "INTEGER PRIMARY KEY AUTOINCREMENT";
        String CREATE_PUT_COMMAND_END = RIGHT_PARENTHESES + COMMAND_ENDING;
        String DEVIDER = ", ";
        String EQUAL = "=?";
        String QUESTION = "?";
        String TEXT = "TEXT";
        String INTEGER = "INTEGER";
        String AND = "AND";
    }

    public static String createTableCommand(boolean temp, @DataSchemeVersion int version) {
        StringBuilder result = new StringBuilder();
        result.append(TimetableConst.CREATE_COMMAND_BEGIN);
        result.append(temp ? TimetableConst.TEMPORARY_TABLE_NAME : TimetableConst.TABLE_NAME);
        result.append(" (");
        Pair<String[], String[]> columns = version == DataSchemeVersion.V1 ?
                new Pair<>(COLUMNS_VERSION_1, TYPES_COLUMNS_1) :
                new Pair<>(COLUMNS_VERSION_2, TYPES_COLUMNS_2);
        for (int i = 0, l = columns.first.length; i < l; i++) {
            result.append(columns.first[i]).append(" ").append(columns.second[i]).append(",");
        }
        result.deleteCharAt(result.length() - 1);
        result.append(");");
        Log.d("MyLog", result.toString());
        return result.toString();
    }

    public static String insertToTemp() {
        StringBuilder result = new StringBuilder();
        result.append(TimetableCommands.INSERT).
                append(TimetableConst.TEMPORARY_TABLE_NAME).append(" (");

        for (String s: COLUMNS_VERSION_1) {
            result.append(s).append(",");
        }
        result.deleteCharAt(result.length() - 1).append(")");

        result.append(TimetableCommands.SELECT);
        for (String s: COLUMNS_VERSION_1) {
            result.append(s).append(",");
        }
        result.deleteCharAt(result.length() - 1);

        result.append(TimetableCommands.FROM).append(TimetableConst.TABLE_NAME).append(";");
        Log.d("MyLog", result.toString());
        return result.toString();
    }

    public static String insertToTable(@DataSchemeVersion int version) {
        StringBuilder result = new StringBuilder();
        result.append(TimetableConst.INSERT_INTO).
                append(TimetableConst.TABLE_NAME).append(" (");

        String[] now = (version == DataSchemeVersion.V1) ? COLUMNS_VERSION_1 : COLUMNS_VERSION_2;
        for (String s: now) {
            result.append(s).append(",");
        }
        result.deleteCharAt(result.length() - 1);

        result.append(TimetableConst.VALUES_WITH_SCOPES);
        for (String ignored : now) {
            result.append("?").append(",");
        }
        result.deleteCharAt(result.length() - 1);

        result.append(");");
        Log.d("MyLog", result.toString());
        return result.toString();
    }

    public static String getColumnInsert() {
        return TimetableCommands.ALTER_TABLE +
                TimetableConst.TABLE_NAME +
                TimetableCommands.ADD_COLUMN +
                TimetableColumns.TRAIN_NAME + " " +
                TimetableConst.TEXT + ";";
    }

    public static String deleteTable() {
        return TimetableCommands.DROP_TABLE +
                TimetableConst.TABLE_NAME + ";";
    }

    public static String renameTable() {
        return TimetableCommands.ALTER_TABLE +
                TimetableConst.TEMPORARY_TABLE_NAME +
                TimetableCommands.RENAME_FRAZE +
                TimetableConst.TABLE_NAME + ";";
    }

    public static String getPrefs() {
        return TimetableColumns.FROM_STATION_ID + TimetableConst.EQUAL + " AND " +
                TimetableColumns.TO_STATION_ID + TimetableConst.EQUAL + " AND " +
                TimetableColumns.TIME + TimetableConst.EQUAL;
    }
}
