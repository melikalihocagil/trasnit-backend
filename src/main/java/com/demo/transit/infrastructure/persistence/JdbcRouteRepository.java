package com.demo.transit.infrastructure.persistence;

import com.demo.transit.application.model.AffectedRoute;
import com.demo.transit.application.model.Route;
import com.demo.transit.application.port.RouteRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class JdbcRouteRepository implements RouteRepository {

    private static final String PARAM_ROUTE_ID = "routeId";
    private static final String PARAM_USER_ID = "userId";

    private final JdbcClient jdbcClient;
    private final JdbcTemplate jdbcTemplate;

    public JdbcRouteRepository(JdbcClient jdbcClient, JdbcTemplate jdbcTemplate) {
        this.jdbcClient = jdbcClient;
        this.jdbcTemplate = jdbcTemplate;
    }

    private static Route mapRoute(ResultSet rs, int rowNum) throws SQLException {
        return new Route(
                rs.getString("route_id"),
                rs.getString("agency_id"),
                rs.getString("route_short_name"),
                rs.getString("route_long_name"),
                rs.getInt("route_type"),
                rs.getString("route_color"),
                rs.getString("route_text_color")
        );
    }

    @Override
    public List<Route> findAll(String userId) {
        return jdbcClient.sql("""
                SELECT
                    route_id,
                    agency_id,
                    route_short_name,
                    route_long_name,
                    route_type,
                    route_color,
                    route_text_color
                FROM published.routes
                ORDER BY route_id
                """)
                .query(JdbcRouteRepository::mapRoute)
                .list();
    }

    @Override
    public Optional<Route> findById(String routeId) {
        return jdbcClient.sql("""
                SELECT
                    route_id,
                    agency_id,
                    route_short_name,
                    route_long_name,
                    route_type,
                    route_color,
                    route_text_color
                FROM published.routes
                WHERE route_id = :routeId
                """)
                .param(PARAM_ROUTE_ID, routeId)
                .query(JdbcRouteRepository::mapRoute)
                .optional();
    }

    @Override
    public void replaceAffectedRoutes(String userId, List<AffectedRoute> affectedRoutes) {
        // Kullanıcının mevcut listesi her durumda silinir; boş istek tüm kayıtları temizler
        jdbcClient.sql("DELETE FROM draft.affected_routes WHERE user_id = :userId")
                .param(PARAM_USER_ID, userId)
                .update();

        if (affectedRoutes == null || affectedRoutes.isEmpty()) {
            return;
        }
        // Aynı hat istekte birden fazla geldiyse son gelen geçerli olur
        Map<String, AffectedRoute> byRouteId = new LinkedHashMap<>();
        for (AffectedRoute route : affectedRoutes) {
            if (route != null && route.routeId() != null) {
                byRouteId.put(route.routeId(), route);
            }
        }
        List<AffectedRoute> rows = List.copyOf(byRouteId.values());
        if (rows.isEmpty()) {
            return;
        }
        String sql = """
                INSERT INTO draft.affected_routes (user_id, route_id, stop_ids, updated_at)
                VALUES (?, ?, ?, NOW())
                """;
        jdbcTemplate.batchUpdate(sql, rows, rows.size(), (ps, route) -> {
            ps.setString(1, userId);
            ps.setString(2, route.routeId());
            ps.setArray(3, ps.getConnection().createArrayOf("text", route.stopIds().toArray()));
        });
    }

    @Override
    public List<AffectedRoute> findAffectedRoutes(String userId) {
        return jdbcClient.sql("""
                SELECT route_id, stop_ids
                FROM draft.affected_routes
                WHERE user_id = :userId
                ORDER BY route_id
                """)
                .param(PARAM_USER_ID, userId)
                .query((rs, rowNum) -> new AffectedRoute(rs.getString("route_id"), toStringList(rs.getArray("stop_ids"))))
                .list();
    }

    private static List<String> toStringList(Array sqlArray) throws SQLException {
        if (sqlArray == null) {
            return List.of();
        }
        Object[] values = (Object[]) sqlArray.getArray();
        return Arrays.stream(values).map(String::valueOf).toList();
    }
}
