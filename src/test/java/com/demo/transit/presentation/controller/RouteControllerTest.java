package com.demo.transit.presentation.controller;

import com.demo.transit.application.RouteService;
import com.demo.transit.application.RoutingService;
import com.demo.transit.application.model.Coordinate;
import com.demo.transit.application.model.Leg;
import com.demo.transit.application.model.RouteDraftItem;
import com.demo.transit.application.model.RouteNavigation;
import com.demo.transit.application.model.RoutePoints;
import com.demo.transit.application.model.Stop;
import com.demo.transit.application.model.Via;
import com.demo.transit.presentation.dto.RouteDraftRequest;
import com.demo.transit.presentation.dto.RouteResponse;
import com.demo.transit.presentation.dto.RouteReviewItem;
import com.demo.transit.presentation.mapper.TransitDtoMapperImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RouteController.class)
@Import(TransitDtoMapperImpl.class)
@DisplayName("RouteController WebMvc API Sözleşme Testleri")
class RouteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private RouteService routeService;

    @MockitoBean
    private RoutingService routingService;

    private static final String USER_ID = "operator-1";
    private static final String ROUTE_ID = "R1";

    @Nested
    @DisplayName("GET /api/routes Sözleşme Testleri")
    class GetRoutesTests {

        @Test
        @DisplayName("X-User-Id header'ı ile çağrıldığında 200 OK ve hat listesi JSON dizisi dönmelidir")
        void shouldReturnRoutesWithCustomUserHeader() throws Exception {
            // Given
            RouteResponse route = new RouteResponse("R1", "1A", "Atatürk - Çarşı", "#FF0000", "#FFFFFF", 0, "Çarşı Yönü");
            when(routeService.getRoutes(USER_ID)).thenReturn(List.of(route));

            // When & Then
            mockMvc.perform(get("/api/routes")
                            .header("X-User-Id", USER_ID)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].routeId").value("R1"))
                    .andExpect(jsonPath("$[0].routeShortName").value("1A"))
                    .andExpect(jsonPath("$[0].routeLongName").value("Atatürk - Çarşı"))
                    .andExpect(jsonPath("$[0].routeColor").value("#FF0000"))
                    .andExpect(jsonPath("$[0].routeTextColor").value("#FFFFFF"))
                    .andExpect(jsonPath("$[0].directionId").value(0))
                    .andExpect(jsonPath("$[0].tripHeadsign").value("Çarşı Yönü"));

            verify(routeService).getRoutes(USER_ID);
        }

        @Test
        @DisplayName("X-User-Id header'ı verilmediğinde varsayılan 'operator-1' kullanılarak 200 OK dönmelidir")
        void shouldFallbackToDefaultUserHeaderWhenMissing() throws Exception {
            // Given
            when(routeService.getRoutes("operator-1")).thenReturn(List.of());

            // When & Then
            mockMvc.perform(get("/api/routes"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(0));

            verify(routeService).getRoutes("operator-1");
        }
    }

    @Nested
    @DisplayName("GET /api/routes/{routeId} Sözleşme Testleri")
    class GetRouteDataTests {

        @Test
        @DisplayName("Taslak veri yokken 200 OK ile sadece published detayını ve null draft dönmelidir")
        void shouldReturnRouteDataWithPublishedOnlyWhenDraftIsNull() throws Exception {
            // Given
            Stop s1 = new Stop("S1", "Durak 1", 39.90, 41.25, 0);
            Stop s2 = new Stop("S2", "Durak 2", 39.92, 41.27, 0);
            Via v1 = new Via("S1", "S2", 1, 39.91, 41.26);
            RoutePoints publishedPoints = new RoutePoints(List.of(s1, s2), List.of(v1));

            when(routeService.getPublishedPoints(ROUTE_ID)).thenReturn(publishedPoints);
            when(routeService.getDraftPoints(ROUTE_ID, USER_ID)).thenReturn(null);

            Leg leg = new Leg(List.of(new Coordinate(39.90, 41.25), new Coordinate(39.92, 41.27)), 1500.0, 180.0);
            RouteNavigation nav = new RouteNavigation(1500.0, 180.0, List.of(leg));
            when(routingService.calculateNavigation(publishedPoints.stops(), publishedPoints.vias())).thenReturn(nav);

            // When & Then
            mockMvc.perform(get("/api/routes/{routeId}", ROUTE_ID)
                            .header("X-User-Id", USER_ID))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.routeId").value("R1"))
                    .andExpect(jsonPath("$.published").isMap())
                    .andExpect(jsonPath("$.published.stops.length()").value(2))
                    .andExpect(jsonPath("$.published.vias.length()").value(1))
                    .andExpect(jsonPath("$.published.navigation.distance").value(1500.0))
                    .andExpect(jsonPath("$.published.navigation.duration").value(180.0))
                    .andExpect(jsonPath("$.published.navigation.legs.length()").value(1))
                    .andExpect(jsonPath("$.draft").doesNotExist());
        }

        @Test
        @DisplayName("Taslak veri varken 200 OK ile hem published hem draft RouteDetail nesnesi dönmelidir")
        void shouldReturnRouteDataWithBothPublishedAndDraft() throws Exception {
            // Given
            Stop s1 = new Stop("S1", "Durak 1", 39.90, 41.25, 0);
            Stop s2 = new Stop("S2", "Durak 2", 39.92, 41.27, 0);
            RoutePoints publishedPoints = new RoutePoints(List.of(s1, s2), List.of());
            RoutePoints draftPoints = new RoutePoints(List.of(s1, s2), List.of(new Via("S1", "S2", 1, 39.91, 41.26)));

            when(routeService.getPublishedPoints(ROUTE_ID)).thenReturn(publishedPoints);
            when(routeService.getDraftPoints(ROUTE_ID, USER_ID)).thenReturn(draftPoints);

            when(routingService.calculateNavigation(publishedPoints.stops(), publishedPoints.vias()))
                    .thenReturn(new RouteNavigation(1500.0, 180.0, List.of()));
            when(routingService.calculateNavigation(draftPoints.stops(), draftPoints.vias()))
                    .thenReturn(new RouteNavigation(1600.0, 195.0, List.of()));

            // When & Then
            mockMvc.perform(get("/api/routes/{routeId}", ROUTE_ID)
                            .header("X-User-Id", USER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.routeId").value("R1"))
                    .andExpect(jsonPath("$.published.navigation.distance").value(1500.0))
                    .andExpect(jsonPath("$.draft.navigation.distance").value(1600.0))
                    .andExpect(jsonPath("$.draft.vias.length()").value(1));
        }
    }

    @Nested
    @DisplayName("Taslak Kayıt ve Silme Sözleşme Testleri")
    class PersistenceAndDeletionTests {

        @Test
        @DisplayName("POST /api/routes/save: Toplu hat taslağı kaydedildiğinde 200 OK ile güncel RouteDataResponse listesi dönmelidir")
        void shouldSaveAllRouteDraftsAndReturnUpdatedData() throws Exception {
            // Given
            Stop stop = new Stop("S1", "Durak 1", 39.90, 41.25, 0);
            Via via = new Via("S1", "S2", 1, 39.91, 41.26);
            RouteDraftRequest requestItem = new RouteDraftRequest("R1", List.of(stop), List.of(via));

            RoutePoints points = new RoutePoints(List.of(stop), List.of(via));
            when(routeService.getPublishedPoints("R1")).thenReturn(points);
            when(routeService.getDraftPoints("R1", USER_ID)).thenReturn(points);
            when(routingService.calculateNavigation(anyList(), anyList())).thenReturn(
                    new RouteNavigation(1500.0, 180.0, List.of())
            );

            // When & Then
            mockMvc.perform(post("/api/routes/save")
                            .header("X-User-Id", USER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(List.of(requestItem))))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].routeId").value("R1"))
                    .andExpect(jsonPath("$[0].published").exists())
                    .andExpect(jsonPath("$[0].draft").exists());

            verify(routeService).saveAllDraftPoints(USER_ID, List.of(new RouteDraftItem("R1", List.of(stop), List.of(via))));
        }

        @Test
        @DisplayName("DELETE /api/routes/delete/{routeId}: Hat taslağı silme 204 No Content dönmelidir")
        void shouldDeleteRouteDraftAndReturn204() throws Exception {
            // When & Then
            mockMvc.perform(delete("/api/routes/delete/{routeId}", ROUTE_ID)
                            .header("X-User-Id", USER_ID))
                    .andExpect(status().isNoContent());

            verify(routeService).deleteDraftPoints(ROUTE_ID, USER_ID);
        }
    }

    @Nested
    @DisplayName("Hat İnceleme (Reviews) Sözleşme Testleri")
    class RouteReviewTests {

        @Test
        @DisplayName("PUT /api/routes/reviews: İnceleme kayıt isteği 204 No Content dönmelidir")
        void shouldUpdateRouteReviewsAndReturn204() throws Exception {
            // Given
            RouteReviewItem review = new RouteReviewItem("R1", true);

            // When & Then
            mockMvc.perform(put("/api/routes/reviews")
                            .header("X-User-Id", USER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(List.of(review))))
                    .andExpect(status().isNoContent());

            verify(routeService).saveRouteReviews(USER_ID, List.of(review));
        }

        @Test
        @DisplayName("GET /api/routes/reviews: İncelenen hat ID'leri 200 OK ve JSON dizisi olarak dönmelidir")
        void shouldGetReviewedRouteIdsAndReturn200() throws Exception {
            // Given
            when(routeService.getReviewedRouteIds(USER_ID)).thenReturn(List.of("R1", "R2"));

            // When & Then
            mockMvc.perform(get("/api/routes/reviews")
                            .header("X-User-Id", USER_ID))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0]").value("R1"))
                    .andExpect(jsonPath("$[1]").value("R2"));

            verify(routeService).getReviewedRouteIds(USER_ID);
        }
    }
}
