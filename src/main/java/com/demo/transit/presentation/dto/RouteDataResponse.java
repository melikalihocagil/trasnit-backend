package com.demo.transit.presentation.dto;

public record RouteDataResponse(
        String routeId,
        RouteDetailResponse published,
        RouteDetailResponse draft
) {}
