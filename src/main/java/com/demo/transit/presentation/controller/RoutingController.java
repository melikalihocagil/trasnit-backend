package com.demo.transit.presentation.controller;

import com.demo.transit.application.RoutingService;
import com.demo.transit.application.model.RouteNavigation;
import com.demo.transit.presentation.dto.RoutingRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/routing")
public class RoutingController {

    private final RoutingService routingService;

    public RoutingController(RoutingService routingService) {
        this.routingService = routingService;
    }

    @PostMapping("/calculate")
    public RouteNavigation calculateRoute(@RequestBody RoutingRequest request) {
        if (request == null) {
            return routingService.calculateNavigation(null, null);
        }
        return routingService.calculateNavigation(request.stops(), request.vias());
    }
}
