package com.demo.transit.application.model;

import java.util.List;

public record Leg(
        List<Coordinate> coordinates,
        Double distance,
        Double duration
) {}
