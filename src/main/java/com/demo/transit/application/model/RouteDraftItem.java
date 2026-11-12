package com.demo.transit.application.model;

import java.util.List;

public record RouteDraftItem(
        String routeId,
        List<Stop> stops,
        List<Via> vias
) {}
