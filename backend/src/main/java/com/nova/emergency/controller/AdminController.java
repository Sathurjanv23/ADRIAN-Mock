package com.nova.emergency.controller;

import com.nova.emergency.dto.ApiResponse;
import com.nova.emergency.model.AuditLog;
import com.nova.emergency.model.User;
import com.nova.emergency.repository.AuditLogRepository;
import com.nova.emergency.repository.UserRepository;
import com.nova.emergency.repository.RescueTeamRepository;
import com.nova.emergency.model.RescueTeam;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final PasswordEncoder passwordEncoder;
    private final RescueTeamRepository rescueTeamRepository;

    public AdminController(UserRepository userRepository, AuditLogRepository auditLogRepository, PasswordEncoder passwordEncoder, RescueTeamRepository rescueTeamRepository) {
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
        this.passwordEncoder = passwordEncoder;
        this.rescueTeamRepository = rescueTeamRepository;
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<User>>> getUsers() {
        List<User> users = userRepository.findAll();
        // Don't expose password hashes
        users.forEach(u -> u.setPasswordHash(null));
        return ResponseEntity.ok(ApiResponse.ok(users));
    }

    @PostMapping("/users")
    public ResponseEntity<ApiResponse<User>> createUser(@RequestBody Map<String, Object> data) {
        User user = new User();
        user.setName((String) data.get("name"));
        user.setEmail(((String) data.get("email")).toLowerCase());
        user.setPasswordHash(passwordEncoder.encode((String) data.get("password")));
        user.setRole((String) data.get("role"));
        user.setApprovalStatus("citizen".equalsIgnoreCase(user.getRole()) ? "APPROVED" : "PENDING_APPROVAL");
        user.setPhone((String) data.get("phone"));
        user.setDistrict((String) data.get("district"));
        user.setOrganization((String) data.get("organization"));
        user.setLanguage(data.getOrDefault("language", "en").toString());
        user.setCreatedAt(Instant.now().toString());
        user.setLastActive(Instant.now().toString());
        user.setActive(true);
        user.setVerified(true);
        user.setProvider("email");

        User saved = userRepository.save(user);
        saved.setPasswordHash(null);
        return ResponseEntity.ok(ApiResponse.ok(saved, "User created"));
    }

    @PatchMapping("/users/{id}")
    public ResponseEntity<ApiResponse<User>> updateUser(
            @PathVariable String id,
            @RequestBody Map<String, Object> data) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
        if (data.containsKey("name")) user.setName((String) data.get("name"));
        if (data.containsKey("role")) user.setRole((String) data.get("role"));
        if (data.containsKey("approvalStatus")) {
            String approvalStatus = (String) data.get("approvalStatus");
            String requestedTeamId = data.containsKey("rescueTeamId") ? (String) data.get("rescueTeamId") : user.getRescueTeamId();
            if ("APPROVED".equalsIgnoreCase(approvalStatus) && "rescue_team".equals(user.getRole())
                && (requestedTeamId == null || rescueTeamRepository.findById(requestedTeamId).isEmpty())) {
                throw new IllegalArgumentException("An approved rescue user must have a valid rescueTeamId.");
            }
            user.setApprovalStatus(approvalStatus);
        }
        if (data.containsKey("district")) user.setDistrict((String) data.get("district"));
        if (data.containsKey("isActive")) user.setActive((Boolean) data.get("isActive"));
        if (data.containsKey("rescueTeamId")) {
            String teamId = (String) data.get("rescueTeamId");
            if (!"rescue_team".equals(user.getRole())) {
                throw new IllegalArgumentException("Only rescue users may be linked to a rescue team.");
            }
            if (teamId == null || rescueTeamRepository.findById(teamId).isEmpty()) {
                throw new IllegalArgumentException("Selected rescue team does not exist.");
            }
            user.setRescueTeamId(teamId);
        }
        User saved = userRepository.save(user);
        saved.setPasswordHash(null);
        return ResponseEntity.ok(ApiResponse.ok(saved, "User updated"));
    }

    @PostMapping("/users/{id}/approval")
    public ResponseEntity<ApiResponse<User>> approveUser(@PathVariable String id, @RequestBody Map<String, String> data) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
        String decision = data.get("decision");
        if (!"APPROVE".equalsIgnoreCase(decision) && !"REJECT".equalsIgnoreCase(decision)) {
            throw new IllegalArgumentException("Decision must be APPROVE or REJECT.");
        }
        if ("APPROVE".equalsIgnoreCase(decision) && "rescue_team".equals(user.getRole())) {
            String teamId = data.get("rescueTeamId");
            if (teamId == null || rescueTeamRepository.findById(teamId).isEmpty()) {
                throw new IllegalArgumentException("A valid rescueTeamId is required for rescue approval.");
            }
            user.setRescueTeamId(teamId);
        }
        if ("REJECT".equalsIgnoreCase(decision)) {
            user.setRescueTeamId(null);
        }
        user.setApprovalStatus("APPROVE".equalsIgnoreCase(decision) ? "APPROVED" : "REJECTED");
        if ("APPROVE".equalsIgnoreCase(decision)) user.setStatus("ACTIVE");
        User saved = userRepository.save(user);
        saved.setPasswordHash(null);
        return ResponseEntity.ok(ApiResponse.ok(saved, "User approval updated"));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<ApiResponse<String>> deleteUser(@PathVariable String id) {
        userRepository.deleteById(id);
        return ResponseEntity.ok(ApiResponse.ok("Deleted", "User deleted"));
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<ApiResponse<List<AuditLog>>> getAuditLogs() {
        return ResponseEntity.ok(ApiResponse.ok(auditLogRepository.findAll()));
    }

    @GetMapping("/system/health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSystemHealth() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("database", "MongoDB Atlas — Connected");
        health.put("timestamp", Instant.now().toString());
        health.put("userCount", userRepository.count());
        health.put("incidentCount", 0);
        return ResponseEntity.ok(ApiResponse.ok(health));
    }
}
