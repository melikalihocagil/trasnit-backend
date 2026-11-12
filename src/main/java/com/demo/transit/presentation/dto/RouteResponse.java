package com.demo.transit.presentation.dto;

public record RouteResponse(
        String routeId,
        String routeShortName,
        String routeLongName,
        String routeColor,
        String routeTextColor,
        int directionId,
        String tripHeadsign
) {}
