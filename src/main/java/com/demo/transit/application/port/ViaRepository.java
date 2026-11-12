package com.demo.transit.application.port;

import com.demo.transit.application.model.Via;

import java.util.List;

public interface ViaRepository {
    List<Via> findPublishedVias(String routeId);
    List<Via> findDraftVias(String routeId, String userId);
    void deleteDraftVias(String userId, String routeId);
    void batchInsertDraftVias(String userId, String routeId, List<Via> vias);
}
