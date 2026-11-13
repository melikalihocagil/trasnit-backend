package com.demo.transit.application;

import com.demo.transit.application.model.Route;
import com.demo.transit.application.model.AffectedRoute;
import com.demo.transit.application.model.RouteDraftItem;
import com.demo.transit.application.model.RoutePoints;
import com.demo.transit.application.model.Stop;
import com.demo.transit.application.model.Trip;
import com.demo.transit.application.model.Via;
import com.demo.transit.application.port.RouteRepository;
import com.demo.transit.application.port.StopTimeRepository;
import com.demo.transit.application.port.TripRepository;
import com.demo.transit.application.port.ViaRepository;
import com.demo.transit.presentation.dto.RouteResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RouteService Birim Testleri")
class RouteServiceTest {

    @Mock
    private RouteRepository routeRepository;

    @Mock
    private TripRepository tripRepository;

    @Mock
    private StopTimeRepository stopTimeRepository;

    @Mock
    private ViaRepository viaRepository;

    @InjectMocks
    private RouteService routeService;

    private static final String USER_ID = "operator-1";
    private static final String ROUTE_ID = "R1";
    private static final String TRIP_ID = "T1";

    @Nested
    @DisplayName("Hat Listeleme ve Detay (getRoutes & getRoute) Testleri")
    class RouteQueryTests {

        @Test
        @DisplayName("getRoutes: Sefer (Trip) bilgisi mevcut olan hatları directionId ve tripHeadsign ile zenginleştirmelidir")
        void shouldReturnRoutesWithRepresentativeTripInfo() {
            // Given
            Route route = new Route("R1", "A1", "1A", "Atatürk - Çarşı", 3, "#FF0000", "#FFFFFF");
            Trip trip = new Trip(TRIP_ID, "R1", "S1", "SH1", "Çarşı Yönü", 1);

            when(routeRepository.findAll(USER_ID)).thenReturn(List.of(route));
            when(tripRepository.findRepresentativeTrip("R1")).thenReturn(Optional.of(trip));

            // When
            List<RouteResponse> results = routeService.getRoutes(USER_ID);

            // Then
            assertThat(results).hasSize(1);
            RouteResponse response = results.getFirst();
            assertThat(response.routeId()).isEqualTo("R1");
            assertThat(response.routeShortName()).isEqualTo("1A");
            assertThat(response.routeLongName()).isEqualTo("Atatürk - Çarşı");
            assertThat(response.routeColor()).isEqualTo("#FF0000");
            assertThat(response.routeTextColor()).isEqualTo("#FFFFFF");
            assertThat(response.directionId()).isEqualTo(1);
            assertThat(response.tripHeadsign()).isEqualTo("Çarşı Yönü");

            verify(routeRepository).findAll(USER_ID);
            verify(tripRepository).findRepresentativeTrip("R1");
        }

        @Test
        @DisplayName("getRoutes: Sefer (Trip) bilgisi bulunmayan hatlar için varsayılan directionId(0) ve boş headsign dönmelidir")
        void shouldFallbackToDefaultsWhenNoRepresentativeTripExists() {
            // Given
            Route route = new Route("R2", "A1", "2B", "Gar - Üniversite", 3, "#00FF00", "#000000");

            when(routeRepository.findAll(USER_ID)).thenReturn(List.of(route));
            when(tripRepository.findRepresentativeTrip("R2")).thenReturn(Optional.empty());

            // When
            List<RouteResponse> results = routeService.getRoutes(USER_ID);

            // Then
            assertThat(results).hasSize(1);
            RouteResponse response = results.getFirst();
            assertThat(response.routeId()).isEqualTo("R2");
            assertThat(response.directionId()).isEqualTo(0);
            assertThat(response.tripHeadsign()).isEmpty();
        }

        @Test
        @DisplayName("getRoute: Hat mevcut olduğunda Route nesnesini dönmelidir")
        void shouldReturnRouteWhenFound() {
            // Given
            Route route = new Route(ROUTE_ID, "A1", "1A", "Merkez Hat", 3, "#FF0000", "#FFFFFF");
            when(routeRepository.findById(ROUTE_ID)).thenReturn(Optional.of(route));

            // When
            Route result = routeService.getRoute(ROUTE_ID);

            // Then
            assertThat(result).isEqualTo(route);
            verify(routeRepository).findById(ROUTE_ID);
        }

        @Test
        @DisplayName("getRoute: Hat bulunamadığında NoSuchElementException fırlatmalıdır")
        void shouldThrowExceptionWhenRouteNotFound() {
            // Given
            when(routeRepository.findById("UNKNOWN")).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> routeService.getRoute("UNKNOWN"))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessageContaining("Route not found: UNKNOWN");
        }
    }

    @Nested
    @DisplayName("Canlı Nokta Sorgulama (getPublishedPoints) Testleri")
    class PublishedPointsTests {

        @Test
        @DisplayName("getPublishedPoints: Temsili sefer ve canlı vialar mevcut olduğunda tam RoutePoints dönmelidir")
        void shouldReturnPublishedPointsWhenTripExists() {
            // Given
            Trip trip = new Trip(TRIP_ID, ROUTE_ID, "S1", "SH1", "Varış", 0);
            Stop stop = new Stop("S1", "Durak 1", 39.90, 41.25, 0);
            Via via = new Via("S1", "S2", 1, 39.91, 41.26);

            when(tripRepository.findRepresentativeTrip(ROUTE_ID)).thenReturn(Optional.of(trip));
            when(stopTimeRepository.findPublishedStops(TRIP_ID)).thenReturn(List.of(stop));
            when(viaRepository.findPublishedVias(ROUTE_ID)).thenReturn(List.of(via));

            // When
            RoutePoints points = routeService.getPublishedPoints(ROUTE_ID);

            // Then
            assertThat(points).isNotNull();
            assertThat(points.stops()).containsExactly(stop);
            assertThat(points.vias()).containsExactly(via);
            verify(tripRepository).findRepresentativeTrip(ROUTE_ID);
            verify(stopTimeRepository).findPublishedStops(TRIP_ID);
            verify(viaRepository).findPublishedVias(ROUTE_ID);
        }

        @Test
        @DisplayName("getPublishedPoints: Temsili sefer yoksa durak listesi boş dönmeli, canlı vialar okunmalıdır")
        void shouldReturnEmptyStopsWhenRepresentativeTripNotFound() {
            // Given
            Via via = new Via("S1", "S2", 1, 39.91, 41.26);
            when(tripRepository.findRepresentativeTrip(ROUTE_ID)).thenReturn(Optional.empty());
            when(viaRepository.findPublishedVias(ROUTE_ID)).thenReturn(List.of(via));

            // When
            RoutePoints points = routeService.getPublishedPoints(ROUTE_ID);

            // Then
            assertThat(points.stops()).isEmpty();
            assertThat(points.vias()).containsExactly(via);
            verify(stopTimeRepository, never()).findPublishedStops(anyString());
        }
    }

    @Nested
    @DisplayName("Taslak Nokta Sorgulama (getDraftPoints) Testleri")
    class DraftPointsTests {

        @Test
        @DisplayName("getDraftPoints: Taslak durak sırası, taslak via veya taslak durak konumu yoksa null dönmelidir")
        void shouldReturnNullWhenNoDraftModificationsExist() {
            // Given
            Trip trip = new Trip(TRIP_ID, ROUTE_ID, "S1", "SH1", "Varış", 0);
            when(tripRepository.findRepresentativeTrip(ROUTE_ID)).thenReturn(Optional.of(trip));
            when(stopTimeRepository.findDraftStopTimes(TRIP_ID, USER_ID)).thenReturn(List.of());
            when(viaRepository.findDraftVias(ROUTE_ID, USER_ID)).thenReturn(List.of());
            when(stopTimeRepository.hasDraftStops(TRIP_ID, USER_ID)).thenReturn(false);

            // When
            RoutePoints result = routeService.getDraftPoints(ROUTE_ID, USER_ID);

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("getDraftPoints: Taslak durak sırası ve taslak via varsa ikisini birleştirip dönmelidir")
        void shouldReturnDraftStopsAndDraftViasWhenBothExist() {
            // Given
            Trip trip = new Trip(TRIP_ID, ROUTE_ID, "S1", "SH1", "Varış", 0);
            Stop draftStop = new Stop("S1", "Taslak Durak 1", 39.90, 41.25, 0);
            Via draftVia = new Via("S1", "S2", 1, 39.91, 41.26);

            when(tripRepository.findRepresentativeTrip(ROUTE_ID)).thenReturn(Optional.of(trip));
            when(stopTimeRepository.findDraftStopTimes(TRIP_ID, USER_ID)).thenReturn(List.of(draftStop));
            when(viaRepository.findDraftVias(ROUTE_ID, USER_ID)).thenReturn(List.of(draftVia));
            when(stopTimeRepository.hasDraftStops(TRIP_ID, USER_ID)).thenReturn(false);

            // When
            RoutePoints result = routeService.getDraftPoints(ROUTE_ID, USER_ID);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.stops()).containsExactly(draftStop);
            assertThat(result.vias()).containsExactly(draftVia);
            verify(viaRepository, never()).findPublishedVias(ROUTE_ID);
        }

        @Test
        @DisplayName("getDraftPoints: Taslak durak olup taslak via yoksa canlı vialara fallback yapmalıdır")
        void shouldFallbackToPublishedViasWhenDraftViasEmpty() {
            // Given
            Trip trip = new Trip(TRIP_ID, ROUTE_ID, "S1", "SH1", "Varış", 0);
            Stop draftStop = new Stop("S1", "Taslak Durak 1", 39.90, 41.25, 0);
            Via publishedVia = new Via("S1", "S2", 1, 39.95, 41.35);

            when(tripRepository.findRepresentativeTrip(ROUTE_ID)).thenReturn(Optional.of(trip));
            when(stopTimeRepository.findDraftStopTimes(TRIP_ID, USER_ID)).thenReturn(List.of(draftStop));
            when(viaRepository.findDraftVias(ROUTE_ID, USER_ID)).thenReturn(List.of());
            when(stopTimeRepository.hasDraftStops(TRIP_ID, USER_ID)).thenReturn(false);
            when(viaRepository.findPublishedVias(ROUTE_ID)).thenReturn(List.of(publishedVia));

            // When
            RoutePoints result = routeService.getDraftPoints(ROUTE_ID, USER_ID);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.stops()).containsExactly(draftStop);
            assertThat(result.vias()).containsExactly(publishedVia);
            verify(viaRepository).findPublishedVias(ROUTE_ID);
        }

        @Test
        @DisplayName("getDraftPoints: Taslak durak sırası boş ama taslak durak konumu değişmişse (hasDraftStops=true) findDraftStops çağrılmalıdır")
        void shouldFetchDraftStopsWhenHasDraftStopsIsTrue() {
            // Given
            Trip trip = new Trip(TRIP_ID, ROUTE_ID, "S1", "SH1", "Varış", 0);
            Stop movedStop = new Stop("S1", "Kaydırılan Durak", 39.92, 41.28, 0);

            when(tripRepository.findRepresentativeTrip(ROUTE_ID)).thenReturn(Optional.of(trip));
            when(stopTimeRepository.findDraftStopTimes(TRIP_ID, USER_ID)).thenReturn(List.of());
            when(viaRepository.findDraftVias(ROUTE_ID, USER_ID)).thenReturn(List.of());
            when(stopTimeRepository.hasDraftStops(TRIP_ID, USER_ID)).thenReturn(true);
            when(stopTimeRepository.findDraftStops(TRIP_ID, USER_ID)).thenReturn(List.of(movedStop));
            when(viaRepository.findPublishedVias(ROUTE_ID)).thenReturn(List.of());

            // When
            RoutePoints result = routeService.getDraftPoints(ROUTE_ID, USER_ID);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.stops()).containsExactly(movedStop);
            verify(stopTimeRepository).findDraftStops(TRIP_ID, USER_ID);
        }
    }

    @Nested
    @DisplayName("Taslak Kayıt ve Silme (saveAllDraftPoints & deleteDraftPoints) Testleri")
    class DraftPersistenceTests {

        @Test
        @DisplayName("saveAllDraftPoints: Taslak listesi null veya boş olduğunda işlem yapılmamalıdır (early return)")
        void shouldReturnEarlyWhenDraftListIsNullOrEmpty() {
            // When
            routeService.saveAllDraftPoints(USER_ID, null);
            routeService.saveAllDraftPoints(USER_ID, List.of());

            // Then
            verifyNoInteractions(routeRepository, tripRepository, stopTimeRepository, viaRepository);
        }

        @Test
        @DisplayName("saveAllDraftPoints: Her bir hat taslağı için eski veriler silinmeli ve yeni duraklar ile via'lar batch kaydedilmelidir")
        void shouldDeleteOldDraftsAndBatchInsertNewDrafts() {
            // Given
            Stop stop1 = new Stop("S1", "Durak 1", 39.90, 41.25, 0);
            Via via1 = new Via("S1", "S2", 1, 39.91, 41.26);
            RouteDraftItem draftItem = new RouteDraftItem(ROUTE_ID, List.of(stop1), List.of(via1));

            Trip trip = new Trip(TRIP_ID, ROUTE_ID, "S1", "SH1", "Varış", 0);
            when(tripRepository.findRepresentativeTrip(ROUTE_ID)).thenReturn(Optional.of(trip));

            // When
            routeService.saveAllDraftPoints(USER_ID, List.of(draftItem));

            // Then
            // Önceki taslak silinmeli
            verify(viaRepository).deleteDraftVias(USER_ID, ROUTE_ID);
            verify(stopTimeRepository).deleteDraftStopTimes(USER_ID, TRIP_ID);

            // Yeni taslak batch yazılmalı
            verify(stopTimeRepository).batchInsertDraftStopTimes(USER_ID, TRIP_ID, List.of(stop1));
            verify(viaRepository).batchInsertDraftVias(USER_ID, ROUTE_ID, List.of(via1));
        }

        @Test
        @DisplayName("saveAllDraftPoints: Durak veya via listesi boş olan taslaklarda ilgili batch insert çağrılmamalıdır")
        void shouldNotCallBatchInsertWhenStopsOrViasAreEmpty() {
            // Given
            RouteDraftItem emptyDraftItem = new RouteDraftItem(ROUTE_ID, List.of(), List.of());
            Trip trip = new Trip(TRIP_ID, ROUTE_ID, "S1", "SH1", "Varış", 0);
            when(tripRepository.findRepresentativeTrip(ROUTE_ID)).thenReturn(Optional.of(trip));

            // When
            routeService.saveAllDraftPoints(USER_ID, List.of(emptyDraftItem));

            // Then
            verify(viaRepository).deleteDraftVias(USER_ID, ROUTE_ID);
            verify(stopTimeRepository).deleteDraftStopTimes(USER_ID, TRIP_ID);
            verify(stopTimeRepository, never()).batchInsertDraftStopTimes(any(), any(), any());
            verify(viaRepository, never()).batchInsertDraftVias(any(), any(), any());
        }

        @Test
        @DisplayName("deleteDraftPoints: Sefer mevcut olduğunda hem taslak via'ları hem taslak stop_time'ları silmelidir")
        void shouldDeleteBothDraftViasAndStopTimesWhenTripExists() {
            // Given
            Trip trip = new Trip(TRIP_ID, ROUTE_ID, "S1", "SH1", "Varış", 0);
            when(tripRepository.findRepresentativeTrip(ROUTE_ID)).thenReturn(Optional.of(trip));

            // When
            routeService.deleteDraftPoints(ROUTE_ID, USER_ID);

            // Then
            verify(viaRepository).deleteDraftVias(USER_ID, ROUTE_ID);
            verify(stopTimeRepository).deleteDraftStopTimes(USER_ID, TRIP_ID);
        }

        @Test
        @DisplayName("deleteDraftPoints: Sefer bulunamadığında sadece taslak via'ları silmeli, stop_times silmeye çalışmamalıdır")
        void shouldOnlyDeleteDraftViasWhenTripNotFound() {
            // Given
            when(tripRepository.findRepresentativeTrip(ROUTE_ID)).thenReturn(Optional.empty());

            // When
            routeService.deleteDraftPoints(ROUTE_ID, USER_ID);

            // Then
            verify(viaRepository).deleteDraftVias(USER_ID, ROUTE_ID);
            verify(stopTimeRepository, never()).deleteDraftStopTimes(any(), any());
        }
    }

    @Nested
    @DisplayName("Durak Değişiminden Etkilenen Hat Testleri")
    class AffectedRouteTests {

        @Test
        @DisplayName("replaceAffectedRoutes: Etkilenen hat listesini RouteRepository'ye delege etmelidir")
        void shouldDelegateReplaceAffectedRoutes() {
            // Given
            List<AffectedRoute> affectedRoutes = List.of(
                    new AffectedRoute("R1", List.of("S1", "S2")),
                    new AffectedRoute("R2", List.of("S3"))
            );

            // When
            routeService.replaceAffectedRoutes(USER_ID, affectedRoutes);

            // Then
            verify(routeRepository).replaceAffectedRoutes(USER_ID, affectedRoutes);
        }

        @Test
        @DisplayName("replaceAffectedRoutes: Boş liste de RouteRepository'ye iletilmelidir (mevcut kayıtlar temizlenir)")
        void shouldDelegateEmptyListToClearAffectedRoutes() {
            // When
            routeService.replaceAffectedRoutes(USER_ID, List.of());

            // Then
            verify(routeRepository).replaceAffectedRoutes(USER_ID, List.of());
        }

        @Test
        @DisplayName("getAffectedRoutes: Etkilenen hatları ve etkileyen durakları RouteRepository'den dönmelidir")
        void shouldDelegateGetAffectedRoutes() {
            // Given
            List<AffectedRoute> affectedRoutes = List.of(
                    new AffectedRoute("R1", List.of("S1", "S2")),
                    new AffectedRoute("R2", List.of("S3"))
            );
            when(routeRepository.findAffectedRoutes(USER_ID)).thenReturn(affectedRoutes);

            // When
            List<AffectedRoute> result = routeService.getAffectedRoutes(USER_ID);

            // Then
            assertThat(result).isEqualTo(affectedRoutes);
            verify(routeRepository).findAffectedRoutes(USER_ID);
        }
    }
}
