package ru.ifmo.droid2016.rzddemo.cache.factory;

import android.database.Cursor;
import android.support.annotation.NonNull;

import java.util.Calendar;

import ru.ifmo.droid2016.rzddemo.model.TimetableEntry;
import ru.ifmo.droid2016.rzddemo.utils.TimeUtils;

public class TimetableEntryFactoryV2 implements TimetableEntryCursorFactory {
    @Override
    @NonNull
    public TimetableEntry createEntryFromCurrentRow(@NonNull Cursor cursor) {
        String departureStationId = cursor.getString(0);
        String departureStationName = cursor.getString(1);
        Calendar departureTime = Calendar.getInstance();
        departureTime.setTimeZone(TimeUtils.getMskTimeZone());
        departureTime.setTimeInMillis(cursor.getLong(2));
        String arrivalStationId = cursor.getString(3);
        String arrivalStationName = cursor.getString(4);
        Calendar arrivalTime = Calendar.getInstance();
        arrivalTime.setTimeZone(TimeUtils.getMskTimeZone());
        arrivalTime.setTimeInMillis(cursor.getLong(5));
        String trainRouteId = cursor.getString(6);
        String routeStartStationName = cursor.getString(7);
        String routeEndStationName = cursor.getString(8);
        String trainName = cursor.getString(9);
        return new TimetableEntry(departureStationId, departureStationName, departureTime,
                arrivalStationId, arrivalStationName, arrivalTime,
                trainRouteId, trainName,routeStartStationName, routeEndStationName);
    }
}
