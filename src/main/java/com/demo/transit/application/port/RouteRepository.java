package com.demo.transit.application.port;

import com.demo.transit.application.model.Route;
import com.demo.transit.presentation.dto.RouteReviewItem;

import java.util.List;
import java.util.Optional;

public interface RouteRepository {
    List<Route> findAll(String userId);
    Optional<Route> findById(String routeId);
    void saveRouteReviews(String userId, List<RouteReviewItem> reviews);
    List<String> findReviewedRouteIds(String userId);
}
