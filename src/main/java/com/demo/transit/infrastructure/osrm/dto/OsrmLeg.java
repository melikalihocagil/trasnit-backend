package com.demo.transit.infrastructure.osrm.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OsrmLeg(
        double distance,
        double duration,
        OsrmLineString geometry
) {}
