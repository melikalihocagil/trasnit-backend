package com.demo.transit.presentation.controller;

import com.demo.transit.application.RouteService;
import com.demo.transit.application.RoutingService;
import com.demo.transit.application.StopService;
import com.demo.transit.application.model.RouteNavigation;
import com.demo.transit.application.model.RoutePoints;
import com.demo.transit.application.model.Stop;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StopController.class)
@DisplayName("StopController WebMvc API Sözleşme Testleri")
class StopControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private StopService stopService;

    @MockitoBean
    private RouteService routeService;

    @MockitoBean
    private RoutingService routingService;

    private static final String USER_ID = "operator-1";

    @Nested
    @DisplayName("GET /api/stops Sözleşme Testleri")
    class GetStopsTests {

        @Test
        @DisplayName("Taslak durak yokken 200 OK ile published durakları ve null draft dönmelidir")
        void shouldReturnPublishedStopsWithNullDraft() throws Exception {
            // Given
            Stop pubStop = new Stop("S1", "Atatürk Meydanı", 39.90, 41.25, 0);
            when(stopService.getPublishedStops()).thenReturn(List.of(pubStop));
            when(stopService.getDraftStops(USER_ID)).thenReturn(List.of());

            // When & Then
            mockMvc.perform(get("/api/stops")
                            .header("X-User-Id", USER_ID)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.published").isArray())
                    .andExpect(jsonPath("$.published.length()").value(1))
                    .andExpect(jsonPath("$.published[0].stopId").value("S1"))
                    .andExpect(jsonPath("$.published[0].stopName").value("Atatürk Meydanı"))
                    .andExpect(jsonPath("$.published[0].lat").value(39.90))
                    .andExpect(jsonPath("$.published[0].lon").value(41.25))
                    .andExpect(jsonPath("$.draft").doesNotExist());

            verify(stopService).getPublishedStops();
            verify(stopService).getDraftStops(USER_ID);
        }

        @Test
        @DisplayName("Taslak durak mevcutken 200 OK ile hem published hem draft dizisi dönmelidir")
        void shouldReturnBothPublishedAndDraftStops() throws Exception {
            // Given
            Stop pubStop = new Stop("S1", "Canlı Durak", 39.90, 41.25, 0);
            Stop draftStop = new Stop("S1", "Taslak Durak (Taşınmış)", 39.92, 41.27, 0);
            when(stopService.getPublishedStops()).thenReturn(List.of(pubStop));
            when(stopService.getDraftStops(USER_ID)).thenReturn(List.of(draftStop));

            // When & Then
            mockMvc.perform(get("/api/stops")
                            .header("X-User-Id", USER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.published[0].stopName").value("Canlı Durak"))
                    .andExpect(jsonPath("$.draft[0].stopName").value("Taslak Durak (Taşınmış)"));
        }
    }

    @Nested
    @DisplayName("POST /api/stops/move Sözleşme Testleri")
    class MoveStopTests {

        @Test
        @DisplayName("Durak taşındığında veritabanına kayıt yapılmamalı ve sadece geometrisi gerçekten değişen hat ID'leri 200 OK ile dönmelidir")
        void shouldMoveStopAndReturnTrulyAffectedRoutes() throws Exception {
            // Given
            Stop movedStop = new Stop("S1", "Taşınan Durak", 39.95, 41.30, 0);
            Stop pubStop = new Stop("S1", "Orijinal Durak", 39.90, 41.25, 0);
            Stop stop2 = new Stop("S2", "İkinci Durak", 39.96, 41.26, 0);

            when(stopService.findRouteIdsByStopId(USER_ID, "S1")).thenReturn(List.of("R1", "R2"));

            RoutePoints r1Pub = new RoutePoints(List.of(pubStop), List.of());
            RoutePoints r1Draft = new RoutePoints(List.of(movedStop), List.of());
            when(routeService.getPublishedPoints("R1")).thenReturn(r1Pub);
            when(routeService.getDraftPoints("R1", USER_ID)).thenReturn(r1Draft);

            RoutePoints r2Pub = new RoutePoints(List.of(stop2), List.of());
            RoutePoints r2Draft = new RoutePoints(List.of(stop2), List.of());
            when(routeService.getPublishedPoints("R2")).thenReturn(r2Pub);
            when(routeService.getDraftPoints("R2", USER_ID)).thenReturn(r2Draft);

            RouteNavigation nav1Pub = new RouteNavigation(1000.0, 100.0, List.of());
            RouteNavigation nav1Draft = new RouteNavigation(1200.0, 130.0, List.of());
            RouteNavigation nav2 = new RouteNavigation(2000.0, 200.0, List.of());

            when(routingService.calculateNavigation(r1Pub.stops(), r1Pub.vias())).thenReturn(nav1Pub);
            when(routingService.calculateNavigation(r1Draft.stops(), r1Draft.vias())).thenReturn(nav1Draft);
            when(routingService.calculateNavigation(r2Pub.stops(), r2Pub.vias())).thenReturn(nav2);

            when(routingService.hasGeometryChanged(nav1Pub, nav1Draft)).thenReturn(true);
            when(routingService.hasGeometryChanged(nav2, nav2)).thenReturn(false);

            // When & Then
            mockMvc.perform(post("/api/stops/move")
                            .header("X-User-Id", USER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(movedStop)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0]").value("R1"));

            verify(stopService, never()).saveDraftStop(any(), any());
            verify(stopService).findRouteIdsByStopId(USER_ID, "S1");
        }
    }

    @Nested
    @DisplayName("Durak Kayıt ve Silme Sözleşme Testleri")
    class PersistenceAndDeletionTests {

        @Test
        @DisplayName("POST /api/stops/save: Toplu taslak durak kaydı 204 No Content dönmelidir")
        void shouldSaveAllDraftStopsAndReturn204() throws Exception {
            // Given
            Stop s1 = new Stop("S1", "Durak 1", 39.90, 41.25, 0);
            Stop s2 = new Stop("S2", "Durak 2", 39.91, 41.26, 0);
            List<Stop> stops = List.of(s1, s2);

            // When & Then
            mockMvc.perform(post("/api/stops/save")
                            .header("X-User-Id", USER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(stops)))
                    .andExpect(status().isNoContent());

            verify(stopService).saveAllDraftStops(USER_ID, stops);
        }

        @Test
        @DisplayName("DELETE /api/stops/delete/{stopId}: Tekil taslak durak silme 204 No Content dönmelidir")
        void shouldDeleteDraftStopAndReturn204() throws Exception {
            // When & Then
            mockMvc.perform(delete("/api/stops/delete/S1")
                            .header("X-User-Id", USER_ID))
                    .andExpect(status().isNoContent());

            verify(stopService).deleteDraftStop(USER_ID, "S1");
        }

        @Test
        @DisplayName("DELETE /api/stops/delete: Tüm taslak durakları silme 204 No Content dönmelidir")
        void shouldDeleteAllDraftStopsAndReturn204() throws Exception {
            // When & Then
            mockMvc.perform(delete("/api/stops/delete")
                            .header("X-User-Id", USER_ID))
                    .andExpect(status().isNoContent());

            verify(stopService).deleteAllDraftStops(USER_ID);
        }
    }
}
