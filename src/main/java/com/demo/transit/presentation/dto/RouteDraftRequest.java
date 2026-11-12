package com.demo.transit.presentation.dto;

import com.demo.transit.application.model.Stop;
import com.demo.transit.application.model.Via;

import java.util.List;

public record RouteDraftRequest(
        String routeId,
        List<Stop> stops,
        List<Via> vias
) {}
