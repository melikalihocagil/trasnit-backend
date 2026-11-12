package com.demo.transit.infrastructure.osrm.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OsrmRoute(
        double distance,
        double duration,
        List<OsrmLeg> legs
) {}
