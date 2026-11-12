package com.demo.transit.application.port;

import com.demo.transit.application.model.Coordinate;
import com.demo.transit.application.model.RouteNavigation;

import java.util.List;

public interface RoutingEngine {
    RouteNavigation calculateNavigation(List<Coordinate> coordinates);
}
