package com.demo.transit.application.model;

public record StopTime(
        String tripId,
        String stopId,
        int stopSequence,
        String arrivalTime,
        String departureTime
) {}
