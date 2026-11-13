package com.demo.transit.application.model;

import java.util.List;

/**
 * Durak değişiminden etkilenen bir hat ve bu hattı etkileyen durakların ID'leri.
 */
public record AffectedRoute(
        String routeId,
        List<String> stopIds
) {
    public AffectedRoute {
        stopIds = stopIds == null ? List.of() : List.copyOf(stopIds);
    }
}
