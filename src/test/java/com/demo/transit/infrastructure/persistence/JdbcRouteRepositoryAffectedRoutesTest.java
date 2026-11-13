package com.demo.transit.infrastructure.persistence;

import com.demo.transit.application.model.AffectedRoute;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JdbcRouteRepository Etkilenen Hat Kayıt Testleri (H2)")
class JdbcRouteRepositoryAffectedRoutesTest {

    private static final String USER_ID = "operator-1";
    private static final String OTHER_USER_ID = "operator-2";

    private JdbcRouteRepository repository;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1", "sa", "");
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.execute("CREATE SCHEMA draft");
        jdbcTemplate.execute("""
                CREATE TABLE draft.affected_routes (
                    user_id    VARCHAR NOT NULL,
                    route_id   VARCHAR NOT NULL,
                    stop_ids   VARCHAR ARRAY NOT NULL,
                    updated_at TIMESTAMP NOT NULL,
                    PRIMARY KEY (user_id, route_id)
                )
                """);
        repository = new JdbcRouteRepository(JdbcClient.create(dataSource), jdbcTemplate);
    }

    @Test
    @DisplayName("Kayıt, kullanıcının önceki listesini silip istekteki listeyle değiştirmelidir")
    void shouldReplacePreviousListWithRequestList() {
        repository.replaceAffectedRoutes(USER_ID, List.of(
                new AffectedRoute("R1", List.of("S1")),
                new AffectedRoute("R2", List.of("S2"))
        ));

        repository.replaceAffectedRoutes(USER_ID, List.of(
                new AffectedRoute("R2", List.of("S3", "S4")),
                new AffectedRoute("R3", List.of())
        ));

        assertThat(repository.findAffectedRoutes(USER_ID)).containsExactly(
                new AffectedRoute("R2", List.of("S3", "S4")),
                new AffectedRoute("R3", List.of())
        );
    }

    @Test
    @DisplayName("Boş liste geldiğinde kullanıcının tüm kayıtları silinmeli, diğer kullanıcılar etkilenmemelidir")
    void shouldClearOnlyCurrentUserOnEmptyList() {
        repository.replaceAffectedRoutes(USER_ID, List.of(new AffectedRoute("R1", List.of("S1"))));
        repository.replaceAffectedRoutes(OTHER_USER_ID, List.of(new AffectedRoute("R9", List.of("S9"))));

        repository.replaceAffectedRoutes(USER_ID, List.of());

        assertThat(repository.findAffectedRoutes(USER_ID)).isEmpty();
        assertThat(repository.findAffectedRoutes(OTHER_USER_ID))
                .containsExactly(new AffectedRoute("R9", List.of("S9")));
    }

    @Test
    @DisplayName("Aynı hat istekte birden fazla gelirse son gelen kaydedilmelidir")
    void shouldKeepLastEntryForDuplicateRoute() {
        repository.replaceAffectedRoutes(USER_ID, List.of(
                new AffectedRoute("R1", List.of("S1")),
                new AffectedRoute("R1", List.of("S2"))
        ));

        assertThat(repository.findAffectedRoutes(USER_ID))
                .containsExactly(new AffectedRoute("R1", List.of("S2")));
    }
}
