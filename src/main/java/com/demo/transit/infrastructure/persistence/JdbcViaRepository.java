package com.demo.transit.infrastructure.persistence;

import com.demo.transit.application.model.Via;
import com.demo.transit.application.port.ViaRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Repository
public class JdbcViaRepository implements ViaRepository {

    private static final String PARAM_ROUTE_ID = "routeId";
    private static final String PARAM_USER_ID = "userId";

    private final JdbcClient jdbcClient;
    private final JdbcTemplate jdbcTemplate;

    public JdbcViaRepository(JdbcClient jdbcClient, JdbcTemplate jdbcTemplate) {
        this.jdbcClient = jdbcClient;
        this.jdbcTemplate = jdbcTemplate;
    }

    private static Via mapVia(ResultSet rs, int rowNum) throws SQLException {
        return new Via(
                rs.getString("from_stop_id"),
                rs.getString("to_stop_id"),
                rs.getInt("via_sequence"),
                rs.getDouble("lat"),
                rs.getDouble("lon")
        );
    }

    @Override
    public List<Via> findPublishedVias(String routeId) {
        return jdbcClient.sql("""
                SELECT from_stop_id, to_stop_id, sequence as via_sequence, lat, lon
                FROM published.vias
                WHERE route_id = :routeId
                ORDER BY sequence
                """)
                .param(PARAM_ROUTE_ID, routeId)
                .query(JdbcViaRepository::mapVia)
                .list();
    }

    @Override
    public List<Via> findDraftVias(String routeId, String userId) {
        return jdbcClient.sql("""
                SELECT from_stop_id, to_stop_id, sequence as via_sequence, lat, lon
                FROM draft.vias
                WHERE user_id = :userId AND route_id = :routeId
                ORDER BY sequence
                """)
                .param(PARAM_USER_ID, userId)
                .param(PARAM_ROUTE_ID, routeId)
                .query(JdbcViaRepository::mapVia)
                .list();
    }

    @Override
    public void deleteDraftVias(String userId, String routeId) {
        jdbcClient.sql("""
                DELETE FROM draft.vias
                WHERE user_id = :userId AND route_id = :routeId
                """)
                .param(PARAM_USER_ID, userId)
                .param(PARAM_ROUTE_ID, routeId)
                .update();
    }

    @Override
    public void batchInsertDraftVias(String userId, String routeId, List<Via> vias) {
        if (vias == null || vias.isEmpty()) {
            return;
        }
        String sql = """
                INSERT INTO draft.vias (user_id, route_id, from_stop_id, to_stop_id, sequence, lat, lon)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        List<Object[]> batchArgs = new ArrayList<>();
        for (Via via : vias) {
            batchArgs.add(new Object[]{
                    userId,
                    routeId,
                    via.fromStopId(),
                    via.toStopId(),
                    via.viaSequence(),
                    via.lat(),
                    via.lon()
            });
        }
        jdbcTemplate.batchUpdate(sql, batchArgs);
    }
}
