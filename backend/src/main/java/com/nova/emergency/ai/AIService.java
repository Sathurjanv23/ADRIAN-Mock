package com.nova.emergency.ai;

/**
 * AIService — Clean abstraction for all AI operations.
 *
 * Implementations:
 *   - MockAIService     : deterministic rules-based (default, dev/fallback)
 *   - BedrockAIService  : Amazon Bedrock Claude (production)
 *
 * The platform ALWAYS continues operating if AI is unavailable.
 * AI is advisory — critical decisions remain with human operators.
 */
public interface AIService {

    /**
     * Analyze an incoming emergency incident to determine type, severity, and recommendations.
     */
    IncidentAnalysisResult analyzeIncident(IncidentAnalysisRequest request);

    /**
     * Natural-language Command Center copilot query.
     */
    CopilotResponse copilot(String query, String contextJson);

    /**
     * Recommend the best relief plan for a disaster location.
     */
    ReliefRecommendation recommendRelief(ReliefRecommendationRequest request);

    /**
     * Returns true if the AI backend is reachable and healthy.
     * Used for fallback detection.
     */
    boolean isAvailable();

    // ─── Request / Response DTOs (inner records) ─────────────────────

    record IncidentAnalysisRequest(
        String incidentId,
        String type,
        String description,
        String detectedLanguage,
        int peopleAffected,
        double lat,
        double lng,
        boolean hasPhoto,
        boolean hasAudio
    ) {}

    record IncidentAnalysisResult(
        String emergencyType,
        String severity,           // critical | high | medium | low
        int confidenceScore,       // 0-100
        int estimatedPeopleAffected,
        String recommendedAction,
        java.util.List<String> riskFactors,
        int estimatedResponseTimeMinutes,
        boolean requiresRelief,
        String detectedLanguage,
        String explanation
    ) {}

    record CopilotResponse(
        String answer,
        java.util.List<String> recommendations,
        java.util.List<String> sources,
        int confidenceScore,
        boolean requiresHumanReview
    ) {}

    record ReliefRecommendationRequest(
        String reliefRequestId,
        double disasterLat,
        double disasterLng,
        int peopleAffected,
        int requiredMeals,
        int requiredWater,
        String priority,
        java.util.List<FoodSourceOption> availableSources
    ) {}

    record FoodSourceOption(
        String sourceId,
        String sourceName,
        String sourceType,
        double lat,
        double lng,
        int availableMeals,
        int waterBottles,
        double distanceKm,
        String expiryTime
    ) {}

    record ReliefRecommendation(
        String bestSourceId,
        String bestSourceName,
        String reasoning,
        int estimatedDeliveryMinutes,
        java.util.List<String> alternativeSources,
        java.util.List<String> routeWarnings,
        String deliveryPriority,
        int confidenceScore
    ) {}
}
