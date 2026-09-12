package com.nova.emergency.controller;

import com.nova.emergency.dto.ApiResponse;
import com.nova.emergency.model.RescueTeam;
import com.nova.emergency.service.RescueTeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rescue-teams")
public class RescueTeamController {

    private final RescueTeamService rescueTeamService;

    public RescueTeamController(RescueTeamService rescueTeamService) {
        this.rescueTeamService = rescueTeamService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<RescueTeam>>> getAll() {
        List<RescueTeam> teams = rescueTeamService.getAll();
        return ResponseEntity.ok(ApiResponse.ok(teams));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RescueTeam>> getById(@PathVariable String id) {
        return rescueTeamService.getById(id)
                .map(t -> ResponseEntity.ok(ApiResponse.ok(t)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<RescueTeam>> updateStatus(
            @PathVariable String id,
            @RequestBody Map<String, String> body) {
        String status = body.get("status");
        RescueTeam updated = rescueTeamService.updateStatus(id, status);
        return ResponseEntity.ok(ApiResponse.ok(updated, "Status updated"));
    }

    @PostMapping("/assign")
    public ResponseEntity<ApiResponse<RescueTeam>> assign(@RequestBody Map<String, String> body) {
        String teamId = body.get("teamId");
        String incidentId = body.get("incidentId");
        RescueTeam updated = rescueTeamService.assign(teamId, incidentId);
        return ResponseEntity.ok(ApiResponse.ok(updated, "Team assigned"));
    }
}
