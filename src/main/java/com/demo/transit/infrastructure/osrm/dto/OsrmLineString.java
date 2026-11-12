package com.demo.transit.infrastructure.osrm.dto;

import com.demo.transit.application.model.Coordinate;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OsrmLineString(
        List<List<Double>> coordinates
) {
    public List<Coordinate> toCoordinates() {
        if (coordinates == null) {
            return List.of();
        }
        return coordinates.stream()
                .filter(pt -> pt != null && pt.size() >= 2)
                .map(pt -> new Coordinate(pt.get(1), pt.get(0))) // [lon, lat] -> Coordinate(lat, lon)
                .toList();
    }
}
