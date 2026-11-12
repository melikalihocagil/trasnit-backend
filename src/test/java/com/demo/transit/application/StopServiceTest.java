package com.demo.transit.application;

import com.demo.transit.application.model.Stop;
import com.demo.transit.application.port.StopRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("StopService Birim Testleri")
class StopServiceTest {

    @Mock
    private StopRepository stopRepository;

    @InjectMocks
    private StopService stopService;

    private static final String USER_ID = "operator-1";

    @Nested
    @DisplayName("Durak Sorgulama (Query) Testleri")
    class QueryTests {

        @Test
        @DisplayName("getPublishedStops: Canlı şemadaki tüm durakları repository'den dönmelidir")
        void shouldReturnPublishedStops() {
            // Given
            Stop stop1 = new Stop("S1", "Durak 1", 39.90, 41.25, 0);
            Stop stop2 = new Stop("S2", "Durak 2", 39.91, 41.26, 0);
            when(stopRepository.findAllPublished()).thenReturn(List.of(stop1, stop2));

            // When
            List<Stop> result = stopService.getPublishedStops();

            // Then
            assertThat(result).containsExactly(stop1, stop2);
            verify(stopRepository).findAllPublished();
        }

        @Test
        @DisplayName("getDraftStops: Kullanıcıya ait taslak durakları repository'den dönmelidir")
        void shouldReturnDraftStopsForUser() {
            // Given
            Stop draftStop = new Stop("S1", "Taslak Durak 1", 39.92, 41.27, 0);
            when(stopRepository.findAllDraft(USER_ID)).thenReturn(List.of(draftStop));

            // When
            List<Stop> result = stopService.getDraftStops(USER_ID);

            // Then
            assertThat(result).containsExactly(draftStop);
            verify(stopRepository).findAllDraft(USER_ID);
        }

        @Test
        @DisplayName("findRouteIdsByStopId: Belirli bir durağı kullanan hat ID'lerini dönmelidir")
        void shouldReturnRouteIdsBySingleStopId() {
            // Given
            when(stopRepository.findRouteIdsByStopId("S1", USER_ID)).thenReturn(List.of("R1", "R2"));

            // When
            List<String> result = stopService.findRouteIdsByStopId(USER_ID, "S1");

            // Then
            assertThat(result).containsExactly("R1", "R2");
            verify(stopRepository).findRouteIdsByStopId("S1", USER_ID);
        }

        @Test
        @DisplayName("findRouteIdsByStopIds: Birden fazla durağı kullanan tüm tekil hat ID'lerini dönmelidir")
        void shouldReturnRouteIdsByMultipleStopIds() {
            // Given
            List<String> stopIds = List.of("S1", "S2");
            when(stopRepository.findRouteIdsByStopIds(stopIds, USER_ID)).thenReturn(List.of("R1", "R2", "R3"));

            // When
            List<String> result = stopService.findRouteIdsByStopIds(USER_ID, stopIds);

            // Then
            assertThat(result).containsExactly("R1", "R2", "R3");
            verify(stopRepository).findRouteIdsByStopIds(stopIds, USER_ID);
        }
    }

    @Nested
    @DisplayName("Durak Kayıt (Persistence) Testleri")
    class PersistenceTests {

        @Test
        @DisplayName("saveDraftStop: Tek bir taslak durağı repository'ye kaydetmelidir")
        void shouldSaveSingleDraftStop() {
            // Given
            Stop stop = new Stop("S1", "Yeni Durak", 39.95, 41.30, 0);

            // When
            stopService.saveDraftStop(USER_ID, stop);

            // Then
            verify(stopRepository).saveDraftStop(USER_ID, stop);
        }

        @Test
        @DisplayName("saveAllDraftStops: Liste dolu olduğunda tüm taslak durakları repository'ye toplu kaydetmelidir")
        void shouldSaveAllDraftStopsWhenListNotEmpty() {
            // Given
            Stop stop1 = new Stop("S1", "Durak 1", 39.90, 41.25, 0);
            Stop stop2 = new Stop("S2", "Durak 2", 39.91, 41.26, 0);
            List<Stop> stops = List.of(stop1, stop2);

            // When
            stopService.saveAllDraftStops(USER_ID, stops);

            // Then
            verify(stopRepository).saveAllDraftStops(USER_ID, stops);
        }

        @Test
        @DisplayName("saveAllDraftStops: Liste null olduğunda repository çağrılmamalıdır (early return)")
        void shouldNotCallRepositoryWhenStopsListIsNull() {
            // When
            stopService.saveAllDraftStops(USER_ID, null);

            // Then
            verify(stopRepository, never()).saveAllDraftStops(USER_ID, null);
            verifyNoMoreInteractions(stopRepository);
        }

        @Test
        @DisplayName("saveAllDraftStops: Liste boş olduğunda repository çağrılmamalıdır (early return)")
        void shouldNotCallRepositoryWhenStopsListIsEmpty() {
            // When
            stopService.saveAllDraftStops(USER_ID, List.of());

            // Then
            verify(stopRepository, never()).saveAllDraftStops(USER_ID, List.of());
            verifyNoMoreInteractions(stopRepository);
        }
    }

    @Nested
    @DisplayName("Durak Silme (Deletion) Testleri")
    class DeletionTests {

        @Test
        @DisplayName("deleteDraftStop: Belirli bir durağın taslağını silmelidir")
        void shouldDeleteDraftStopById() {
            // When
            stopService.deleteDraftStop(USER_ID, "S1");

            // Then
            verify(stopRepository).deleteDraftStop(USER_ID, "S1");
        }

        @Test
        @DisplayName("deleteAllDraftStops: Kullanıcının tüm taslak duraklarını silmelidir")
        void shouldDeleteAllDraftStopsForUser() {
            // When
            stopService.deleteAllDraftStops(USER_ID);

            // Then
            verify(stopRepository).deleteAllDraftStops(USER_ID);
        }
    }
}
