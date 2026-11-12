package com.demo.transit.application.model;

public record Trip(
        String tripId,
        String routeId,
        String serviceId,
        String shapeId,
        String tripHeadsign,
        int directionId
) {}
