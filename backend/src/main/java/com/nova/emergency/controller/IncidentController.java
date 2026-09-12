package com.nova.emergency.controller;

import com.nova.emergency.dto.ApiResponse;
import com.nova.emergency.model.Incident;
import com.nova.emergency.service.GridFsStorageService;
import com.nova.emergency.service.IncidentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/incidents")
public class IncidentController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(IncidentController.class);

    private final IncidentService incidentService;
    private final GridFsStorageService gridFsStorageService;

    public IncidentController(IncidentService incidentService, GridFsStorageService gridFsStorageService) {
        this.incidentService = incidentService;
        this.gridFsStorageService = gridFsStorageService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Incident>>> getAll(
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String status) {
        List<Incident> incidents = incidentService.getAll(severity, status);
        ApiResponse<List<Incident>> response = ApiResponse.ok(incidents);
        response.setMeta(new ApiResponse.Meta((long) incidents.size(), null, null));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Incident>> getById(@PathVariable String id) {
        return incidentService.getById(id)
                .map(inc -> ResponseEntity.ok(ApiResponse.ok(inc)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/tracking/{code}")
    public ResponseEntity<ApiResponse<Incident>> getByTrackingCode(@PathVariable String code) {
        return incidentService.getByTrackingCode(code)
                .map(inc -> ResponseEntity.ok(ApiResponse.ok(inc)))
                .orElse(ResponseEntity.notFound().build());
    }

    // ─── Multipart Citizen Emergency Report Submission ───────────

    @PostMapping(value = "/report", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_JSON_VALUE })
    public ResponseEntity<ApiResponse<Incident>> reportEmergency(
            @RequestParam(value = "emergencyType", required = false) String emergencyType,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "latitude", required = false) Double latitude,
            @RequestParam(value = "longitude", required = false) Double longitude,
            @RequestParam(value = "locationAccuracy", required = false) Double locationAccuracy,
            @RequestParam(value = "locationCapturedAt", required = false) String locationCapturedAt,
            @RequestParam(value = "manualAddress", required = false) String manualAddress,
            @RequestParam(value = "reporterName", required = false) String reporterName,
            @RequestParam(value = "reporterPhone", required = false) String reporterPhone,
            @RequestParam(value = "isSilentSos", required = false) Boolean isSilentSos,
            @RequestPart(value = "photo", required = false) MultipartFile photo,
            @RequestPart(value = "audio", required = false) MultipartFile audio,
            Authentication auth
    ) {
        String effectiveType = emergencyType != null ? emergencyType : (type != null ? type : "unknown");
        String userEmail = auth != null ? auth.getName() : "citizen-anonymous";

        Incident created = incidentService.reportEmergency(
            effectiveType,
            description,
            latitude,
            longitude,
            locationAccuracy,
            locationCapturedAt,
            manualAddress,
            reporterName,
            reporterPhone,
            userEmail,
            isSilentSos,
            photo,
            audio
        );

        return ResponseEntity.ok(ApiResponse.ok(created, "Emergency incident submitted successfully"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Incident>> create(
            @RequestBody Map<String, Object> data,
            Authentication auth) {
        String email = auth != null ? auth.getName() : "anonymous";
        Incident created = incidentService.create(data, email);
        return ResponseEntity.ok(ApiResponse.ok(created, "Incident created"));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<Incident>> update(
            @PathVariable String id,
            @RequestBody Map<String, Object> data) {
        Incident updated = incidentService.update(id, data);
        return ResponseEntity.ok(ApiResponse.ok(updated, "Incident updated"));
    }

    @PostMapping("/{incidentId}/assign")
    public ResponseEntity<ApiResponse<Incident>> assignTeam(
            @PathVariable String incidentId,
            @RequestBody Map<String, String> body,
            Authentication auth) {
        String teamId = body.get("teamId");
        String teamName = body.getOrDefault("teamName", teamId);
        String officerEmail = auth != null ? auth.getName() : "Command Center";
        Incident updated = incidentService.assignTeam(incidentId, teamId, teamName, officerEmail);
        return ResponseEntity.ok(ApiResponse.ok(updated, "Team assigned"));
    }

    @PostMapping("/{id}/accept")
    public ResponseEntity<ApiResponse<Incident>> acceptMission(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, String> body,
            Authentication auth) {
        String teamId = body != null ? body.get("teamId") : null;
        if (teamId == null || teamId.isBlank()) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("BAD_REQUEST", "teamId is required to accept a mission."));
        }
        String teamName = body.getOrDefault("teamName", teamId);
        String responderEmail = auth != null ? auth.getName() : body.getOrDefault("officerName", "Rescue Team");
        Incident updated = incidentService.acceptMission(id, teamId, teamName, responderEmail);
        return ResponseEntity.ok(ApiResponse.ok(updated, "Mission accepted by rescue team"));
    }

    @PostMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Incident>> updateMissionStatus(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, String> body,
            Authentication auth) {
        String status = body != null ? body.getOrDefault("status", body.get("newStatus")) : null;
        String teamId = body != null ? body.get("teamId") : null;
        String notes = body != null ? body.get("notes") : null;
        String responderEmail = auth != null ? auth.getName() : "Rescue Responder";
        Incident updated = incidentService.updateMissionStatus(id, status, teamId, notes, responderEmail);
        return ResponseEntity.ok(ApiResponse.ok(updated, "Mission status updated"));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Incident>> patchMissionStatus(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, String> body,
            Authentication auth) {
        return updateMissionStatus(id, body, auth);
    }

    @PostMapping("/{id}/acknowledge")
    public ResponseEntity<ApiResponse<Incident>> acknowledge(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, String> body,
            Authentication auth) {
        String agency = body != null ? body.get("agency") : null;
        String officerName = auth != null ? auth.getName() : (body != null ? body.get("officerName") : "Duty Officer");
        Incident updated = incidentService.acknowledge(id, agency, officerName);
        return ResponseEntity.ok(ApiResponse.ok(updated, "Incident acknowledged"));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<Incident>> cancel(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, String> body,
            Authentication auth) {
        String reason = body != null ? body.get("reason") : "False alarm / Cancelled by citizen";
        String citizenEmail = auth != null ? auth.getName() : "Citizen";
        Incident updated = incidentService.cancel(id, reason, citizenEmail);
        return ResponseEntity.ok(ApiResponse.ok(updated, "Incident cancelled"));
    }

    @PostMapping("/{id}/escalate")
    public ResponseEntity<ApiResponse<Incident>> escalate(
            @PathVariable String id,
            @RequestBody Map<String, String> body,
            Authentication auth) {
        String reason = body.getOrDefault("reason", "Manual escalation");
        String officerEmail = auth != null ? auth.getName() : "Command Center";
        Incident updated = incidentService.escalate(id, reason, officerEmail);
        return ResponseEntity.ok(ApiResponse.ok(updated, "Incident escalated"));
    }

    @PostMapping("/{id}/resolve")
    public ResponseEntity<ApiResponse<Incident>> resolve(
            @PathVariable String id,
            @RequestBody Map<String, String> body,
            Authentication auth) {
        String resolution = body.getOrDefault("resolution", "Incident resolved");
        String officerEmail = auth != null ? auth.getName() : "Command Center";
        Incident updated = incidentService.resolve(id, resolution, officerEmail);
        return ResponseEntity.ok(ApiResponse.ok(updated, "Incident resolved"));
    }

    // ─── Stream Media from MongoDB Atlas GridFS ───────────────────

    @GetMapping("/media/{objectId:.+}")
    public ResponseEntity<StreamingResponseBody> getMedia(@PathVariable String objectId) {
        GridFsResource resource = gridFsStorageService.load(objectId);
        if (resource == null || !resource.exists()) {
            log.warn("GridFS media not found for objectId: {}", objectId);
            return ResponseEntity.notFound().build();
        }

        String contentType = gridFsStorageService.getContentType(objectId);

        StreamingResponseBody body = outputStream -> {
            try (var inputStream = resource.getInputStream()) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }
        };

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + objectId + "\"")
                .body(body);
    }
}
