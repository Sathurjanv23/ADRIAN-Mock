package com.nova.emergency.controller;

import com.nova.emergency.dto.ApiResponse;
import com.nova.emergency.model.ReliefRequest;
import com.nova.emergency.service.ReliefService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * ReliefRequestController — REST API for managing relief requests.
 *
 * POST /api/relief-requests                        → Create relief request
 * POST /api/relief-requests/from-incident/{id}    → Create from existing incident
 * GET  /api/relief-requests                        → All requests
 * GET  /api/relief-requests/{id}                   → Single request
 * PUT  /api/relief-requests/{id}/status            → Update status
 * GET  /api/relief-requests/stats                  → Aggregated stats
 */
@RestController
@RequestMapping("/api/relief-requests")
public class ReliefRequestController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ReliefRequestController.class);

    private final ReliefService reliefService;

    public ReliefRequestController(ReliefService reliefService) {
        this.reliefService = reliefService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ReliefRequest>> create(
            @RequestBody ReliefRequest request,
            Authentication auth) {
        String user = auth != null ? auth.getName() : "SYSTEM";
        request.setCreatedBy(user);
        ReliefRequest created = reliefService.createReliefRequest(request);
        return ResponseEntity.ok(ApiResponse.ok(created, "Relief request created"));
    }

    @PostMapping("/from-incident/{incidentId}")
    public ResponseEntity<ApiResponse<ReliefRequest>> createFromIncident(
            @PathVariable String incidentId,
            Authentication auth) {
        String user = auth != null ? auth.getName() : "SYSTEM";
        log.info("Creating relief request from incident {} by {}", incidentId, user);
        ReliefRequest created = reliefService.createReliefRequestFromIncident(incidentId, user);
        return ResponseEntity.ok(ApiResponse.ok(created, "Relief request created from incident"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ReliefRequest>>> getAll(
            @RequestParam(required = false) String status) {
        List<ReliefRequest> requests = status != null
            ? reliefService.getReliefRequestsByStatus(status)
            : reliefService.getAllReliefRequests();
        return ResponseEntity.ok(ApiResponse.ok(requests));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ReliefRequest>> getById(@PathVariable String id) {
        return reliefService.getReliefRequestById(id)
            .map(r -> ResponseEntity.ok(ApiResponse.ok(r)))
            .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<ReliefRequest>> updateStatus(
            @PathVariable String id,
            @RequestBody Map<String, String> body) {
        String status = body.get("status");
        if (status == null || status.isBlank()) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("BAD_REQUEST", "status is required"));
        }
        ReliefRequest updated = reliefService.updateReliefRequestStatus(id, status);
        return ResponseEntity.ok(ApiResponse.ok(updated, "Status updated"));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStats() {
        return ResponseEntity.ok(ApiResponse.ok(reliefService.getReliefStats()));
    }
}
