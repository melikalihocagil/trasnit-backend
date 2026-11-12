package com.demo.transit.presentation.controller;

import com.demo.transit.application.RouteService;
import com.demo.transit.application.RoutingService;
import com.demo.transit.application.model.RouteDraftItem;
import com.demo.transit.application.model.RouteNavigation;
import com.demo.transit.application.model.RoutePoints;
import com.demo.transit.presentation.dto.RouteDetailResponse;
import com.demo.transit.presentation.dto.RouteDataResponse;
import com.demo.transit.presentation.dto.RouteDraftRequest;
import com.demo.transit.presentation.dto.RouteResponse;
import com.demo.transit.presentation.dto.RouteReviewItem;
import com.demo.transit.presentation.mapper.TransitDtoMapper;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/routes")
public class RouteController {

    private final RouteService routeService;
    private final RoutingService routingService;
    private final TransitDtoMapper mapper;

    public RouteController(RouteService routeService, RoutingService routingService, TransitDtoMapper mapper) {
        this.routeService = routeService;
        this.routingService = routingService;
        this.mapper = mapper;
    }

    @GetMapping
    public List<RouteResponse> getAllRoutes(
            @RequestHeader(value = "X-User-Id", defaultValue = "operator-1") String userId
    ) {
        return routeService.getRoutes(userId);
    }

    @GetMapping("/{routeId}")
    public RouteDataResponse getRouteData(
            @PathVariable String routeId,
            @RequestHeader(value = "X-User-Id", defaultValue = "operator-1") String userId
    ) {
        RoutePoints publishedPoints = routeService.getPublishedPoints(routeId);
        RouteNavigation publishedNavigation = routingService.calculateNavigation(
                publishedPoints.stops(),
                publishedPoints.vias()
        );
        RouteDetailResponse published = mapper.toRouteDetailResponse(publishedPoints, publishedNavigation);

        RoutePoints draftPoints = routeService.getDraftPoints(routeId, userId);
        RouteDetailResponse draft = draftPoints == null ? null : mapper.toRouteDetailResponse(
                draftPoints,
                routingService.calculateNavigation(draftPoints.stops(), draftPoints.vias())
        );

        return new RouteDataResponse(routeId, published, draft);
    }

    @PostMapping("/save")
    public List<RouteDataResponse> saveAllRouteDrafts(
            @RequestBody List<RouteDraftRequest> routes,
            @RequestHeader(value = "X-User-Id", defaultValue = "operator-1") String userId
    ) {
        if (routes == null) {
            return List.of();
        }
        List<RouteDraftItem> domainItems = routes.stream()
                .map(r -> new RouteDraftItem(
                        r.routeId(),
                        r.stops() == null ? List.of() : r.stops(),
                        r.vias() == null ? List.of() : r.vias()
                ))
                .toList();

        routeService.saveAllDraftPoints(userId, domainItems);

        return routes.stream()
                .map(r -> getRouteData(r.routeId(), userId))
                .toList();
    }

    @DeleteMapping("/delete/{routeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRouteDraft(
            @PathVariable String routeId,
            @RequestHeader(value = "X-User-Id", defaultValue = "operator-1") String userId
    ) {
        routeService.deleteDraftPoints(routeId, userId);
    }

    @PutMapping("/reviews")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateRouteReviews(
            @RequestBody List<RouteReviewItem> reviews,
            @RequestHeader(value = "X-User-Id", defaultValue = "operator-1") String userId
    ) {
        routeService.saveRouteReviews(userId, reviews);
    }

    @GetMapping("/reviews")
    public List<String> getReviewedRouteIds(
            @RequestHeader(value = "X-User-Id", defaultValue = "operator-1") String userId
    ) {
        return routeService.getReviewedRouteIds(userId);
    }
}
