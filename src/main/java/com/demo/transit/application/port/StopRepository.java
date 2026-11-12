package com.demo.transit.application.port;

import com.demo.transit.application.model.Stop;

import java.util.List;

public interface StopRepository {
    List<Stop> findAllPublished();
    List<Stop> findAllDraft(String userId);
    void saveDraftStop(String userId, Stop stop);
    void saveAllDraftStops(String userId, List<Stop> stops);
    void deleteDraftStop(String userId, String stopId);
    void deleteAllDraftStops(String userId);
    List<String> findRouteIdsByStopId(String stopId, String userId);
    List<String> findRouteIdsByStopIds(List<String> stopIds, String userId);
}
