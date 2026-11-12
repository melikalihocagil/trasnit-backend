package com.demo.transit.infrastructure.persistence;

import com.demo.transit.application.model.Trip;
import com.demo.transit.application.port.TripRepository;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcTripRepository implements TripRepository {

    private static final String PARAM_ROUTE_ID = "routeId";
    private static final String PARAM_TRIP_ID = "tripId";

    private final JdbcClient jdbcClient;

    public JdbcTripRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    private static Trip mapTrip(ResultSet rs, int rowNum) throws SQLException {
        return new Trip(
                rs.getString("trip_id"),
                rs.getString("route_id"),
                rs.getString("service_id"),
                rs.getString("shape_id"),
                rs.getString("trip_headsign"),
                rs.getInt("direction_id")
        );
    }

    @Override
    public Optional<Trip> findRepresentativeTrip(String routeId) {
        return jdbcClient.sql("""
                SELECT trip_id, route_id, service_id, shape_id, trip_headsign, direction_id
                FROM published.trips
                WHERE route_id = :routeId
                ORDER BY trip_id NULLS LAST
                LIMIT 1
                """)
                .param(PARAM_ROUTE_ID, routeId)
                .query(JdbcTripRepository::mapTrip)
                .optional();
    }

    @Override
    public List<Trip> findByRouteId(String routeId) {
        return jdbcClient.sql("""
                SELECT trip_id, route_id, service_id, shape_id, trip_headsign, direction_id
                FROM published.trips
                WHERE route_id = :routeId
                ORDER BY trip_id
                """)
                .param(PARAM_ROUTE_ID, routeId)
                .query(JdbcTripRepository::mapTrip)
                .list();
    }

    @Override
    public Optional<Trip> findById(String tripId) {
        return jdbcClient.sql("""
                SELECT trip_id, route_id, service_id, shape_id, trip_headsign, direction_id
                FROM published.trips
                WHERE trip_id = :tripId
                """)
                .param(PARAM_TRIP_ID, tripId)
                .query(JdbcTripRepository::mapTrip)
                .optional();
    }
}
