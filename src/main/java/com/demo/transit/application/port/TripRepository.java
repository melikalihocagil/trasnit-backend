package com.demo.transit.application.port;

import com.demo.transit.application.model.Trip;

import java.util.List;
import java.util.Optional;

public interface TripRepository {
    Optional<Trip> findRepresentativeTrip(String routeId);
    List<Trip> findByRouteId(String routeId);
    Optional<Trip> findById(String tripId);
}
