package com.demo.transit.application.model;

public record Stop(
        String stopId,
        String stopName,
        double lat,
        double lon,
        int locationType
) {}
