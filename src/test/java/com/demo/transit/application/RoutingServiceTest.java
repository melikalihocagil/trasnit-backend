package com.demo.transit.application;

import com.demo.transit.application.model.Coordinate;
import com.demo.transit.application.model.Leg;
import com.demo.transit.application.model.RouteNavigation;
import com.demo.transit.application.model.Stop;
import com.demo.transit.application.model.Via;
import com.demo.transit.application.port.RoutingEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RoutingService Birim Testleri")
class RoutingServiceTest {

    @Mock
    private RoutingEngine routingEngine;

    @InjectMocks
    private RoutingService routingService;

    @Nested
    @DisplayName("Navigasyon Hesaplama (calculateNavigation) Testleri")
    class CalculateNavigationTests {

        @Test
        @DisplayName("Durak listesi null ise OSRM motoru çağrılmamalı ve boş navigasyon dönmelidir")
        void shouldReturnEmptyNavigationWhenStopsIsNull() {
            // When
            RouteNavigation result = routingService.calculateNavigation(null, null);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.distance()).isEqualTo(0.0);
            assertThat(result.duration()).isEqualTo(0.0);
            assertThat(result.legs()).isEmpty();
            verify(routingEngine, never()).calculateNavigation(anyList());
        }

        @Test
        @DisplayName("Durak sayısı 2'den az ise OSRM motoru çağrılmamalı ve boş navigasyon dönmelidir")
        void shouldReturnEmptyNavigationWhenStopsLessThanTwo() {
            // Given
            Stop singleStop = new Stop("S1", "Tek Durak", 39.90, 41.25, 0);

            // When
            RouteNavigation resultEmpty = routingService.calculateNavigation(List.of(), null);
            RouteNavigation resultSingle = routingService.calculateNavigation(List.of(singleStop), List.of());

            // Then
            assertThat(resultEmpty.legs()).isEmpty();
            assertThat(resultEmpty.distance()).isEqualTo(0.0);
            assertThat(resultSingle.legs()).isEmpty();
            assertThat(resultSingle.distance()).isEqualTo(0.0);
            verify(routingEngine, never()).calculateNavigation(anyList());
        }

        @Test
        @DisplayName("Via listesi null veya boş olduğunda sadece durak koordinatları ile OSRM çağrılmalıdır")
        void shouldCalculateNavigationWithStopsOnlyWhenViasEmptyOrNull() {
            // Given
            Stop s1 = new Stop("S1", "Durak 1", 39.90, 41.25, 0);
            Stop s2 = new Stop("S2", "Durak 2", 39.92, 41.27, 0);
            List<Stop> stops = List.of(s1, s2);

            Leg leg = new Leg(List.of(new Coordinate(39.90, 41.25), new Coordinate(39.92, 41.27)), 2500.0, 300.0);
            RouteNavigation expectedNav = new RouteNavigation(2500.0, 300.0, List.of(leg));

            when(routingEngine.calculateNavigation(List.of(
                    new Coordinate(39.90, 41.25),
                    new Coordinate(39.92, 41.27)
            ))).thenReturn(expectedNav);

            // When
            RouteNavigation resultWithNull = routingService.calculateNavigation(stops, null);
            RouteNavigation resultWithEmpty = routingService.calculateNavigation(stops, List.of());

            // Then
            assertThat(resultWithNull).isEqualTo(expectedNav);
            assertThat(resultWithEmpty).isEqualTo(expectedNav);
        }

        @Test
        @DisplayName("Via noktaları ilgili segmentler arasında sequence sırasına göre dizilerek OSRM motoruna verilmelidir")
        void shouldOrderViasCorrectlyBetweenStopSegments() {
            // Given
            Stop s1 = new Stop("S1", "Durak 1", 39.10, 41.10, 0);
            Stop s2 = new Stop("S2", "Durak 2", 39.30, 41.30, 0);
            Stop s3 = new Stop("S3", "Durak 3", 39.50, 41.50, 0);

            // S1 -> S2 segmenti için karışık sırada vialar
            Via via1_seq2 = new Via("S1", "S2", 2, 39.25, 41.25);
            Via via1_seq1 = new Via("S1", "S2", 1, 39.15, 41.15);

            // S2 -> S3 segmenti için tek via
            Via via2_seq1 = new Via("S2", "S3", 1, 39.40, 41.40);

            // İlgisiz segment viası (rotaya girmemeli)
            Via unrelatedVia = new Via("S99", "S100", 1, 99.0, 99.0);

            List<Stop> stops = List.of(s1, s2, s3);
            List<Via> vias = List.of(via1_seq2, unrelatedVia, via2_seq1, via1_seq1);

            Leg leg1 = new Leg(List.of(new Coordinate(39.10, 41.10)), 100.0, 10.0);
            RouteNavigation mockResponse = new RouteNavigation(100.0, 10.0, List.of(leg1));

            List<Coordinate> expectedCoordinates = List.of(
                    new Coordinate(39.10, 41.10), // S1
                    new Coordinate(39.15, 41.15), // S1-S2 Via 1
                    new Coordinate(39.25, 41.25), // S1-S2 Via 2
                    new Coordinate(39.30, 41.30), // S2
                    new Coordinate(39.40, 41.40), // S2-S3 Via 1
                    new Coordinate(39.50, 41.50)  // S3
            );

            when(routingEngine.calculateNavigation(expectedCoordinates)).thenReturn(mockResponse);

            // When
            RouteNavigation result = routingService.calculateNavigation(stops, vias);

            // Then
            assertThat(result).isEqualTo(mockResponse);
            verify(routingEngine).calculateNavigation(expectedCoordinates);
        }
    }

    @Nested
    @DisplayName("Geometri Değişim Kontrolü (hasGeometryChanged) Testleri")
    class HasGeometryChangedTests {

        @Test
        @DisplayName("Her iki navigasyon da null ise geometri değişmemiştir (false)")
        void shouldReturnFalseWhenBothAreNull() {
            // When & Then
            assertThat(routingService.hasGeometryChanged(null, null)).isFalse();
        }

        @Test
        @DisplayName("Biri null diğeri dolu ise geometri kesinlikle değişmiştir (true)")
        void shouldReturnTrueWhenOneIsNull() {
            // Given
            RouteNavigation nav = new RouteNavigation(100.0, 20.0, List.of());

            // When & Then
            assertThat(routingService.hasGeometryChanged(nav, null)).isTrue();
            assertThat(routingService.hasGeometryChanged(null, nav)).isTrue();
        }

        @Test
        @DisplayName("Toplam koordinat noktası sayısı farklı ise geometri değişmiştir (true)")
        void shouldReturnTrueWhenPointCountsDiffer() {
            // Given
            Leg leg1 = new Leg(List.of(new Coordinate(39.1, 41.1)), 100.0, 20.0);
            Leg leg2 = new Leg(List.of(new Coordinate(39.1, 41.1), new Coordinate(39.2, 41.2)), 100.0, 20.0);

            RouteNavigation nav1 = new RouteNavigation(100.0, 20.0, List.of(leg1));
            RouteNavigation nav2 = new RouteNavigation(100.0, 20.0, List.of(leg2));

            // When & Then
            assertThat(routingService.hasGeometryChanged(nav1, nav2)).isTrue();
        }

        @Test
        @DisplayName("Mesafe farkı 1 metreden fazla (> 1.0) ise geometri değişmiştir (true)")
        void shouldReturnTrueWhenDistanceDiffersMoreThanOneMeter() {
            // Given
            Coordinate coord = new Coordinate(39.1, 41.1);
            Leg leg1 = new Leg(List.of(coord), 1000.0, 100.0);
            Leg leg2 = new Leg(List.of(coord), 1001.5, 100.0);

            RouteNavigation nav1 = new RouteNavigation(1000.0, 100.0, List.of(leg1));
            RouteNavigation nav2 = new RouteNavigation(1001.5, 100.0, List.of(leg2));

            // When & Then
            assertThat(routingService.hasGeometryChanged(nav1, nav2)).isTrue();
        }

        @Test
        @DisplayName("Mesafe farkı 1 metreden az veya eşit ise ve içerik aynı ise değişmemiştir (false)")
        void shouldReturnFalseWhenDistanceDiffersWithinToleranceAndContentIdentical() {
            // Given
            Coordinate coord = new Coordinate(39.1, 41.1);
            Leg leg = new Leg(List.of(coord), 1000.0, 100.0);

            RouteNavigation nav1 = new RouteNavigation(1000.0, 100.0, List.of(leg));
            RouteNavigation nav2 = new RouteNavigation(1000.0, 100.0, List.of(leg));

            // When & Then
            assertThat(routingService.hasGeometryChanged(nav1, nav2)).isFalse();
        }

        @Test
        @DisplayName("Nokta sayısı ve mesafe aynı olmasına rağmen koordinat veya süre içeriği farklı ise geometri değişmiştir (true)")
        void shouldReturnTrueWhenCoordinatesOrContentDifferEvenWithSameCountAndDistance() {
            // Given
            Leg leg1 = new Leg(List.of(new Coordinate(39.1, 41.1)), 1000.0, 100.0);
            Leg leg2 = new Leg(List.of(new Coordinate(39.2, 41.2)), 1000.0, 100.0);

            RouteNavigation nav1 = new RouteNavigation(1000.0, 100.0, List.of(leg1));
            RouteNavigation nav2 = new RouteNavigation(1000.0, 100.0, List.of(leg2));

            // When & Then
            assertThat(routingService.hasGeometryChanged(nav1, nav2)).isTrue();
        }
    }
}
