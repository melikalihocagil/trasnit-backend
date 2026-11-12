package com.demo.transit.application;

import com.demo.transit.application.model.Stop;
import com.demo.transit.application.port.StopRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class StopService {

    private final StopRepository stopRepository;

    public StopService(StopRepository stopRepository) {
        this.stopRepository = stopRepository;
    }

    public List<Stop> getPublishedStops() {
        return stopRepository.findAllPublished();
    }

    public List<Stop> getDraftStops(String userId) {
        return stopRepository.findAllDraft(userId);
    }

    @Transactional
    public void saveDraftStop(String userId, Stop stop) {
        stopRepository.saveDraftStop(userId, stop);
    }

    @Transactional
    public void saveAllDraftStops(String userId, List<Stop> stops) {
        if (stops == null || stops.isEmpty()) {
            return;
        }
        stopRepository.saveAllDraftStops(userId, stops);
    }

    public List<String> findRouteIdsByStopId(String userId, String stopId) {
        return stopRepository.findRouteIdsByStopId(stopId, userId);
    }

    public List<String> findRouteIdsByStopIds(String userId, List<String> stopIds) {
        return stopRepository.findRouteIdsByStopIds(stopIds, userId);
    }

    @Transactional
    public void deleteDraftStop(String userId, String stopId) {
        stopRepository.deleteDraftStop(userId, stopId);
    }

    @Transactional
    public void deleteAllDraftStops(String userId) {
        stopRepository.deleteAllDraftStops(userId);
    }
}
