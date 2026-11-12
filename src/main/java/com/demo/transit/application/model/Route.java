package com.demo.transit.application.model;

public record Route(
        String routeId,
        String agencyId,
        String routeShortName,
        String routeLongName,
        int routeType,
        String routeColor,
        String routeTextColor
) {}
