package com.demo.transit.application.port;

import com.demo.transit.application.model.Stop;

import java.util.List;

public interface StopTimeRepository {
    List<Stop> findPublishedStops(String tripId);
    List<Stop> findDraftStopTimes(String tripId, String userId);
    List<Stop> findDraftStops(String tripId, String userId);
    boolean hasDraftStops(String tripId, String userId);
    void deleteDraftStopTimes(String userId, String tripId);
    void batchInsertDraftStopTimes(String userId, String tripId, List<Stop> stops);
}
