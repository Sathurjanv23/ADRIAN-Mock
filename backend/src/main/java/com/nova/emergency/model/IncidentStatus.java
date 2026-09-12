package com.nova.emergency.model;

import com.nova.emergency.exception.InvalidStatusException;
import lombok.Getter;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Getter
public enum IncidentStatus {
    REPORTED("reported"),
    AI_ANALYZED("ai_analyzed"),
    PRIORITIZED("prioritized"),
    DISPATCHED("dispatched"),
    ACKNOWLEDGED("acknowledged"),
    EN_ROUTE("en_route"),
    ON_SCENE("on_scene"),
    TRANSPORTING("transporting"),
    RESOLVED("resolved"),
    CANCELLED("cancelled");

    private final String value;

    IncidentStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    // State machine transition table
    private static final Map<IncidentStatus, Set<IncidentStatus>> ALLOWED_TRANSITIONS = Map.of(
            REPORTED, EnumSet.of(REPORTED, AI_ANALYZED, PRIORITIZED, DISPATCHED, ACKNOWLEDGED, CANCELLED),
            AI_ANALYZED, EnumSet.of(AI_ANALYZED, PRIORITIZED, DISPATCHED, ACKNOWLEDGED, CANCELLED),
            PRIORITIZED, EnumSet.of(PRIORITIZED, DISPATCHED, ACKNOWLEDGED, CANCELLED),
            DISPATCHED, EnumSet.of(DISPATCHED, ACKNOWLEDGED, EN_ROUTE, CANCELLED),
            ACKNOWLEDGED, EnumSet.of(ACKNOWLEDGED, EN_ROUTE, ON_SCENE, CANCELLED),
            EN_ROUTE, EnumSet.of(EN_ROUTE, ON_SCENE, TRANSPORTING, CANCELLED),
            ON_SCENE, EnumSet.of(ON_SCENE, TRANSPORTING, RESOLVED, CANCELLED),
            TRANSPORTING, EnumSet.of(TRANSPORTING, RESOLVED, CANCELLED),
            RESOLVED, EnumSet.of(RESOLVED),
            CANCELLED, EnumSet.of(CANCELLED));

    public static IncidentStatus fromString(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new InvalidStatusException("Incident status must not be empty.");
        }
        String normalized = raw.trim().toLowerCase().replace("-", "_").replace(" ", "_");

        return switch (normalized) {
            case "reported", "submitted" -> REPORTED;
            case "ai_analyzed", "analysing", "analyzing" -> AI_ANALYZED;
            case "prioritized", "verified" -> PRIORITIZED;
            case "dispatched", "assigned" -> DISPATCHED;
            case "acknowledged" -> ACKNOWLEDGED;
            case "en_route", "enroute", "on_the_way" -> EN_ROUTE;
            case "on_scene", "onscene", "responding", "arrived", "arrival", "rescued" -> ON_SCENE;
            case "transporting", "transport", "delivered_to_hospital", "delivered" -> TRANSPORTING;
            case "resolved", "closed", "completed" -> RESOLVED;
            case "cancelled", "canceled" -> CANCELLED;
            default -> throw new InvalidStatusException("Invalid incident status: '" + raw
                    + "'. Allowed values: reported, ai_analyzed, prioritized, dispatched, acknowledged, en_route, on_scene, transporting, resolved, cancelled.");
        };
    }

    public static boolean isValidTransition(IncidentStatus from, IncidentStatus to) {
        if (from == null || to == null)
            return false;
        if (from == to)
            return true; // Idempotent
        if (to == CANCELLED && from != RESOLVED)
            return true;
        Set<IncidentStatus> allowed = ALLOWED_TRANSITIONS.get(from);
        return allowed != null && allowed.contains(to);
    }
}
