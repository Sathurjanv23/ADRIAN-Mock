package com.nova.emergency.controller;

import com.nova.emergency.dto.ApiResponse;
import com.nova.emergency.model.Incident;
import com.nova.emergency.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final IncidentRepository incidentRepository;
    private final RescueTeamRepository rescueTeamRepository;
    private final ResourceRepository resourceRepository;
    private final HospitalRepository hospitalRepository;

    public AnalyticsController(IncidentRepository incidentRepository, RescueTeamRepository rescueTeamRepository,
                               ResourceRepository resourceRepository, HospitalRepository hospitalRepository) {
        this.incidentRepository = incidentRepository;
        this.rescueTeamRepository = rescueTeamRepository;
        this.resourceRepository = resourceRepository;
        this.hospitalRepository = hospitalRepository;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSummary(
            @RequestParam(defaultValue = "Last 30 Days") String period) {

        List<Incident> allIncidents = incidentRepository.findAll();
        long total = allIncidents.size();
        long critical = allIncidents.stream().filter(i -> "critical".equals(i.getSeverity())).count();
        long resolved = allIncidents.stream().filter(i -> "resolved".equals(i.getStatus()) || "closed".equals(i.getStatus())).count();
        long peopleAssisted = allIncidents.stream().mapToLong(Incident::getPeopleAffected).sum();

        // Incidents by type
        Map<String, Long> byType = new HashMap<>();
        allIncidents.forEach(i -> byType.merge(i.getType(), 1L, Long::sum));

        // Teams deployed
        long teamsDeployed = rescueTeamRepository.findAll().stream()
                .filter(t -> !"available".equals(t.getStatus()) && !"unavailable".equals(t.getStatus()))
                .count();

        Map<String, Object> summary = new HashMap<>();
        summary.put("period", period);
        summary.put("totalIncidents", total);
        summary.put("criticalIncidents", critical);
        summary.put("resolvedIncidents", resolved);
        double avgResponseTime = allIncidents.stream().filter(i -> i.getEta() != null)
            .mapToInt(Incident::getEta).average().orElse(0);
        summary.put("avgResponseTime", avgResponseTime);
        summary.put("peopleAssisted", peopleAssisted);
        summary.put("resourcesDeployed", resourceRepository.findAll().stream().mapToLong(r -> r.getDeployed()).sum());
        summary.put("predictionAccuracy", null);
        summary.put("resolutionRate", total > 0 ? (double) resolved / total * 100 : 0);
        summary.put("teamsDeployed", teamsDeployed);
        summary.put("incidentsByType", byType);

        return ResponseEntity.ok(ApiResponse.ok(summary));
    }

    @GetMapping("/incidents/trends")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getIncidentTrends() {
        Map<String, Object> trends = new HashMap<>();
        trends.put("data", incidentRepository.findAll().stream().map(i -> Map.of(
            "date", i.getReportedAt() != null ? i.getReportedAt() : "",
            "status", i.getStatus() != null ? i.getStatus() : "",
            "severity", i.getSeverity() != null ? i.getSeverity() : ""
        )).toList());
        return ResponseEntity.ok(ApiResponse.ok(trends));
    }

    @GetMapping("/response-times")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getResponseTimes() {
        Map<String, Object> data = new HashMap<>();
        data.put("avgResponseTime", incidentRepository.findAll().stream().filter(i -> i.getEta() != null)
            .mapToInt(Incident::getEta).average().orElse(0));
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @GetMapping("/resources")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getResourceUsage() {
        Map<String, Object> data = new HashMap<>();
        data.put("resources", resourceRepository.findAll());
        return ResponseEntity.ok(ApiResponse.ok(data));
    }
}
