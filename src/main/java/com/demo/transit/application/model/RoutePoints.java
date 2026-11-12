package com.demo.transit.application.model;

import java.util.List;

public record RoutePoints(
        List<Stop> stops,
        List<Via> vias
) {}
