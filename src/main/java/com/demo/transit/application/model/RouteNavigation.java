package com.demo.transit.application.model;

import java.util.List;

public record RouteNavigation(
        double distance,
        double duration,
        List<Leg> legs
) {
    public RouteNavigation {
        legs = legs == null ? List.of() : List.copyOf(legs);
    }
}
