package com.nova.emergency.service;

import com.nova.emergency.model.RescueTeam;
import com.nova.emergency.repository.RescueTeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class RescueTeamService {

    private final RescueTeamRepository rescueTeamRepository;

    public RescueTeamService(RescueTeamRepository rescueTeamRepository) {
        this.rescueTeamRepository = rescueTeamRepository;
    }

    public List<RescueTeam> getAll() {
        return rescueTeamRepository.findAll();
    }

    public Optional<RescueTeam> getById(String id) {
        return rescueTeamRepository.findById(id);
    }

    public RescueTeam updateStatus(String id, String status) {
        RescueTeam team = rescueTeamRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Rescue team not found: " + id));
        team.setStatus(status);
        team.setLastUpdated(Instant.now().toString());
        return rescueTeamRepository.save(team);
    }

    public RescueTeam assign(String teamId, String incidentId) {
        RescueTeam team = rescueTeamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Rescue team not found: " + teamId));
        team.setStatus("assigned");
        team.setCurrentIncident(incidentId);
        team.setLastUpdated(Instant.now().toString());
        return rescueTeamRepository.save(team);
    }

    public RescueTeam update(String id, Map<String, Object> data) {
        RescueTeam team = rescueTeamRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Rescue team not found: " + id));
        if (data.containsKey("status"))
            team.setStatus((String) data.get("status"));
        if (data.containsKey("currentIncident"))
            team.setCurrentIncident((String) data.get("currentIncident"));
        if (data.containsKey("eta"))
            team.setEta((Integer) data.get("eta"));
        team.setLastUpdated(Instant.now().toString());
        return rescueTeamRepository.save(team);
    }
}
