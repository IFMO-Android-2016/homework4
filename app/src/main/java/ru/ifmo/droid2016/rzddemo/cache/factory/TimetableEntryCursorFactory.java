package ru.ifmo.droid2016.rzddemo.cache.factory;

import android.database.Cursor;
import android.support.annotation.NonNull;

import ru.ifmo.droid2016.rzddemo.model.TimetableEntry;

public interface TimetableEntryCursorFactory {
    /**
     * Конструктор TimetableEntry из данных запроса к базе данных
     * @param cursor Cursor, из данных которого нужно сделать TimetableEntry
     * @return Созданный TimetableEntry
     */
    @NonNull
    TimetableEntry createEntryFromCurrentRow(@NonNull Cursor cursor);
}
