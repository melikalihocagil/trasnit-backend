package com.demo.transit.presentation.controller;

import com.demo.transit.application.RouteService;
import com.demo.transit.application.model.Route;
import com.demo.transit.presentation.dto.RouteInfoResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Hat bilgisi (ad, tür, renk) taslağı. Taslak, yayınlandığında canlı hat listesinin
 * yerine geçecek tam listedir; bu yüzden toplu okunur ve toplu kaydedilir.
 */
@RestController
@RequestMapping("/api/route-info")
public class RouteInfoController {

    private final RouteService routeService;

    public RouteInfoController(RouteService routeService) {
        this.routeService = routeService;
    }

    @GetMapping
    public RouteInfoResponse getRouteInfos(
            @RequestHeader(value = "X-User-Id", defaultValue = "operator-1") String userId
    ) {
        List<Route> published = routeService.getPublishedRouteInfos(userId);
        List<Route> draftRoutes = routeService.getDraftRouteInfos(userId);
        List<Route> draft = draftRoutes.isEmpty() ? null : draftRoutes;
        return new RouteInfoResponse(published, draft);
    }

    @PostMapping("/save")
    public RouteInfoResponse saveAllRouteInfos(
            @RequestBody List<Route> routes,
            @RequestHeader(value = "X-User-Id", defaultValue = "operator-1") String userId
    ) {
        routeService.saveAllDraftRouteInfos(userId, routes);
        return getRouteInfos(userId);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleInvalidRouteInfo(IllegalArgumentException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }
}
