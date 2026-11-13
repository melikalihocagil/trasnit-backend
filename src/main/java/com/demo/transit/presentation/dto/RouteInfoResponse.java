package com.demo.transit.presentation.dto;

import com.demo.transit.application.model.Route;

import java.util.List;

public record RouteInfoResponse(
        List<Route> published,
        List<Route> draft
) {}
