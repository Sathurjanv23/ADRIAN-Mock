package com.nova.emergency.controller;

import com.nova.emergency.ai.AIService;
import com.nova.emergency.dto.ApiResponse;
import com.nova.emergency.service.ReliefService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * AIController — REST API for AI-powered analysis and copilot.
 *
 * POST /api/ai/analyze-incident        → Analyze an emergency incident
 * POST /api/ai/copilot                 → Natural-language Command Center query
 * POST /api/ai/relief-recommendation   → Get AI relief supply recommendation
 */
@RestController
@RequestMapping("/api/ai")
public class AIController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AIController.class);

    private final AIService aiService;
    private final ReliefService reliefService;

    public AIController(AIService aiService, ReliefService reliefService) {
        this.aiService = aiService;
        this.reliefService = reliefService;
    }

    /**
     * Analyze an incident using AI.
     * Returns severity, recommended action, resource requirements, and relief flag.
     */
    @PostMapping("/analyze-incident")
    public ResponseEntity<ApiResponse<AIService.IncidentAnalysisResult>> analyzeIncident(
            @RequestBody Map<String, Object> body,
            Authentication auth) {

        String incidentId     = (String) body.getOrDefault("incidentId", "");
        String type           = (String) body.getOrDefault("type", "unknown");
        String description    = (String) body.getOrDefault("description", "");
        String lang           = (String) body.getOrDefault("detectedLanguage", "en");
        int peopleAffected    = ((Number) body.getOrDefault("peopleAffected", 1)).intValue();
        double lat            = ((Number) body.getOrDefault("lat", 0.0)).doubleValue();
        double lng            = ((Number) body.getOrDefault("lng", 0.0)).doubleValue();
        boolean hasPhoto      = Boolean.TRUE.equals(body.get("hasPhoto"));
        boolean hasAudio      = Boolean.TRUE.equals(body.get("hasAudio"));

        log.info("[AI] Analyzing incident type={} people={} by {}",
            type, peopleAffected, auth != null ? auth.getName() : "unknown");

        AIService.IncidentAnalysisRequest request = new AIService.IncidentAnalysisRequest(
            incidentId, type, description, lang, peopleAffected, lat, lng, hasPhoto, hasAudio
        );

        AIService.IncidentAnalysisResult result = aiService.analyzeIncident(request);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /**
     * Command Center AI Copilot — answer natural-language operational questions.
     */
    @PostMapping("/copilot")
    public ResponseEntity<ApiResponse<AIService.CopilotResponse>> copilot(
            @RequestBody Map<String, String> body,
            Authentication auth) {

        String query   = body.getOrDefault("query", "");
        String context = body.getOrDefault("context", "{}");

        if (query.isBlank()) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("BAD_REQUEST", "query cannot be empty"));
        }

        log.info("[AI] Copilot query from {} : '{}'",
            auth != null ? auth.getName() : "unknown", query);

        AIService.CopilotResponse response = aiService.copilot(query, context);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * Get AI relief recommendation for a pending relief request.
     */
    @PostMapping("/relief-recommendation")
    public ResponseEntity<ApiResponse<AIService.ReliefRecommendation>> reliefRecommendation(
            @RequestBody Map<String, String> body,
            Authentication auth) {

        String reliefRequestId = body.get("reliefRequestId");
        if (reliefRequestId == null || reliefRequestId.isBlank()) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("BAD_REQUEST", "reliefRequestId is required"));
        }

        log.info("[AI] Relief recommendation requested for {} by {}",
            reliefRequestId, auth != null ? auth.getName() : "unknown");

        AIService.ReliefRecommendation recommendation = reliefService.getAIReliefRecommendation(reliefRequestId);
        return ResponseEntity.ok(ApiResponse.ok(recommendation));
    }

    /**
     * Health check for the AI service.
     */
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> status() {
        Map<String, Object> status = Map.of(
            "available", aiService.isAvailable(),
            "provider", aiService.getClass().getSimpleName(),
            "mode", aiService.isAvailable() ? "ONLINE" : "FALLBACK_MOCK"
        );
        return ResponseEntity.ok(ApiResponse.ok(status));
    }
}
