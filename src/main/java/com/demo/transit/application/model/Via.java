package com.demo.transit.application.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Via(
        String fromStopId,
        String toStopId,
        @JsonProperty("viaSequence") int viaSequence,
        double lat,
        double lon
) {}
