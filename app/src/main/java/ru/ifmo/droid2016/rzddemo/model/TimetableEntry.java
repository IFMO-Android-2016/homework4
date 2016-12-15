package ru.ifmo.droid2016.rzddemo.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Calendar;

/**
 * Элемент расписания движения поездов.
 */
public class TimetableEntry {

    public enum Key {
        DEPARTURE_STATION_ID,
        DEPARTURE_STATION_NAME,
        DEPARTURE_TIME,
        ARRIVAL_STATION_ID,
        ARRIVAL_STATION_NAME,
        ARRIVAL_TIME,
        TRAIN_ROUTE_ID,
        TRAIN_NAME,
        ROUTE_START_STATION_NAME,
        ROUTE_END_STATION_NAME
    }

    /**
     * ID станции отправления.
     */
    @NonNull
    public final String departureStationId;

    /**
     * Название станции отправления.
     */
    @NonNull
    public final String departureStationName;

    /**
     * Время отправления.
     */
    @NonNull
    public final Calendar departureTime;

    /**
     * ID станции прибытия.
     */
    @NonNull
    public final String arrivalStationId;

    /**
     * Название станции прибытия.
     */
    @NonNull
    public final String arrivalStationName;

    /**
     * Время прибытия.
     */
    @NonNull
    public final Calendar arrivalTime;

    /**
     * Номер поезда.
     */
    @NonNull
    public final String trainRouteId;

    /**
     * Название поезда.
     */
    @Nullable
    public final String trainName;

    /**
     * Название начальной станции поезда.
     */
    @NonNull
    public final String routeStartStationName;

    /**
     * Название конечной станции поезда.
     */
    @NonNull
    public final String routeEndStationName;

    public TimetableEntry(@NonNull String departureStationId,
                          @NonNull String departureStationName,
                          @NonNull Calendar departureTime,
                          @NonNull String arrivalStationId,
                          @NonNull String arrivalStationName,
                          @NonNull Calendar arrivalTime,
                          @NonNull String trainRouteId,
                          @Nullable String trainName,
                          @NonNull String routeStartStationName,
                          @NonNull String routeEndStationName) {
        this.departureStationId = departureStationId;
        this.departureStationName = departureStationName;
        this.departureTime = departureTime;
        this.arrivalStationId = arrivalStationId;
        this.arrivalStationName = arrivalStationName;
        this.arrivalTime = arrivalTime;
        this.trainRouteId = trainRouteId;
        this.trainName = trainName;
        this.routeStartStationName = routeStartStationName;
        this.routeEndStationName = routeEndStationName;
    }
}
