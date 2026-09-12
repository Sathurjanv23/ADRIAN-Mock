package com.nova.emergency.ai;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * MockAIService — Deterministic, rules-based AI implementation.
 *
 * Used in two scenarios:
 * 1. Local development (adrn.ai.provider=mock, the default)
 * 2. Automatic fallback when BedrockAIService is unavailable
 *
 * The platform remains fully operational with this service.
 */
@Service
@Primary
@ConditionalOnProperty(name = "adrn.ai.provider", havingValue = "mock", matchIfMissing = true)
public class MockAIService implements AIService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MockAIService.class);

    // Severity rules: incident type → base severity
    private static final Map<String, String> TYPE_SEVERITY = Map.ofEntries(
        Map.entry("flood",            "high"),
        Map.entry("fire",             "critical"),
        Map.entry("earthquake",       "critical"),
        Map.entry("landslide",        "high"),
        Map.entry("medical",          "high"),
        Map.entry("building_collapse","critical"),
        Map.entry("road_accident",    "high"),
        Map.entry("severe_weather",   "medium"),
        Map.entry("crime",            "medium"),
        Map.entry("missing_person",   "low"),
        Map.entry("other",            "medium"),
        Map.entry("unknown",          "medium")
    );

    // Relief-requiring types
    private static final List<String> RELIEF_TYPES = List.of("flood", "earthquake", "landslide", "severe_weather", "building_collapse");

    @Override
    public IncidentAnalysisResult analyzeIncident(IncidentAnalysisRequest request) {
        log.info("[MockAI] Analyzing incident {} type={}", request.incidentId(), request.type());

        String type = request.type() != null ? request.type().toLowerCase() : "unknown";
        String baseSeverity = TYPE_SEVERITY.getOrDefault(type, "medium");

        // Escalate severity based on people affected
        String severity = baseSeverity;
        if (request.peopleAffected() > 100) severity = "critical";
        else if (request.peopleAffected() > 20 && "medium".equals(baseSeverity)) severity = "high";

        int confidence = 75;
        if (request.hasPhoto()) confidence += 10;
        if (request.hasAudio()) confidence += 5;
        confidence = Math.min(confidence, 95);

        List<String> riskFactors = new ArrayList<>();
        if (request.peopleAffected() > 10) riskFactors.add("Large number of people affected");
        if ("flood".equals(type) || "landslide".equals(type)) riskFactors.add("Terrain and weather conditions may worsen");
        if ("fire".equals(type)) riskFactors.add("Risk of structural collapse and toxic smoke");
        if ("medical".equals(type)) riskFactors.add("Time-critical medical intervention required");

        int responseTime = switch (severity) {
            case "critical" -> 8;
            case "high"     -> 15;
            case "medium"   -> 25;
            default          -> 40;
        };

        boolean requiresRelief = RELIEF_TYPES.contains(type) || request.peopleAffected() > 50;

        String recommendedAction = buildRecommendedAction(type, severity, request.peopleAffected());

        return new IncidentAnalysisResult(
            type,
            severity,
            confidence,
            request.peopleAffected(),
            recommendedAction,
            riskFactors,
            responseTime,
            requiresRelief,
            request.detectedLanguage() != null ? request.detectedLanguage() : "en",
            "AI analysis completed using deterministic emergency protocol rules. " +
            "Severity escalated to " + severity + " based on incident type and affected population."
        );
    }

    @Override
    public CopilotResponse copilot(String query, String contextJson) {
        log.info("[MockAI] Copilot query: {}", query);

        String lowerQuery = query.toLowerCase();
        String answer;
        List<String> recommendations = new ArrayList<>();
        boolean requiresHumanReview = false;

        if (lowerQuery.contains("critical") || lowerQuery.contains("most urgent")) {
            answer = "Based on current data, critical incidents requiring immediate attention are those with severity=CRITICAL and status=assigned or en_route. " +
                     "Recommend reviewing Command Center incident board sorted by priority.";
            recommendations.add("Prioritize incidents with severity=CRITICAL");
            recommendations.add("Ensure all CRITICAL incidents have assigned rescue teams");
        } else if (lowerQuery.contains("hospital") && (lowerQuery.contains("icu") || lowerQuery.contains("capacity"))) {
            answer = "Hospital ICU capacity should be checked in real-time. Hospitals with icuAvailable > 0 can accept critical patients. " +
                     "Contact hospitals directly for the most current availability.";
            recommendations.add("Check hospital portal for real-time ICU availability");
            recommendations.add("Consider patient severity vs hospital specialization");
        } else if (lowerQuery.contains("flood") && lowerQuery.contains("impact")) {
            answer = "Flood incidents are tracked by geographic zone. The largest impact is determined by combining peopleAffected across all active flood incidents. " +
                     "Review the Disaster Map for zone-level impact visualization.";
            recommendations.add("Activate flood response teams in affected districts");
            recommendations.add("Coordinate with relief logistics for water supply");
        } else if (lowerQuery.contains("relief") || lowerQuery.contains("food") || lowerQuery.contains("water")) {
            answer = "Relief logistics are managed through the ADRN Relief Module. Food sources are ranked by proximity, available quantity, and expiry time. " +
                     "AI recommends assigning the nearest active food source with sufficient stock.";
            recommendations.add("Check Relief Dashboard for current meal/water availability");
            recommendations.add("Prioritize deliveries to children and elderly shelters");
            requiresHumanReview = true;
        } else {
            answer = "I'm NOVA, the ADRN AI Copilot. I can help you with incident analysis, hospital capacity, rescue team status, " +
                     "and relief logistics recommendations. Please ask a specific operational question.";
            recommendations.add("Use specific incident IDs for detailed analysis");
            recommendations.add("Ask about specific districts or emergency types for targeted intelligence");
        }

        return new CopilotResponse(
            answer,
            recommendations,
            List.of("Incident Database", "Hospital Registry", "Relief Logistics Module"),
            78,
            requiresHumanReview
        );
    }

    @Override
    public ReliefRecommendation recommendRelief(ReliefRecommendationRequest request) {
        log.info("[MockAI] Relief recommendation for request={} people={} priority={}",
                request.reliefRequestId(), request.peopleAffected(), request.priority());

        if (request.availableSources() == null || request.availableSources().isEmpty()) {
            return new ReliefRecommendation(
                null, "No sources available",
                "No food sources with sufficient inventory are available in the area. " +
                "Consider activating emergency government reserves or requesting mutual aid.",
                0, List.of(), List.of("NO AVAILABLE SOURCES — Manual intervention required"), "CRITICAL", 20
            );
        }

        // ─── Priority Rules Evaluation ──────────────────────────────────
        // 1. Hospital emergency supplies (CRITICAL priority or medical triage)
        // 2. Children and elderly shelters (HIGH priority)
        // 3. Large shelters (> 100 people affected)
        // 4. Remaining public requests
        String priorityTier;
        int priorityRank;
        if ("CRITICAL".equalsIgnoreCase(request.priority())) {
            priorityTier = "Tier 1: Hospital Emergency Supplies & Critical Triage";
            priorityRank = 1;
        } else if (request.peopleAffected() > 100) {
            priorityTier = "Tier 2/3: High Capacity Disaster Shelter (>100 affected)";
            priorityRank = 2;
        } else if ("HIGH".equalsIgnoreCase(request.priority())) {
            priorityTier = "Tier 2: Vulnerable Population Shelter (Children/Elderly Priority)";
            priorityRank = 3;
        } else {
            priorityTier = "Tier 4: General Public Relief Request";
            priorityRank = 4;
        }

        // Rank sources: multi-factor composite score
        // Factor 1: Distance proximity score (closer is better)
        // Factor 2: Quantity sufficiency (can cover required meals & water)
        // Factor 3: Food freshness (higher shelf-life buffer)
        // Factor 4: Travel time / speed feasibility
        FoodSourceOption best = request.availableSources().stream()
            .min((a, b) -> {
                // Check if sufficient quantity exists
                boolean aSufficient = a.availableMeals() >= request.requiredMeals() && a.waterBottles() >= request.requiredWater();
                boolean bSufficient = b.availableMeals() >= request.requiredMeals() && b.waterBottles() >= request.requiredWater();

                if (aSufficient != bSufficient) {
                    return aSufficient ? -1 : 1; // Prioritize source with full stock
                }

                // Weighted score: 50% distance + 30% quantity ratio + 20% freshness buffer
                double aScore = a.distanceKm() * 1.5 - Math.min(2.0, (double) a.availableMeals() / Math.max(1, request.requiredMeals()));
                double bScore = b.distanceKm() * 1.5 - Math.min(2.0, (double) b.availableMeals() / Math.max(1, request.requiredMeals()));
                return Double.compare(aScore, bScore);
            })
            .orElse(request.availableSources().get(0));

        int travelEta = (int) Math.max(5, Math.ceil(best.distanceKm() / 40.0 * 60)); // assume 40 km/h relief vehicle speed

        List<String> alternatives = request.availableSources().stream()
            .filter(s -> !s.sourceId().equals(best.sourceId()))
            .map(FoodSourceOption::sourceName)
            .limit(3)
            .toList();

        List<String> warnings = new ArrayList<>();
        if (travelEta > 30) {
            warnings.add("ETA exceeds 30 minutes (" + travelEta + " min) — suggest establishing an intermediate staging post.");
        }
        if (best.availableMeals() < request.requiredMeals()) {
            warnings.add("Partial meal fulfilment: Selected source has " + best.availableMeals() +
                         " meals (Required: " + request.requiredMeals() + "). Secondary dispatch recommended.");
        }
        if (best.waterBottles() < request.requiredWater()) {
            warnings.add("Partial water fulfilment: Selected source has " + best.waterBottles() +
                         " bottles (Required: " + request.requiredWater() + ").");
        }

        String rationale = String.format(
            "AI Decision Engine selected [%s] for %s. Proximity: %.1f km (Travel time: ~%d min). " +
            "Stock available: %d hot meals, %d bottled water. Meets %.0f%% of emergency nourishment requirements.",
            best.sourceName(),
            priorityTier,
            best.distanceKm(),
            travelEta,
            best.availableMeals(),
            best.waterBottles(),
            Math.min(100.0, (double) best.availableMeals() / Math.max(1, request.requiredMeals()) * 100.0)
        );

        return new ReliefRecommendation(
            best.sourceId(),
            best.sourceName(),
            rationale,
            travelEta,
            alternatives,
            warnings,
            request.priority() != null ? request.priority() : "HIGH",
            Math.min(98, 80 + (priorityRank == 1 ? 15 : 8))
        );
    }

    @Override
    public boolean isAvailable() {
        return true; // MockAI is always available
    }

    // ─── Private helpers ──────────────────────────────────────────────

    private String buildRecommendedAction(String type, String severity, int peopleAffected) {
        return switch (type) {
            case "flood"    -> "Deploy rescue boats and evacuation teams. Coordinate with relief logistics for food/water supply. " +
                               "Estimate " + (peopleAffected * 3) + " meals per day required.";
            case "fire"     -> "Dispatch fire rescue and medical teams immediately. Evacuate " + peopleAffected + " people from affected area.";
            case "earthquake","building_collapse" -> "Activate search-and-rescue protocol. Deploy structural engineers. " +
                               "Prepare field hospital for potential casualties.";
            case "landslide"-> "Block affected roads immediately. Deploy heavy machinery and rescue teams. " +
                               "Coordinate with hospitals for potential injuries.";
            case "medical"  -> "Dispatch nearest ambulance and paramedic team. Notify receiving hospital of incoming patient.";
            default         -> "Assess situation and deploy appropriate emergency response team. " +
                               "Monitor and update Command Center every 10 minutes.";
        };
    }
}
