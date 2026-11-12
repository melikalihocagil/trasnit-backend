package com.demo.transit.infrastructure.osrm;

import com.demo.transit.application.model.Coordinate;
import com.demo.transit.application.model.Leg;
import com.demo.transit.application.model.RouteNavigation;
import com.demo.transit.infrastructure.osrm.dto.OsrmLeg;
import com.demo.transit.infrastructure.osrm.dto.OsrmLineString;
import com.demo.transit.infrastructure.osrm.dto.OsrmResponse;
import com.demo.transit.infrastructure.osrm.dto.OsrmRoute;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OsrmRoutingAdapterTest {

    @Mock
    private RestClient restClient;

    @Mock
    private RestClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    private OsrmRoutingAdapter adapter;

    @BeforeEach
    void setUp() {
        OsrmProperties properties = new OsrmProperties("http://localhost:5000", 3000);
        adapter = new OsrmRoutingAdapter(restClient, properties);
    }

    @Test
    @DisplayName("En az iki koordinat verilmediğinde IllegalArgumentException fırlatılmalıdır")
    void shouldThrowExceptionWhenCoordinatesLessThanTwo() {
        assertThatThrownBy(() -> adapter.calculateNavigation(List.of()))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> adapter.calculateNavigation(List.of(new Coordinate(39.9, 41.2))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Her leg'in kendi geometrisiyle geldiği OSRM yanıtı RouteNavigation'a dönüştürülmelidir")
    void shouldConvertOsrmResponseWithLegGeometriesToRouteNavigation() {
        // 1. Bacak çizgisi: (lon=41.1, lat=39.1) -> (lon=41.2, lat=39.2)
        OsrmLineString leg1Geom = new OsrmLineString(List.of(
                List.of(41.1, 39.1),
                List.of(41.2, 39.2)
        ));
        OsrmLeg leg1 = new OsrmLeg(1500.0, 180.0, leg1Geom);

        // 2. Bacak çizgisi: (lon=41.2, lat=39.2) -> (lon=41.3, lat=39.3)
        OsrmLineString leg2Geom = new OsrmLineString(List.of(
                List.of(41.2, 39.2),
                List.of(41.3, 39.3)
        ));
        OsrmLeg leg2 = new OsrmLeg(2000.0, 240.0, leg2Geom);

        OsrmRoute route = new OsrmRoute(3500.0, 420.0, List.of(leg1, leg2));
        OsrmResponse osrmResponse = new OsrmResponse("Ok", List.of(route));

        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(String.class))).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(OsrmResponse.class)).thenReturn(osrmResponse);

        RouteNavigation result = adapter.calculateNavigation(List.of(
                new Coordinate(39.1, 41.1),
                new Coordinate(39.2, 41.2),
                new Coordinate(39.3, 41.3)
        ));

        assertThat(result).isNotNull();
        assertThat(result.distance()).isEqualTo(3500.0);
        assertThat(result.duration()).isEqualTo(420.0);
        assertThat(result.legs()).hasSize(2);

        Leg firstLeg = result.legs().get(0);
        assertThat(firstLeg.distance()).isEqualTo(1500.0);
        assertThat(firstLeg.duration()).isEqualTo(180.0);
        assertThat(firstLeg.coordinates()).hasSize(2);
        assertThat(firstLeg.coordinates().get(0).lat()).isEqualTo(39.1);
        assertThat(firstLeg.coordinates().get(0).lon()).isEqualTo(41.1);
    }
}
