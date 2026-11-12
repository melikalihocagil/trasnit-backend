package com.demo.transit.infrastructure.persistence;

import com.demo.transit.application.model.Stop;
import com.demo.transit.application.port.StopTimeRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Repository
public class JdbcStopTimeRepository implements StopTimeRepository {

    private static final String PARAM_TRIP_ID = "tripId";
    private static final String PARAM_USER_ID = "userId";

    private final JdbcClient jdbcClient;
    private final JdbcTemplate jdbcTemplate;

    public JdbcStopTimeRepository(JdbcClient jdbcClient, JdbcTemplate jdbcTemplate) {
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
    public List<Stop> findPublishedStops(String tripId) {
        return jdbcClient.sql("""
                SELECT s.stop_id, s.stop_name, s.stop_lat, s.stop_lon, s.location_type
                FROM published.stop_times st
                JOIN published.stops s ON st.stop_id = s.stop_id
                WHERE st.trip_id = :tripId
                ORDER BY st.stop_sequence
                """)
                .param(PARAM_TRIP_ID, tripId)
                .query(JdbcStopTimeRepository::mapStop)
                .list();
    }

    @Override
    public List<Stop> findDraftStopTimes(String tripId, String userId) {
        return jdbcClient.sql("""
                SELECT s.stop_id,
                    COALESCE(ds.stop_name, s.stop_name) as stop_name,
                    COALESCE(ds.stop_lat, s.stop_lat) as stop_lat,
                    COALESCE(ds.stop_lon, s.stop_lon) as stop_lon,
                    COALESCE(ds.location_type, s.location_type) as location_type
                FROM draft.stop_times dst
                JOIN published.stops s ON dst.stop_id = s.stop_id
                LEFT JOIN draft.stops ds ON ds.stop_id = s.stop_id AND ds.user_id = :userId
                WHERE dst.trip_id = :tripId AND dst.user_id = :userId
                ORDER BY dst.stop_sequence
                """)
                .param(PARAM_TRIP_ID, tripId)
                .param(PARAM_USER_ID, userId)
                .query(JdbcStopTimeRepository::mapStop)
                .list();
    }

    @Override
    public List<Stop> findDraftStops(String tripId, String userId) {
        return jdbcClient.sql("""
                SELECT
                    s.stop_id,
                    COALESCE(ds.stop_name, s.stop_name) as stop_name,
                    COALESCE(ds.stop_lat, s.stop_lat) as stop_lat,
                    COALESCE(ds.stop_lon, s.stop_lon) as stop_lon,
                    COALESCE(ds.location_type, s.location_type) as location_type
                FROM published.stop_times st
                JOIN published.stops s ON st.stop_id = s.stop_id
                LEFT JOIN draft.stops ds ON ds.stop_id = s.stop_id AND ds.user_id = :userId
                WHERE st.trip_id = :tripId
                ORDER BY st.stop_sequence
                """)
                .param(PARAM_TRIP_ID, tripId)
                .param(PARAM_USER_ID, userId)
                .query(JdbcStopTimeRepository::mapStop)
                .list();
    }

    @Override
    public boolean hasDraftStops(String tripId, String userId) {
        return Boolean.TRUE.equals(jdbcClient.sql("""
                SELECT EXISTS (
                    SELECT 1
                    FROM published.stop_times st
                    JOIN draft.stops ds ON st.stop_id = ds.stop_id AND ds.user_id = :userId
                    WHERE st.trip_id = :tripId
                )
                """)
                .param(PARAM_TRIP_ID, tripId)
                .param(PARAM_USER_ID, userId)
                .query(Boolean.class)
                .single());
    }

    @Override
    public void deleteDraftStopTimes(String userId, String tripId) {
        jdbcClient.sql("""
                DELETE FROM draft.stop_times
                WHERE user_id = :userId AND trip_id = :tripId
                """)
                .param(PARAM_USER_ID, userId)
                .param(PARAM_TRIP_ID, tripId)
                .update();
    }

    @Override
    public void batchInsertDraftStopTimes(String userId, String tripId, List<Stop> stops) {
        if (stops == null || stops.isEmpty()) {
            return;
        }
        String sql = """
                INSERT INTO draft.stop_times (user_id, trip_id, stop_id, stop_sequence, arrival_time, departure_time)
                VALUES (?, ?, ?, ?, '00:00:00', '00:00:00')
                """;
        List<Object[]> batchArgs = new ArrayList<>();
        for (int i = 0; i < stops.size(); i++) {
            batchArgs.add(new Object[]{userId, tripId, stops.get(i).stopId(), i + 1});
        }
        jdbcTemplate.batchUpdate(sql, batchArgs);
    }
}
