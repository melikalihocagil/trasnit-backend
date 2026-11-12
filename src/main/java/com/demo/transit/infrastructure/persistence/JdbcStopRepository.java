package com.demo.transit.infrastructure.persistence;

import com.demo.transit.application.model.Stop;
import com.demo.transit.application.port.StopRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class JdbcStopRepository implements StopRepository {

    private static final String PARAM_USER_ID = "userId";
    private static final String PARAM_STOP_ID = "stopId";

    private final JdbcClient jdbcClient;
    private final JdbcTemplate jdbcTemplate;

    public JdbcStopRepository(JdbcClient jdbcClient, JdbcTemplate jdbcTemplate) {
        this.jdbcClient = jdbcClient;
        this.jdbcTemplate = jdbcTemplate;
    }

    private static Stop mapStop(ResultSet rs, int rowNum) throws SQLException {
        return new Stop(
                rs.getString("stop_id"),
                rs.getString("stop_name"),
                rs.getDouble("stop_lat"),
                rs.getDouble("stop_lon"),
                rs.getInt("location_type")
        );
    }

    @Override
    public List<Stop> findAllPublished() {
        return jdbcClient.sql("""
                SELECT stop_id, stop_name, stop_lat, stop_lon, location_type
                FROM published.stops
                ORDER BY stop_name
                """)
                .query(JdbcStopRepository::mapStop)
                .list();
    }

    @Override
    public List<Stop> findAllDraft(String userId) {
        return jdbcClient.sql("""
                SELECT stop_id, stop_name, stop_lat, stop_lon, location_type
                FROM draft.stops
                WHERE user_id = :userId
                ORDER BY stop_name
                """)
                .param(PARAM_USER_ID, userId)
                .query(JdbcStopRepository::mapStop)
                .list();
    }

    @Override
    public void saveDraftStop(String userId, Stop stop) {
        deleteDraftStop(userId, stop.stopId());
        jdbcClient.sql("""
                INSERT INTO draft.stops (user_id, stop_id, stop_name, stop_lat, stop_lon, location_type)
                VALUES (:userId, :stopId, :stopName, :lat, :lon, :locationType)
                """)
                .param(PARAM_USER_ID, userId)
                .param(PARAM_STOP_ID, stop.stopId())
                .param("stopName", stop.stopName())
                .param("lat", stop.lat())
                .param("lon", stop.lon())
                .param("locationType", stop.locationType())
                .update();
    }

    @Override
    public void saveAllDraftStops(String userId, List<Stop> stops) {
        if (stops == null || stops.isEmpty()) {
            return;
        }
        List<String> stopIds = stops.stream().map(Stop::stopId).toList();
        jdbcClient.sql("""
                DELETE FROM draft.stops
                WHERE user_id = :userId AND stop_id IN (:stopIds)
                """)
                .param(PARAM_USER_ID, userId)
                .param("stopIds", stopIds)
                .update();

        String insertSql = """
                INSERT INTO draft.stops (user_id, stop_id, stop_name, stop_lat, stop_lon, location_type)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        List<Object[]> batchArgs = stops.stream()
                .map(s -> new Object[]{userId, s.stopId(), s.stopName(), s.lat(), s.lon(), s.locationType()})
                .toList();
        jdbcTemplate.batchUpdate(insertSql, batchArgs);
    }

    @Override
    public void deleteDraftStop(String userId, String stopId) {
        jdbcClient.sql("""
                DELETE FROM draft.stops
                WHERE user_id = :userId AND stop_id = :stopId
                """)
                .param(PARAM_USER_ID, userId)
                .param(PARAM_STOP_ID, stopId)
                .update();
    }

    @Override
    public void deleteAllDraftStops(String userId) {
        jdbcClient.sql("""
                DELETE FROM draft.stops
                WHERE user_id = :userId
                """)
                .param(PARAM_USER_ID, userId)
                .update();
    }

    @Override
    public List<String> findRouteIdsByStopId(String stopId, String userId) {
        return findRouteIdsByStopIds(List.of(stopId), userId);
    }

    @Override
    public List<String> findRouteIdsByStopIds(List<String> stopIds, String userId) {
        if (stopIds == null || stopIds.isEmpty()) {
            return List.of();
        }
        return jdbcClient.sql("""
                SELECT DISTINCT t.route_id
                FROM published.trips t
                JOIN published.stop_times st ON t.trip_id = st.trip_id
                WHERE st.stop_id IN (:stopIds)
                UNION
                SELECT DISTINCT t.route_id
                FROM published.trips t
                JOIN draft.stop_times dst ON t.trip_id = dst.trip_id
                WHERE dst.user_id = :userId AND dst.stop_id IN (:stopIds)
                """)
                .param("stopIds", stopIds)
                .param(PARAM_USER_ID, userId)
                .query(String.class)
                .list();
    }
}
