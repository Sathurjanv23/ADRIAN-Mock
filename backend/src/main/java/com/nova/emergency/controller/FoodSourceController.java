package com.nova.emergency.controller;

import com.nova.emergency.dto.ApiResponse;
import com.nova.emergency.model.FoodSource;
import com.nova.emergency.service.ReliefService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * FoodSourceController — REST API for managing food/water relief sources.
 *
 * GET  /api/food-sources          → All sources (authenticated)
 * GET  /api/food-sources/nearby   → Nearby sources sorted by distance
 * GET  /api/food-sources/{id}     → Single source
 * POST /api/food-sources          → Create source (OFFICER, ADMIN)
 * PUT  /api/food-sources/{id}     → Update source (OFFICER, ADMIN)
 */
@RestController
@RequestMapping("/api/food-sources")
public class FoodSourceController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(FoodSourceController.class);

    private final ReliefService reliefService;

    public FoodSourceController(ReliefService reliefService) {
        this.reliefService = reliefService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<FoodSource>>> getAll(
            @RequestParam(required = false) String status) {
        List<FoodSource> sources = "active".equalsIgnoreCase(status)
            ? reliefService.getActiveFoodSources()
            : reliefService.getAllFoodSources();
        return ResponseEntity.ok(ApiResponse.ok(sources));
    }

    @GetMapping("/nearby")
    public ResponseEntity<ApiResponse<List<FoodSource>>> getNearby(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "50") double radiusKm,
            @RequestParam(defaultValue = "0") int minMeals) {
        List<FoodSource> sources = reliefService.getNearbyFoodSources(lat, lng, radiusKm, minMeals);
        return ResponseEntity.ok(ApiResponse.ok(sources));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FoodSource>> getById(@PathVariable String id) {
        return reliefService.getFoodSourceById(id)
            .map(s -> ResponseEntity.ok(ApiResponse.ok(s)))
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<ApiResponse<FoodSource>> create(
            @RequestBody FoodSource source,
            Authentication auth) {
        log.info("Creating food source '{}' by {}", source.getName(), auth != null ? auth.getName() : "unknown");
        FoodSource created = reliefService.createFoodSource(source);
        return ResponseEntity.ok(ApiResponse.ok(created, "Food source created successfully"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<FoodSource>> update(
            @PathVariable String id,
            @RequestBody FoodSource updates,
            Authentication auth) {
        log.info("Updating food source {} by {}", id, auth != null ? auth.getName() : "unknown");
        FoodSource updated = reliefService.updateFoodSource(id, updates);
        return ResponseEntity.ok(ApiResponse.ok(updated, "Food source updated"));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<FoodSource>> updateStatus(
            @PathVariable String id,
            @RequestBody Map<String, String> body) {
        FoodSource updates = new FoodSource();
        updates.setStatus(body.get("status"));
        FoodSource updated = reliefService.updateFoodSource(id, updates);
        return ResponseEntity.ok(ApiResponse.ok(updated, "Status updated"));
    }
}
