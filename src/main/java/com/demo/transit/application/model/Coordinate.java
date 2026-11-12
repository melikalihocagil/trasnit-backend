package com.demo.transit.application.model;

public record Coordinate(
        double lat,
        double lon
) {
    public Coordinate {
        if (Double.isNaN(lat) || lat < -90.0 || lat > 90.0) {
            throw new IllegalArgumentException("Geçersiz enlem (lat): " + lat);
        }
        if (Double.isNaN(lon) || lon < -180.0 || lon > 180.0) {
            throw new IllegalArgumentException("Geçersiz boylam (lon): " + lon);
        }
    }
}
