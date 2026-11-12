package com.demo.transit.application;

import com.demo.transit.application.model.Coordinate;
import com.demo.transit.application.model.RouteNavigation;
import com.demo.transit.application.model.Stop;
import com.demo.transit.application.model.Via;
import com.demo.transit.application.port.RoutingEngine;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class RoutingService {

    private final RoutingEngine routingEngine;

    public RoutingService(RoutingEngine routingEngine) {
        this.routingEngine = routingEngine;
    }

    public RouteNavigation calculateNavigation(List<Stop> stops, List<Via> vias) {
        if (stops == null || stops.size() < 2) {
            return new RouteNavigation(0.0, 0.0, List.of());
        }

        List<Coordinate> coordinates = buildRouteCoordinates(stops, vias);
        if (coordinates.size() < 2) {
            return new RouteNavigation(0.0, 0.0, List.of());
        }

        return routingEngine.calculateNavigation(coordinates);
    }

    public boolean hasGeometryChanged(RouteNavigation published, RouteNavigation draft) {
        if (published == null && draft == null) {
            return false;
        }
        if (published == null || draft == null) {
            return true;
        }

        int publishedPoints = published.legs().stream()
                .filter(l -> l.coordinates() != null)
                .mapToInt(l -> l.coordinates().size())
                .sum();
        int draftPoints = draft.legs().stream()
                .filter(l -> l.coordinates() != null)
                .mapToInt(l -> l.coordinates().size())
                .sum();

        if (publishedPoints != draftPoints) {
            return true;
        }

        if (Math.abs(published.distance() - draft.distance()) > 1.0) {
            return true;
        }

        return !published.equals(draft);
    }

    private List<Coordinate> buildRouteCoordinates(List<Stop> stops, List<Via> vias) {
        List<Coordinate> coordinates = new ArrayList<>();
        for (int i = 0; i < stops.size(); i++) {
            Stop stop = stops.get(i);
            coordinates.add(new Coordinate(stop.lat(), stop.lon()));

            if (i < stops.size() - 1) {
                Stop nextStop = stops.get(i + 1);
                if (vias != null) {
                    List<Via> segmentVias = vias.stream()
                            .filter(v -> stop.stopId().equals(v.fromStopId()) && nextStop.stopId().equals(v.toStopId()))
                            .sorted(Comparator.comparingInt(Via::viaSequence))
                            .toList();
                    for (Via via : segmentVias) {
                        coordinates.add(new Coordinate(via.lat(), via.lon()));
                    }
                }
            }
        }
        return coordinates;
    }
}
