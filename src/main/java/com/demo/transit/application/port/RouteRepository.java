package com.demo.transit.application.port;

import com.demo.transit.application.model.Route;
import com.demo.transit.application.model.AffectedRoute;

import java.util.List;
import java.util.Optional;

public interface RouteRepository {
    List<Route> findAll(String userId);
    Optional<Route> findById(String routeId);
    void replaceAffectedRoutes(String userId, List<AffectedRoute> affectedRoutes);
    List<AffectedRoute> findAffectedRoutes(String userId);
}
