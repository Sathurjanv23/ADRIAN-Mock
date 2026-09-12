package com.nova.emergency.controller;

import com.nova.emergency.dto.ApiResponse;
import com.nova.emergency.model.Notification;
import com.nova.emergency.model.Incident;
import com.nova.emergency.repository.IncidentRepository;
import com.nova.emergency.model.RiskPrediction;
import com.nova.emergency.repository.NotificationRepository;
import com.nova.emergency.repository.RiskPredictionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AlertController — Provides live alert data for the top ticker and alert panel.
 *
 * GET /api/alerts/live — Returns current live alerts from real backend data.
 * No hardcoded/mock alerts. If no alerts exist, returns empty list.
 */
@RestController
@RequestMapping("/api/alerts")
public class AlertController {

    private final NotificationRepository notificationRepository;
    private final RiskPredictionRepository riskPredictionRepository;
    private final IncidentRepository incidentRepository;

    public AlertController(NotificationRepository notificationRepository, RiskPredictionRepository riskPredictionRepository, IncidentRepository incidentRepository) {
        this.notificationRepository = notificationRepository;
        this.riskPredictionRepository = riskPredictionRepository;
        this.incidentRepository = incidentRepository;
    }

    /**
     * Returns the most recent unread alerts for the top ticker.
     * Aggregates notifications + active risk predictions.
     * No mock data — returns empty list if nothing in DB.
     */
    @GetMapping("/live")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getLiveAlerts() {
        List<Map<String, Object>> alerts = new ArrayList<>();
        java.util.Set<String> seenIds = new java.util.HashSet<>();

        // 1. Critical active incidents
        incidentRepository.findAll().stream()
            .filter(i -> "critical".equalsIgnoreCase(i.getSeverity()) && !isClosed(i.getStatus()))
            .forEach(i -> {
                String id = i.getId() != null ? i.getId() : "";
                if (!id.isEmpty() && seenIds.add(id)) {
                    java.util.Map<String, Object> map = new java.util.HashMap<>();
                    map.put("id", id);
                    map.put("type", "critical_incident");
                    map.put("title", i.getTitle() != null ? i.getTitle() : "Critical Emergency");
                    map.put("message", i.getDescription() != null ? i.getDescription() : "Immediate emergency response dispatched.");
                    map.put("severity", "critical");
                    map.put("incidentId", id);
                    map.put("trackingCode", i.getTrackingCode() != null ? i.getTrackingCode() : "");
                    map.put("timestamp", i.getUpdatedAt() != null ? i.getUpdatedAt() : (i.getReportedAt() != null ? i.getReportedAt() : ""));
                    alerts.add(map);
                }
            });

        // 2. Unread notifications
        List<Notification> notifications = notificationRepository.findByReadFalseOrderByCreatedAtDesc();
        notifications.forEach(n -> {
            String id = n.getId() != null ? n.getId() : "";
            String relatedId = n.getRelatedId() != null ? n.getRelatedId() : "";
            if ((id.isEmpty() || seenIds.add(id)) && (relatedId.isEmpty() || seenIds.add(relatedId))) {
                java.util.Map<String, Object> map = new java.util.HashMap<>();
                map.put("id", id);
                map.put("type", n.getType() != null ? n.getType() : "notification");
                map.put("title", n.getTitle() != null && !n.getTitle().isBlank() ? n.getTitle() : "Emergency Notification");
                map.put("message", n.getMessage() != null ? n.getMessage() : "");
                map.put("severity", n.getSeverity() != null ? n.getSeverity().toLowerCase() : "medium");
                map.put("timestamp", n.getCreatedAt() != null ? n.getCreatedAt() : "");
                if (!relatedId.isEmpty()) {
                    map.put("relatedId", relatedId);
                }
                alerts.add(map);
            }
        });

        // 3. High risk predictions
        List<RiskPrediction> riskPredictions = riskPredictionRepository.findAll();
        riskPredictions.stream()
            .filter(rp -> rp.getFloodRisk() >= 70)
            .forEach(rp -> {
                String id = rp.getId() != null ? rp.getId() : "";
                if (id.isEmpty() || seenIds.add(id)) {
                    java.util.Map<String, Object> map = new java.util.HashMap<>();
                    map.put("id", id);
                    map.put("type", "risk_prediction");
                    map.put("title", "High Risk Warning: " + (rp.getZone() != null ? rp.getZone() : "Critical Zone"));
                    map.put("message", buildRiskMessage(rp));
                    map.put("severity", rp.getRiskLevel() != null ? rp.getRiskLevel().toLowerCase() : "high");
                    map.put("timestamp", rp.getUpdatedAt() != null ? rp.getUpdatedAt() : "");
                    alerts.add(map);
                }
            });

        // Sort by timestamp descending (most recent first)
        alerts.sort((a, b) -> {
            String ta = (String) a.getOrDefault("timestamp", "");
            String tb = (String) b.getOrDefault("timestamp", "");
            return tb.compareTo(ta);
        });

        return ResponseEntity.ok(ApiResponse.ok(alerts));
    }

    private boolean isClosed(String status) {
        return "resolved".equalsIgnoreCase(status) || "closed".equalsIgnoreCase(status) || "cancelled".equalsIgnoreCase(status);
    }

    private String buildRiskMessage(RiskPrediction rp) {
        StringBuilder msg = new StringBuilder();
        if (rp.getZone() != null) msg.append(rp.getZone()).append(": ");
        if (rp.getFloodRisk() >= 70) {
            msg.append("Flood risk ").append(rp.getFloodRisk()).append("%");
        }
        if (rp.getRiverLevel() > 0) {
            msg.append(" — River level ").append(String.format("%.1fm", rp.getRiverLevel()));
        }
        return msg.length() > 0 ? msg.toString() : "Elevated risk detected";
    }
}
