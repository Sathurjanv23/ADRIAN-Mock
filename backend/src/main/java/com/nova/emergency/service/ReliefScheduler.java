package com.nova.emergency.service;

import com.nova.emergency.controller.SseController;
import com.nova.emergency.model.ReliefMission;
import com.nova.emergency.model.ReliefRequest;
import com.nova.emergency.repository.ReliefMissionRepository;
import com.nova.emergency.repository.ReliefRequestRepository;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * ReliefScheduler — Background tasks for the ADRN Relief Logistics module.
 *
 * Scheduled tasks:
 * 1. Broadcast active mission locations via SSE every 5 seconds
 * 2. Detect critical shortages and notify Command Center every 30 seconds
 * 3. Recalculate ETAs for active missions every 60 seconds
 */
@Component
@EnableScheduling
public class ReliefScheduler {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ReliefScheduler.class);

    private final ReliefMissionRepository reliefMissionRepository;
    private final ReliefRequestRepository reliefRequestRepository;
    private final SseController sseController;
    private final ReliefService reliefService;

    public ReliefScheduler(ReliefMissionRepository reliefMissionRepository,
                            ReliefRequestRepository reliefRequestRepository,
                            SseController sseController,
                            ReliefService reliefService) {
        this.reliefMissionRepository = reliefMissionRepository;
        this.reliefRequestRepository = reliefRequestRepository;
        this.sseController = sseController;
        this.reliefService = reliefService;
    }

    /**
     * Broadcast active mission locations every 5 seconds.
     * Simulates GPS movement for demo/dev purposes.
     * In production: vehicles update their own location via PUT /api/relief-missions/{id}/location
     */
    @Scheduled(fixedRate = 5000)
    public void broadcastActiveMissionLocations() {
        try {
            List<ReliefMission> activeMissions = reliefMissionRepository
                .findByStatusIn(List.of("ASSIGNED", "ON_THE_WAY"));

            if (activeMissions.isEmpty()) return;

            List<Map<String, Object>> updates = new ArrayList<>();
            for (ReliefMission mission : activeMissions) {
                // Simulate gradual movement toward destination in dev mode
                if (mission.getDestination() != null && mission.getCurrentLat() != 0) {
                    double newLat = simulateMovement(mission.getCurrentLat(),
                                                      mission.getDestination().getLat(), 0.0003);
                    double newLng = simulateMovement(mission.getCurrentLng(),
                                                      mission.getDestination().getLng(), 0.0003);
                    mission.setCurrentLat(newLat);
                    mission.setCurrentLng(newLng);

                    // Recalculate ETA
                    double remaining = ReliefService.haversineDistance(
                        newLat, newLng,
                        mission.getDestination().getLat(),
                        mission.getDestination().getLng()
                    );
                    mission.setEta(Math.max(0, (int) Math.ceil(remaining / 40.0 * 60)));
                    mission.setStatus("ON_THE_WAY");
                    reliefMissionRepository.save(mission);
                }

                Map<String, Object> update = new LinkedHashMap<>();
                update.put("missionId", mission.getId());
                update.put("status", mission.getStatus());
                update.put("lat", mission.getCurrentLat());
                update.put("lng", mission.getCurrentLng());
                update.put("eta", mission.getEta());
                update.put("vehicleName", mission.getVehicleName());
                updates.add(update);
            }

            if (!updates.isEmpty()) {
                sseController.broadcast("RELIEF_LOCATION_UPDATED", updates);
            }

        } catch (Exception e) {
            log.debug("SSE broadcast error (non-critical): {}", e.getMessage());
        }
    }

    /**
     * Check for critical shortages and broadcast alerts every 30 seconds.
     */
    @Scheduled(fixedRate = 30000)
    public void detectCriticalShortages() {
        try {
            List<ReliefRequest> criticalPending = reliefRequestRepository
                .findByPriorityAndStatusOrderByCreatedAtAsc("CRITICAL", "PENDING");

            if (!criticalPending.isEmpty()) {
                Map<String, Object> alert = new LinkedHashMap<>();
                alert.put("type", "CRITICAL_SHORTAGE_DETECTED");
                alert.put("count", criticalPending.size());
                alert.put("message", criticalPending.size() + " critical relief request(s) are PENDING without assigned missions.");
                alert.put("requestIds", criticalPending.stream().map(ReliefRequest::getId).toList());
                alert.put("timestamp", java.time.Instant.now().toString());

                sseController.broadcast("CRITICAL_SHORTAGE_DETECTED", alert);
                log.warn("[ReliefScheduler] Critical shortage: {} unassigned CRITICAL relief requests", criticalPending.size());
            }

        } catch (Exception e) {
            log.debug("Shortage detection error (non-critical): {}", e.getMessage());
        }
    }

    /**
     * Broadcast relief stats summary every 60 seconds.
     */
    @Scheduled(fixedRate = 60000)
    public void broadcastReliefStats() {
        try {
            Map<String, Object> stats = reliefService.getReliefStats();
            sseController.broadcast("RELIEF_STATS_UPDATED", stats);
        } catch (Exception e) {
            log.debug("Relief stats broadcast error (non-critical): {}", e.getMessage());
        }
    }

    /**
     * Simple linear interpolation for GPS simulation in dev mode.
     */
    private double simulateMovement(double current, double target, double step) {
        if (Math.abs(target - current) <= step) return target;
        return current + (target > current ? step : -step);
    }
}
