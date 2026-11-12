package com.demo.transit.presentation.controller;

import com.demo.transit.application.RoutingService;
import com.demo.transit.application.model.Coordinate;
import com.demo.transit.application.model.Leg;
import com.demo.transit.application.model.RouteNavigation;
import com.demo.transit.application.model.Stop;
import com.demo.transit.application.model.Via;
import com.demo.transit.presentation.dto.RoutingRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RoutingController.class)
@DisplayName("RoutingController WebMvc API Sözleşme Testleri")
class RoutingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private RoutingService routingService;

    @Test
    @DisplayName("POST /api/routing/calculate: Duraklar ve via'lar ile rota hesaplandığında 200 OK ve geçerli RouteNavigation JSON dönmelidir")
    void shouldCalculateRouteSuccessfully() throws Exception {
        // Given
        Stop stop1 = new Stop("S1", "Durak 1", 39.90, 41.25, 0);
        Stop stop2 = new Stop("S2", "Durak 2", 39.92, 41.27, 0);
        Via via1 = new Via("S1", "S2", 1, 39.91, 41.26);
        RoutingRequest request = new RoutingRequest(List.of(stop1, stop2), List.of(via1));

        RouteNavigation expectedResponse = new RouteNavigation(
                1250.5,
                195.0,
                List.of(new Leg(List.of(new Coordinate(39.90, 41.25), new Coordinate(39.91, 41.26), new Coordinate(39.92, 41.27)), 1250.5, 195.0))
        );

        when(routingService.calculateNavigation(request.stops(), request.vias())).thenReturn(expectedResponse);

        // When & Then
        mockMvc.perform(post("/api/routing/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.distance").value(1250.5))
                .andExpect(jsonPath("$.duration").value(195.0))
                .andExpect(jsonPath("$.legs").isArray())
                .andExpect(jsonPath("$.legs.length()").value(1))
                .andExpect(jsonPath("$.legs[0].distance").value(1250.5))
                .andExpect(jsonPath("$.legs[0].duration").value(195.0))
                .andExpect(jsonPath("$.legs[0].coordinates.length()").value(3));

        verify(routingService).calculateNavigation(request.stops(), request.vias());
    }

    @Test
    @DisplayName("POST /api/routing/calculate: Boş istek gövdesi gönderildiğinde 200 OK ve sıfırlanmış navigasyon dönmelidir")
    void shouldReturnEmptyNavigationWhenRequestBodyIsEmpty() throws Exception {
        // Given
        RoutingRequest emptyRequest = new RoutingRequest(List.of(), List.of());
        RouteNavigation emptyResponse = new RouteNavigation(0.0, 0.0, List.of());

        when(routingService.calculateNavigation(emptyRequest.stops(), emptyRequest.vias())).thenReturn(emptyResponse);

        // When & Then
        mockMvc.perform(post("/api/routing/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emptyRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.distance").value(0.0))
                .andExpect(jsonPath("$.duration").value(0.0))
                .andExpect(jsonPath("$.legs").isEmpty());
    }
}
