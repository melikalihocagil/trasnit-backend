package com.demo.transit.presentation.dto;

import com.demo.transit.application.model.RouteNavigation;
import com.demo.transit.application.model.Stop;
import com.demo.transit.application.model.Via;

import java.util.List;

public record RouteDetailResponse(
        List<Stop> stops,
        List<Via> vias,
        RouteNavigation navigation
) {}
