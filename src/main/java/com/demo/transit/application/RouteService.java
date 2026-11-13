package com.demo.transit.application;

import com.demo.transit.application.model.Route;
import com.demo.transit.application.model.AffectedRoute;
import com.demo.transit.application.model.RouteDraftItem;
import com.demo.transit.application.model.RoutePoints;
import com.demo.transit.application.model.Stop;
import com.demo.transit.application.model.Trip;
import com.demo.transit.application.model.Via;
import com.demo.transit.application.port.RouteRepository;
import com.demo.transit.application.port.StopTimeRepository;
import com.demo.transit.application.port.TripRepository;
import com.demo.transit.application.port.ViaRepository;
import com.demo.transit.presentation.dto.RouteResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class RouteService {

    private final RouteRepository routeRepository;
    private final TripRepository tripRepository;
    private final StopTimeRepository stopTimeRepository;
    private final ViaRepository viaRepository;

    public RouteService(
            RouteRepository routeRepository,
            TripRepository tripRepository,
            StopTimeRepository stopTimeRepository,
            ViaRepository viaRepository
    ) {
        this.routeRepository = routeRepository;
        this.tripRepository = tripRepository;
        this.stopTimeRepository = stopTimeRepository;
        this.viaRepository = viaRepository;
    }

    public List<RouteResponse> getRoutes(String userId) {
        List<Route> routes = routeRepository.findAll(userId);
        return routes.stream()
                .map(r -> {
                    Optional<Trip> repTrip = tripRepository.findRepresentativeTrip(r.routeId());
                    int directionId = repTrip.map(Trip::directionId).orElse(0);
                    String tripHeadsign = repTrip.map(Trip::tripHeadsign).orElse("");
                    return new RouteResponse(
                            r.routeId(),
                            r.routeShortName(),
                            r.routeLongName(),
                            r.routeColor(),
                            r.routeTextColor(),
                            directionId,
                            tripHeadsign
                    );
                })
                .toList();
    }

    public Route getRoute(String routeId) {
        return routeRepository.findById(routeId)
                .orElseThrow(() -> new NoSuchElementException("Route not found: " + routeId));
    }

    public RoutePoints getPublishedPoints(String routeId) {
        List<Stop> stops = tripRepository.findRepresentativeTrip(routeId)
                .map(Trip::tripId)
                .map(stopTimeRepository::findPublishedStops)
                .orElseGet(List::of);
        List<Via> vias = viaRepository.findPublishedVias(routeId);
        return new RoutePoints(stops, vias);
    }

    public RoutePoints getDraftPoints(String routeId, String userId) {
        Optional<String> tripIdOpt = tripRepository.findRepresentativeTrip(routeId).map(Trip::tripId);
        List<Stop> draftSequenceStops = tripIdOpt
                .map(tripId -> stopTimeRepository.findDraftStopTimes(tripId, userId))
                .orElseGet(List::of);
        List<Via> draftVias = viaRepository.findDraftVias(routeId, userId);
        boolean hasDraftStopLocations = tripIdOpt
                .map(tripId -> stopTimeRepository.hasDraftStops(tripId, userId))
                .orElse(false);

        if (draftSequenceStops.isEmpty() && draftVias.isEmpty() && !hasDraftStopLocations) {
            return null;
        }

        List<Stop> stops;
        if (!draftSequenceStops.isEmpty()) {
            stops = draftSequenceStops;
        } else {
            stops = tripIdOpt
                    .map(tripId -> stopTimeRepository.findDraftStops(tripId, userId))
                    .orElseGet(List::of);
        }

        List<Via> vias = !draftVias.isEmpty()
                ? draftVias
                : viaRepository.findPublishedVias(routeId);

        return new RoutePoints(stops, vias);
    }

    @Transactional
    public void saveAllDraftPoints(String userId, List<RouteDraftItem> routeDrafts) {
        if (routeDrafts == null || routeDrafts.isEmpty()) {
            return;
        }
        for (RouteDraftItem item : routeDrafts) {
            deleteDraftPoints(item.routeId(), userId);

            if (item.stops() != null && !item.stops().isEmpty()) {
                tripRepository.findRepresentativeTrip(item.routeId()).ifPresent(trip ->
                        stopTimeRepository.batchInsertDraftStopTimes(userId, trip.tripId(), item.stops())
                );
            }

            if (item.vias() != null && !item.vias().isEmpty()) {
                viaRepository.batchInsertDraftVias(userId, item.routeId(), item.vias());
            }
        }
    }

    @Transactional
    public void deleteDraftPoints(String routeId, String userId) {
        viaRepository.deleteDraftVias(userId, routeId);
        tripRepository.findRepresentativeTrip(routeId).ifPresent(trip ->
                stopTimeRepository.deleteDraftStopTimes(userId, trip.tripId())
        );
    }

    @Transactional
    public void replaceAffectedRoutes(String userId, List<AffectedRoute> affectedRoutes) {
        routeRepository.replaceAffectedRoutes(userId, affectedRoutes);
    }

    public List<AffectedRoute> getAffectedRoutes(String userId) {
        return routeRepository.findAffectedRoutes(userId);
    }

    public List<Route> getPublishedRouteInfos(String userId) {
        return routeRepository.findAll(userId);
    }

    public List<Route> getDraftRouteInfos(String userId) {
        return routeRepository.findAllDraft(userId);
    }

    /**
     * Kullanıcının hat bilgisi taslağını, doğrudan yayınlanabilecek tam hat listesiyle değiştirir.
     * Listede olmayan canlı hat silinmiş, ID'si boş gelen hat yeni hat kabul edilir.
     */
    @Transactional
    public void saveAllDraftRouteInfos(String userId, List<Route> routes) {
        List<Route> normalized = new ArrayList<>();
        Set<String> routeIds = new HashSet<>();
        if (routes != null) {
            for (Route route : routes) {
                if (route == null) {
                    continue;
                }
                Route withId = isBlank(route.routeId())
                        ? new Route(UUID.randomUUID().toString(), route.agencyId(), route.routeShortName(),
                        route.routeLongName(), route.routeType(), route.routeColor(), route.routeTextColor())
                        : route;
                if (!routeIds.add(withId.routeId())) {
                    throw new IllegalArgumentException("Hat ID'si birden fazla kez gönderildi: " + withId.routeId());
                }
                if (isBlank(withId.routeShortName()) || isBlank(withId.routeLongName())) {
                    throw new IllegalArgumentException("Hat kısa ve uzun adı dolu olmalıdır: " + withId.routeId());
                }
                normalized.add(withId);
            }
        }
        routeRepository.replaceAllDraft(userId, normalized);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
