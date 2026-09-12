package com.nova.emergency.controller;

import com.nova.emergency.dto.ApiResponse;
import com.nova.emergency.model.ReliefMission;
import com.nova.emergency.service.ReliefService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * ReliefMissionController — REST API for managing and tracking relief missions.
 *
 * POST /api/relief-missions                        → Create mission
 * GET  /api/relief-missions                        → All missions
 * GET  /api/relief-missions/active                 → Active missions only
 * GET  /api/relief-missions/{id}                   → Single mission
 * PUT  /api/relief-missions/{id}/status            → Update status
 * PUT  /api/relief-missions/{id}/location          → Update live GPS (vehicle app)
 */
@RestController
@RequestMapping("/api/relief-missions")
public class ReliefMissionController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ReliefMissionController.class);

    private final ReliefService reliefService;

    public ReliefMissionController(ReliefService reliefService) {
        this.reliefService = reliefService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ReliefMission>> create(
            @RequestBody Map<String, String> body,
            Authentication auth) {
        String reliefRequestId = body.get("reliefRequestId");
        String foodSourceId    = body.get("foodSourceId");
        String vehicleId       = body.getOrDefault("vehicleId", "VEH-" + System.currentTimeMillis());
        String vehicleName     = body.getOrDefault("vehicleName", "Relief Vehicle");
        String driverName      = body.getOrDefault("driverName", "Driver");
        String driverContact   = body.getOrDefault("driverContact", "");

        if (reliefRequestId == null || foodSourceId == null) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("BAD_REQUEST", "reliefRequestId and foodSourceId are required"));
        }

        log.info("Creating relief mission for request={} source={} by {}",
            reliefRequestId, foodSourceId, auth != null ? auth.getName() : "unknown");

        ReliefMission mission = reliefService.createReliefMission(
            reliefRequestId, foodSourceId, vehicleId, vehicleName, driverName, driverContact
        );
        return ResponseEntity.ok(ApiResponse.ok(mission, "Relief mission created"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ReliefMission>>> getAll(
            @RequestParam(required = false) String status) {
        List<ReliefMission> missions = status != null
            ? reliefService.getAllReliefMissions().stream()
                .filter(m -> status.equalsIgnoreCase(m.getStatus()))
                .toList()
            : reliefService.getAllReliefMissions();
        return ResponseEntity.ok(ApiResponse.ok(missions));
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<ReliefMission>>> getActive() {
        return ResponseEntity.ok(ApiResponse.ok(reliefService.getActiveMissions()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ReliefMission>> getById(@PathVariable String id) {
        return reliefService.getReliefMissionById(id)
            .map(m -> ResponseEntity.ok(ApiResponse.ok(m)))
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/by-incident/{incidentId}")
    public ResponseEntity<ApiResponse<ReliefMission>> getByIncident(@PathVariable String incidentId) {
        return reliefService.getMissionByIncidentId(incidentId)
            .map(m -> ResponseEntity.ok(ApiResponse.ok(m)))
            .orElse(ResponseEntity.ok(ApiResponse.ok(null, "No active relief mission for incident")));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStats() {
        return ResponseEntity.ok(ApiResponse.ok(reliefService.getReliefStats()));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<ReliefMission>> updateStatus(
            @PathVariable String id,
            @RequestBody Map<String, String> body,
            Authentication auth) {
        String status = body.get("status");
        String notes  = body.get("notes");
        if (status == null || status.isBlank()) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("BAD_REQUEST", "status is required"));
        }
        log.info("Updating relief mission {} to status={} by {}", id, status,
            auth != null ? auth.getName() : "unknown");
        ReliefMission updated = reliefService.updateMissionStatus(id, status.toUpperCase(), notes);
        return ResponseEntity.ok(ApiResponse.ok(updated, "Mission status updated"));
    }

    @PutMapping("/{id}/location")
    public ResponseEntity<ApiResponse<ReliefMission>> updateLocation(
            @PathVariable String id,
            @RequestBody Map<String, Double> body) {
        Double lat = body.get("lat");
        Double lng = body.get("lng");
        if (lat == null || lng == null) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("BAD_REQUEST", "lat and lng are required"));
        }
        ReliefMission updated = reliefService.updateMissionLocation(id, lat, lng);
        return ResponseEntity.ok(ApiResponse.ok(updated, "Location updated"));
    }
}
