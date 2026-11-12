package com.demo.transit.presentation.controller;

import com.demo.transit.application.RouteService;
import com.demo.transit.application.RoutingService;
import com.demo.transit.application.StopService;
import com.demo.transit.application.model.RouteNavigation;
import com.demo.transit.application.model.RoutePoints;
import com.demo.transit.application.model.Stop;
import com.demo.transit.presentation.dto.StopsResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/stops")
public class StopController {

    private final StopService stopService;
    private final RouteService routeService;
    private final RoutingService routingService;

    public StopController(StopService stopService, RouteService routeService, RoutingService routingService) {
        this.stopService = stopService;
        this.routeService = routeService;
        this.routingService = routingService;
    }

    @GetMapping
    public StopsResponse getStops(
            @RequestHeader(value = "X-User-Id", defaultValue = "operator-1") String userId
    ) {
        List<Stop> published = stopService.getPublishedStops();
        List<Stop> draftStops = stopService.getDraftStops(userId);
        List<Stop> draft = draftStops.isEmpty() ? null : draftStops;
        return new StopsResponse(published, draft);
    }

    @PostMapping("/move")
    public List<String> moveStop(
            @RequestBody Stop stop,
            @RequestHeader(value = "X-User-Id", defaultValue = "operator-1") String userId
    ) {
        if (stop == null) {
            return List.of();
        }
        List<String> candidateRouteIds = stopService.findRouteIdsByStopId(userId, stop.stopId());
        if (candidateRouteIds.isEmpty()) {
            return List.of();
        }

        return candidateRouteIds.stream()
                .filter(routeId -> {
                    RoutePoints publishedPoints = routeService.getPublishedPoints(routeId);
                    RoutePoints draftPoints = routeService.getDraftPoints(routeId, userId);
                    if (draftPoints == null) {
                        return false;
                    }

                    RouteNavigation publishedNav = routingService.calculateNavigation(
                            publishedPoints.stops(),
                            publishedPoints.vias()
                    );
                    RouteNavigation draftNav = routingService.calculateNavigation(
                            draftPoints.stops(),
                            draftPoints.vias()
                    );

                    return routingService.hasGeometryChanged(publishedNav, draftNav);
                })
                .toList();
    }

    @PostMapping("/save")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void saveAllDraftStops(
            @RequestBody List<Stop> stops,
            @RequestHeader(value = "X-User-Id", defaultValue = "operator-1") String userId
    ) {
        if (stops != null && !stops.isEmpty()) {
            stopService.saveAllDraftStops(userId, stops);
        }
    }

    @DeleteMapping("/delete/{stopId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDraftStop(
            @PathVariable String stopId,
            @RequestHeader(value = "X-User-Id", defaultValue = "operator-1") String userId
    ) {
        stopService.deleteDraftStop(userId, stopId);
    }

    @DeleteMapping("/delete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAllDraftStops(
            @RequestHeader(value = "X-User-Id", defaultValue = "operator-1") String userId
    ) {
        stopService.deleteAllDraftStops(userId);
    }
}
