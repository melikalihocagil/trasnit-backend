package com.demo.transit.presentation.controller;

import com.demo.transit.application.RouteService;
import com.demo.transit.application.model.Route;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RouteInfoController.class)
@DisplayName("RouteInfoController WebMvc API Sözleşme Testleri")
class RouteInfoControllerTest {

    private static final String USER_ID = "operator-1";

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private RouteService routeService;

    private final Route r1 = new Route("R1", "A1", "1A", "Atatürk - Çarşı", 3, "FF0000", "FFFFFF");
    private final Route r1Edited = new Route("R1", "A1", "1A", "Atatürk - Yakutiye", 3, "FF0000", "FFFFFF");

    @Test
    @DisplayName("GET /api/route-info: Taslak yokken published listesi ve null draft dönmelidir (varsayılan kullanıcı)")
    void shouldReturnPublishedOnlyWhenNoDraft() throws Exception {
        when(routeService.getPublishedRouteInfos(USER_ID)).thenReturn(List.of(r1));
        when(routeService.getDraftRouteInfos(USER_ID)).thenReturn(List.of());

        mockMvc.perform(get("/api/route-info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.published.length()").value(1))
                .andExpect(jsonPath("$.published[0].routeId").value("R1"))
                .andExpect(jsonPath("$.published[0].routeType").value(3))
                .andExpect(jsonPath("$.draft").doesNotExist());
    }

    @Test
    @DisplayName("GET /api/route-info: Taslak varken hem published hem draft listesi dönmelidir")
    void shouldReturnPublishedAndDraft() throws Exception {
        when(routeService.getPublishedRouteInfos("operator-2")).thenReturn(List.of(r1));
        when(routeService.getDraftRouteInfos("operator-2")).thenReturn(List.of(r1Edited));

        mockMvc.perform(get("/api/route-info").header("X-User-Id", "operator-2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.published[0].routeLongName").value("Atatürk - Çarşı"))
                .andExpect(jsonPath("$.draft.length()").value(1))
                .andExpect(jsonPath("$.draft[0].routeLongName").value("Atatürk - Yakutiye"));
    }

    @Test
    @DisplayName("POST /api/route-info/save: Tüm hat listesi servise iletilmeli ve güncel durum dönmelidir")
    void shouldSaveAllAndReturnCurrentState() throws Exception {
        when(routeService.getPublishedRouteInfos(USER_ID)).thenReturn(List.of(r1));
        when(routeService.getDraftRouteInfos(USER_ID)).thenReturn(List.of(r1Edited));

        mockMvc.perform(post("/api/route-info/save")
                        .header("X-User-Id", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(r1Edited))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.published.length()").value(1))
                .andExpect(jsonPath("$.draft[0].routeLongName").value("Atatürk - Yakutiye"));

        verify(routeService).saveAllDraftRouteInfos(USER_ID, List.of(r1Edited));
    }

    @Test
    @DisplayName("POST /api/route-info/save: Doğrulama hatasında 400 Bad Request dönmelidir")
    void shouldReturnBadRequestOnValidationError() throws Exception {
        doThrow(new IllegalArgumentException("Hat ID'si birden fazla kez gönderildi: R1"))
                .when(routeService).saveAllDraftRouteInfos(eq(USER_ID), anyList());

        mockMvc.perform(post("/api/route-info/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(r1, r1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Hat ID'si birden fazla kez gönderildi: R1"));
    }
}
