package com.demo.transit.infrastructure.persistence;

import com.demo.transit.application.model.Route;
import com.demo.transit.application.port.RouteRepository;
import com.demo.transit.presentation.dto.RouteReviewItem;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
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
    public void saveRouteReviews(String userId, List<RouteReviewItem> reviews) {
        if (reviews == null || reviews.isEmpty()) {
            return;
        }
        String sql = """
                INSERT INTO draft.route_reviews (user_id, route_id, is_reviewed, reviewed_at)
                VALUES (?, ?, ?, NOW())
                ON CONFLICT (user_id, route_id)
                DO UPDATE SET is_reviewed = EXCLUDED.is_reviewed, reviewed_at = NOW()
                """;
        List<Object[]> batchArgs = new ArrayList<>();
        for (RouteReviewItem item : reviews) {
            batchArgs.add(new Object[]{userId, item.routeId(), item.reviewed()});
        }
        jdbcTemplate.batchUpdate(sql, batchArgs);
    }

    @Override
    public List<String> findReviewedRouteIds(String userId) {
        return jdbcClient.sql("""
                SELECT route_id
                FROM draft.route_reviews
                WHERE user_id = :userId AND is_reviewed = true
                ORDER BY route_id
                """)
                .param(PARAM_USER_ID, userId)
                .query(String.class)
                .list();
    }
}
