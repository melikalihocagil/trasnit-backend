package com.demo.transit.infrastructure.persistence;

import com.demo.transit.application.model.Route;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JdbcRouteRepository Hat Bilgisi Taslak Kayıt Testleri (H2)")
class JdbcRouteRepositoryRouteInfoDraftTest {

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
                CREATE TABLE draft.routes (
                    user_id          VARCHAR NOT NULL,
                    route_id         VARCHAR NOT NULL,
                    agency_id        VARCHAR,
                    route_short_name VARCHAR NOT NULL,
                    route_long_name  VARCHAR NOT NULL,
                    route_type       INTEGER NOT NULL DEFAULT 3,
                    route_color      VARCHAR DEFAULT '000000',
                    route_text_color VARCHAR DEFAULT 'FFFFFF',
                    action_type      VARCHAR NOT NULL DEFAULT 'UPDATE',
                    updated_at       TIMESTAMP NOT NULL DEFAULT NOW(),
                    PRIMARY KEY (user_id, route_id)
                )
                """);
        repository = new JdbcRouteRepository(JdbcClient.create(dataSource), jdbcTemplate);
    }

    @Test
    @DisplayName("Kayıt, kullanıcının önceki taslağını tamamen silip gönderilen hat listesiyle değiştirmelidir")
    void shouldReplacePreviousDraftWithRequestList() {
        Route r1 = new Route("R1", "A1", "1A", "Atatürk - Çarşı", 3, "FF0000", "FFFFFF");
        Route r2 = new Route("R2", "A1", "2B", "Gar - Üniversite", 3, "00FF00", "000000");
        repository.replaceAllDraft(USER_ID, List.of(r1, r2));

        Route r2Edited = new Route("R2", "A1", "2B", "Gar - Yakutiye", 3, "00FF00", "000000");
        Route r3 = new Route("R3", null, "3", "Yeni Hat", 3, null, null);
        repository.replaceAllDraft(USER_ID, List.of(r2Edited, r3));

        assertThat(repository.findAllDraft(USER_ID)).containsExactly(r2Edited, r3);
    }

    @Test
    @DisplayName("Boş liste geldiğinde kullanıcının taslağı temizlenmeli, diğer kullanıcılar etkilenmemelidir")
    void shouldClearOnlyCurrentUserOnEmptyList() {
        Route r1 = new Route("R1", "A1", "1A", "Atatürk - Çarşı", 3, "FF0000", "FFFFFF");
        Route r9 = new Route("R9", "A1", "9", "Dadaşkent", 3, "0000FF", "FFFFFF");
        repository.replaceAllDraft(USER_ID, List.of(r1));
        repository.replaceAllDraft(OTHER_USER_ID, List.of(r9));

        repository.replaceAllDraft(USER_ID, List.of());

        assertThat(repository.findAllDraft(USER_ID)).isEmpty();
        assertThat(repository.findAllDraft(OTHER_USER_ID)).containsExactly(r9);
    }
}
