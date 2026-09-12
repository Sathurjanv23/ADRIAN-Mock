package com.nova.emergency.service;

import com.nova.emergency.controller.SseController;
import com.nova.emergency.exception.*;
import com.nova.emergency.model.AuditLog;
import com.nova.emergency.model.Hospital;
import com.nova.emergency.model.Incident;
import com.nova.emergency.model.IncidentStatus;
import com.nova.emergency.model.RescueTeam;
import com.nova.emergency.repository.IncidentRepository;
import com.nova.emergency.repository.RescueTeamRepository;
import com.nova.emergency.repository.HospitalRepository;
import com.nova.emergency.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.*;

@Service
public class IncidentService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(IncidentService.class);

    private final IncidentRepository incidentRepository;
    private final RescueTeamRepository rescueTeamRepository;
    private final HospitalRepository hospitalRepository;
    private final AuditLogRepository auditLogRepository;
    private final com.nova.emergency.repository.NotificationRepository notificationRepository;
    private final GridFsStorageService gridFsStorageService;
    private final ReliefService reliefService;

    public IncidentService(IncidentRepository incidentRepository, RescueTeamRepository rescueTeamRepository,
                           HospitalRepository hospitalRepository, AuditLogRepository auditLogRepository,
                           com.nova.emergency.repository.NotificationRepository notificationRepository,
                           GridFsStorageService gridFsStorageService,
                           @org.springframework.context.annotation.Lazy ReliefService reliefService) {
        this.incidentRepository = incidentRepository;
        this.rescueTeamRepository = rescueTeamRepository;
        this.hospitalRepository = hospitalRepository;
        this.auditLogRepository = auditLogRepository;
        this.notificationRepository = notificationRepository;
        this.gridFsStorageService = gridFsStorageService;
        this.reliefService = reliefService;
    }

    // ─── Emergency Routing Rules Matrix ───────────────────────────
    private static final Map<String, List<String>> ROUTING_TABLE = Map.ofEntries(
        Map.entry("medical", List.of("command_centre", "ambulance", "hospital")),
        Map.entry("road_accident", List.of("command_centre", "ambulance", "police", "hospital", "search_rescue")),
        Map.entry("fire", List.of("command_centre", "fire_rescue", "ambulance", "police", "hospital")),
        Map.entry("crime", List.of("command_centre", "police", "ambulance")),
        Map.entry("missing_person", List.of("command_centre", "police", "search_rescue")),
        Map.entry("building_collapse", List.of("command_centre", "fire_rescue", "ambulance", "police", "hospital")),
        Map.entry("flood", List.of("command_centre", "disaster_response", "search_rescue", "police", "ambulance", "hospital")),
        Map.entry("landslide", List.of("command_centre", "disaster_response", "search_rescue", "police", "ambulance", "hospital")),
        Map.entry("severe_weather", List.of("command_centre", "disaster_response", "search_rescue")),
        Map.entry("other", List.of("command_centre", "police", "ambulance", "search_rescue")),
        Map.entry("unknown", List.of("command_centre", "police", "ambulance", "hospital", "search_rescue"))
    );

    public List<Incident> getAll(String severity, String status) {
        if (severity != null && !severity.isEmpty()) {
            return incidentRepository.findBySeverityOrderByPriorityAsc(severity);
        }
        if (status != null && !status.isEmpty()) {
            return incidentRepository.findByStatusOrderByPriorityAsc(status);
        }
        return incidentRepository.findAll();
    }

    public Incident resolveIncident(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            throw new IncidentNotFoundException("Incident identifier cannot be null or empty.");
        }
        // If identifier is a valid database ID, try findById.
        Optional<Incident> byId = incidentRepository.findById(identifier);
        if (byId.isPresent()) return byId.get();

        // Otherwise, or if not found, try findByTrackingCode.
        Optional<Incident> byTracking = incidentRepository.findByTrackingCode(identifier);
        if (byTracking.isPresent()) return byTracking.get();

        // Fallback match on trackingCode or ID across loaded entities (case-insensitive)
        Optional<Incident> fallback = incidentRepository.findAll().stream()
                .filter(i -> identifier.equalsIgnoreCase(i.getTrackingCode()) || identifier.equalsIgnoreCase(i.getId()))
                .findFirst();
        if (fallback.isPresent()) return fallback.get();

        // If neither exists, throw IncidentNotFoundException.
        throw new IncidentNotFoundException("The requested incident '" + identifier + "' could not be found.");
    }

    public Optional<Incident> findIncident(String idOrTrackingCode) {
        try {
            return Optional.of(resolveIncident(idOrTrackingCode));
        } catch (ResourceNotFoundException ex) {
            return Optional.empty();
        }
    }

    public Incident findIncidentOrThrow(String idOrTrackingCode) {
        return resolveIncident(idOrTrackingCode);
    }

    public Optional<Incident> getById(String id) {
        return findIncident(id);
    }

    public Optional<Incident> getByTrackingCode(String trackingCode) {
        return findIncident(trackingCode);
    }

    // ─── Comprehensive Emergency Report Submission (Multipart & JSON) ─

    public Incident reportEmergency(
            String type,
            String description,
            Double latitude,
            Double longitude,
            Double locationAccuracy,
            String locationCapturedAt,
            String manualAddress,
            String reporterName,
            String reporterPhone,
            String reportedByEmail,
            Boolean isSilentSos,
            MultipartFile photoFile,
            MultipartFile audioFile
    ) {
        Incident incident = new Incident();
        String trackingCode = "NOV-" + (int)(Math.random() * 9000 + 1000);
        incident.setId(trackingCode);
        incident.setTrackingCode(trackingCode);
        incident.setType(type != null ? type.toLowerCase().trim() : "unknown");
        incident.setDescription(description != null ? description.trim() : "");
        incident.setTitle((incident.getDescription().length() > 60
                ? incident.getDescription().substring(0, 60) + "..."
                : incident.getDescription().isEmpty()
                ? incident.getType().replace('_', ' ') + " Emergency"
                : incident.getDescription()));

        incident.setSeverity(determineDefaultSeverity(incident.getType(), isSilentSos));
        incident.setStatus("submitted");
        incident.setReportedAt(Instant.now().toString());
        incident.setUpdatedAt(Instant.now().toString());
        incident.setReporterName(reporterName != null && !reporterName.isBlank() ? reporterName : "Citizen Witness");
        incident.setReporterPhone(reporterPhone);
        incident.setReportedBy(reportedByEmail != null ? reportedByEmail : "anonymous-citizen");
        incident.setSilentSos(Boolean.TRUE.equals(isSilentSos));
        incident.setSimulation(false); // Real citizen report — not a simulation

        // Location
        Incident.GeoLocation geo = new Incident.GeoLocation();
        double lat = latitude != null ? latitude : 6.9271; // Default Colombo coordinates
        double lng = longitude != null ? longitude : 79.8612;
        geo.setLat(lat);
        geo.setLng(lng);
        geo.setAccuracy(locationAccuracy);
        geo.setCapturedAt(locationCapturedAt != null ? locationCapturedAt : Instant.now().toString());
        geo.setAddress(manualAddress != null && !manualAddress.isBlank() ? manualAddress : "GPS (" + lat + ", " + lng + ")");
        geo.setDistrict("Colombo");
        incident.setLocation(geo);
        incident.setManualAddress(manualAddress);
        incident.setLocationAccuracy(locationAccuracy);
        incident.setLocationCapturedAt(locationCapturedAt);

        // Handle File Uploads — stored in MongoDB Atlas GridFS (no local filesystem)
        List<Incident.Attachment> attachments = new ArrayList<>();
        List<String> photoUrls = new ArrayList<>();

        if (photoFile != null && !photoFile.isEmpty()) {
            String objectId = gridFsStorageService.store(photoFile, "photo");
            if (objectId != null) {
                String photoUrl = "/api/incidents/media/" + objectId;
                photoUrls.add(photoUrl);
                attachments.add(new Incident.Attachment(
                    UUID.randomUUID().toString(),
                    "image",
                    photoUrl,
                    photoFile.getOriginalFilename() != null ? photoFile.getOriginalFilename() : objectId,
                    photoFile.getSize()
                ));
                log.info("Photo stored in MongoDB GridFS: objectId={}", objectId);
            }
        }
        incident.setPhotoUrls(photoUrls);

        if (audioFile != null && !audioFile.isEmpty()) {
            String objectId = gridFsStorageService.store(audioFile, "audio");
            if (objectId != null) {
                String audioUrl = "/api/incidents/media/" + objectId;
                incident.setAudioUrl(audioUrl);
                attachments.add(new Incident.Attachment(
                    UUID.randomUUID().toString(),
                    "audio",
                    audioUrl,
                    audioFile.getOriginalFilename() != null ? audioFile.getOriginalFilename() : objectId,
                    audioFile.getSize()
                ));
                log.info("Audio stored in MongoDB GridFS: objectId={}", objectId);
            }
        }
        incident.setAttachments(attachments);

        // Recommended Agencies & Authority Notifications
        List<String> recommendedAgencies = ROUTING_TABLE.getOrDefault(incident.getType(), ROUTING_TABLE.get("unknown"));
        incident.setRecommendedAgencies(recommendedAgencies);

        List<Incident.AgencyNotification> notifications = new ArrayList<>();
        String now = Instant.now().toString();
        for (String agency : recommendedAgencies) {
            notifications.add(new Incident.AgencyNotification(
                "notif-" + agency + "-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 1000),
                agency,
                formatAgencyName(agency),
                agency + "@nova.emergency.lk",
                "portal",
                now,
                "delivered",
                null,
                null,
                null
            ));
        }
        incident.setNotifiedAgencies(notifications);

        // Nearest Responders Assignment (Haversine Formula)
        assignNearestResponders(incident, lat, lng);

        // Updates Timeline
        List<Incident.IncidentUpdate> updates = new ArrayList<>();
        updates.add(new Incident.IncidentUpdate(
            "upd-init-" + System.currentTimeMillis(),
            "reported",
            "Emergency report received. Multi-agency notification bus dispatched.",
            "PROJECT NOVA Automated Dispatcher",
            now,
            true
        ));
        incident.setUpdates(updates);

        Incident saved = incidentRepository.save(incident);
        log.info("New emergency incident created: {} (Tracking: {}, Type: {}, Severity: {})",
                saved.getId(), saved.getTrackingCode(), saved.getType(), saved.getSeverity());

        // Save real-time alert notification directly in MongoDB Atlas for Rescue Teams, Hospitals & Command Center
        try {
            com.nova.emergency.model.Notification notif = new com.nova.emergency.model.Notification();
            notif.setType("critical_incident");
            notif.setTitle("🚨 EMERGENCY REPORTED: " + saved.getTrackingCode());
            notif.setMessage(saved.getSeverity().toUpperCase() + " " + saved.getType().replace('_', ' ').toUpperCase() +
                             " emergency reported at " + saved.getLocation().getAddress() + ". Dispatching rescue and hospital triage.");
            notif.setSeverity(saved.getSeverity());
            notif.setRead(false);
            notif.setCreatedAt(now);
            notif.setRelatedId(saved.getId());
            notif.setRelatedType("incident");
            notif.setTargetRole(List.of("rescue_team", "hospital", "officer", "admin"));
            notificationRepository.save(notif);
        } catch (Exception ex) {
            log.warn("Could not save alert notification to MongoDB: {}", ex.getMessage());
        }

        // Auto-trigger Integrated AI Relief Logistics when disaster detected or people affected
        try {
            if (reliefService != null && (Set.of("flood", "fire", "earthquake", "landslide", "building_collapse", "severe_weather").contains(saved.getType())
                    || saved.getPeopleAffected() > 0)) {
                reliefService.autoTriggerReliefForIncident(saved);
            }
        } catch (Exception ex) {
            log.warn("Automatic relief logistics trigger failed for incident {}: {}", saved.getId(), ex.getMessage());
        }

        // Broadcast real-time SSE event to all portals
        SseController.broadcastIncidentCreated(saved);

        return saved;
    }

    public Incident create(Map<String, Object> data, String reportedByEmail) {
        String type = (String) data.getOrDefault("type", "other");
        String description = (String) data.getOrDefault("description", "");
        Double lat = null;
        Double lng = null;
        Double accuracy = null;
        String manualAddress = (String) data.get("manualAddress");
        String reporterName = (String) data.get("reporterName");
        String reporterPhone = (String) data.get("reporterPhone");
        Boolean isSilentSos = (Boolean) data.get("isSilentSos");

        if (data.containsKey("location") && data.get("location") instanceof Map) {
            Map<?, ?> locMap = (Map<?, ?>) data.get("location");
            if (locMap.containsKey("lat")) lat = ((Number) locMap.get("lat")).doubleValue();
            if (locMap.containsKey("lng")) lng = ((Number) locMap.get("lng")).doubleValue();
            if (locMap.containsKey("accuracy")) accuracy = ((Number) locMap.get("accuracy")).doubleValue();
            if (locMap.containsKey("address") && manualAddress == null) manualAddress = (String) locMap.get("address");
        } else {
            if (data.containsKey("latitude")) lat = ((Number) data.get("latitude")).doubleValue();
            if (data.containsKey("longitude")) lng = ((Number) data.get("longitude")).doubleValue();
            if (data.containsKey("locationAccuracy")) accuracy = ((Number) data.get("locationAccuracy")).doubleValue();
        }

        return reportEmergency(type, description, lat, lng, accuracy, Instant.now().toString(),
                manualAddress, reporterName, reporterPhone, reportedByEmail, isSilentSos, null, null);
    }

    public Incident update(String id, Map<String, Object> data) {
        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Incident not found: " + id));

        if (data.containsKey("status")) incident.setStatus((String) data.get("status"));
        if (data.containsKey("severity")) incident.setSeverity((String) data.get("severity"));
        if (data.containsKey("assignedTeam")) incident.setAssignedTeam((String) data.get("assignedTeam"));
        if (data.containsKey("assignedTeamName")) incident.setAssignedTeamName((String) data.get("assignedTeamName"));
        if (data.containsKey("eta")) incident.setEta(((Number) data.get("eta")).intValue());
        if (data.containsKey("priority")) incident.setPriority(((Number) data.get("priority")).intValue());
        incident.setUpdatedAt(Instant.now().toString());

        Incident updated = incidentRepository.save(incident);
        SseController.broadcastIncidentUpdated(updated);
        return updated;
    }

    public Incident assignTeam(String incidentId, String teamId, String teamName, String officerEmail) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new IllegalArgumentException("Incident not found: " + incidentId));

        incident.setAssignedTeam(teamId);
        incident.setAssignedTeamName(teamName);
        incident.setStatus("assigned");
        incident.setUpdatedAt(Instant.now().toString());

        Incident.IncidentUpdate updateEntry = new Incident.IncidentUpdate(
            UUID.randomUUID().toString(),
            "assigned",
            "Team " + teamName + " assigned to this incident.",
            officerEmail != null ? officerEmail : "Command Center",
            Instant.now().toString(),
            false
        );
        if (incident.getUpdates() != null) {
            incident.getUpdates().add(updateEntry);
        }

        logAudit(officerEmail, "ASSIGN_TEAM", "incident", incidentId,
            "Assigned team " + teamName + " to incident " + incidentId);

        Incident saved = incidentRepository.save(incident);
        SseController.broadcastIncidentUpdated(saved);
        return saved;
    }

    public Incident acknowledge(String incidentId, String agency, String officerName) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new IllegalArgumentException("Incident not found: " + incidentId));

        String now = Instant.now().toString();
        if (incident.getNotifiedAgencies() != null) {
            for (Incident.AgencyNotification notif : incident.getNotifiedAgencies()) {
                if (agency == null || agency.equalsIgnoreCase(notif.getAgency())) {
                    notif.setStatus("acknowledged");
                    notif.setAcknowledgedAt(now);
                    notif.setAcknowledgedBy(officerName != null ? officerName : "Officer On Duty");
                }
            }
        }

        if ("reported".equalsIgnoreCase(incident.getStatus()) || "submitted".equalsIgnoreCase(incident.getStatus())) {
            incident.setStatus("verified");
        }
        incident.setUpdatedAt(now);

        if (incident.getUpdates() != null) {
            incident.getUpdates().add(new Incident.IncidentUpdate(
                UUID.randomUUID().toString(),
                "verified",
                "Incident acknowledged by " + (agency != null ? formatAgencyName(agency) : "Command Center"),
                officerName != null ? officerName : "Officer",
                now,
                false
            ));
        }

        logAudit(officerName, "ACKNOWLEDGE_INCIDENT", "incident", incidentId,
                "Acknowledged by agency: " + agency);

        Incident saved = incidentRepository.save(incident);
        SseController.broadcastIncidentUpdated(saved);
        return saved;
    }

    public synchronized Incident acceptMission(String incidentId, String teamId, String teamName, String responderEmail) {
        Incident incident = resolveIncident(incidentId);

        if (teamId == null || teamId.isBlank()) {
            throw new IllegalArgumentException("Team ID is required to accept a mission.");
        }

        // --- Guard 1: Idempotent repeated accept by the same team (check first) ---
        // If this team already owns this incident and it's in any active state, return current state.
        if (teamId.equalsIgnoreCase(incident.getAssignedTeamId())) {
            IncidentStatus currentStatus = IncidentStatus.fromString(incident.getStatus());
            if (currentStatus == IncidentStatus.ACKNOWLEDGED
                    || currentStatus == IncidentStatus.EN_ROUTE
                    || currentStatus == IncidentStatus.ON_SCENE
                    || currentStatus == IncidentStatus.TRANSPORTING) {
                log.info("Idempotent accept: team {} already owns incident {}", teamId, incidentId);
                return incident;
            }
        }

        // --- Guard 2: Incident already assigned to a different team ---
        if (incident.getAssignedTeamId() != null && !incident.getAssignedTeamId().equalsIgnoreCase(teamId)) {
            IncidentStatus incStatus = IncidentStatus.fromString(incident.getStatus());
            if (incStatus == IncidentStatus.ACKNOWLEDGED
                    || incStatus == IncidentStatus.EN_ROUTE
                    || incStatus == IncidentStatus.ON_SCENE
                    || incStatus == IncidentStatus.TRANSPORTING) {
                throw new IllegalStateException("INCIDENT_ALREADY_ASSIGNED: Mission has already been accepted by team: " + incident.getAssignedTeamName());
            }
        }

        // --- Guard 3: Prevent accepting already resolved or cancelled missions ---
        if ("resolved".equalsIgnoreCase(incident.getStatus()) || "cancelled".equalsIgnoreCase(incident.getStatus())) {
            throw new IllegalStateException("INVALID_STATUS_TRANSITION: Cannot accept mission in '" + incident.getStatus() + "' state.");
        }

        // --- Guard 4: Load the rescue team and check its availability ---
        RescueTeam team = rescueTeamRepository.findById(teamId).orElse(null);
        if (team != null) {
            String teamStatus = team.getStatus();
            if ("unavailable".equalsIgnoreCase(teamStatus)) {
                throw new IllegalStateException("TEAM_BUSY: Rescue team '" + team.getName() + "' is currently unavailable.");
            }
            // Team is busy with a DIFFERENT incident
            if (team.getCurrentIncident() != null
                    && !team.getCurrentIncident().equalsIgnoreCase(incident.getId())
                    && !team.getCurrentIncident().equalsIgnoreCase(incident.getTrackingCode()
            )) {
                throw new IllegalStateException("TEAM_BUSY: Rescue team '" + team.getName() + "' is currently busy with another active mission: " + team.getCurrentIncident());
            }
        }

        // --- Guard 5: Validate state machine transition ---
        IncidentStatus currentStatus = IncidentStatus.fromString(incident.getStatus());
        if (!IncidentStatus.isValidTransition(currentStatus, IncidentStatus.ACKNOWLEDGED)) {
            throw new InvalidStatusTransitionException("INVALID_STATUS_TRANSITION: Cannot accept mission in status '" + currentStatus.getValue() + "'.");
        }

        String now = Instant.now().toString();
        String effectiveTeamName = (team != null && team.getName() != null && !team.getName().isBlank())
                ? team.getName()
                : (teamName != null && !teamName.isBlank() ? teamName : teamId);

        incident.setStatus(IncidentStatus.ACKNOWLEDGED.getValue());
        incident.setAssignedTeamId(teamId);
        incident.setAssignedTeamName(effectiveTeamName);
        incident.setAssignedTeam(effectiveTeamName);
        incident.setAcknowledgedAt(now);
        incident.setAcknowledgedBy(responderEmail != null ? responderEmail : effectiveTeamName);
        incident.setUpdatedAt(now);

        if (incident.getUpdates() == null) {
            incident.setUpdates(new ArrayList<>());
        } else {
            incident.setUpdates(new ArrayList<>(incident.getUpdates()));
        }
        incident.getUpdates().add(new Incident.IncidentUpdate(
            UUID.randomUUID().toString(),
            IncidentStatus.ACKNOWLEDGED.getValue(),
            "Mission accepted by " + effectiveTeamName + ". Deployment initiated.",
            responderEmail != null ? responderEmail : effectiveTeamName,
            now,
            false
        ));

        // Update RescueTeam entity
        if (team != null) {
            team.setStatus("assigned");
            team.setCurrentIncident(incident.getId());
            team.setLastUpdated(now);
            rescueTeamRepository.save(team);
        }

        logAudit(responderEmail, "ACCEPT_MISSION", "incident", incident.getId(),
                "Mission accepted by " + effectiveTeamName);

        Incident saved = incidentRepository.save(incident);
        SseController.broadcastIncidentUpdated(saved);
        return saved;
    }

    public synchronized Incident updateMissionStatus(String incidentId, String status, String teamId, String notes, String responderEmail) {
        Incident incident = resolveIncident(incidentId);

        if (status == null || status.isBlank()) {
            throw new InvalidStatusException("Status field is required.");
        }

        IncidentStatus targetStatus = IncidentStatus.fromString(status);
        IncidentStatus currentStatus = IncidentStatus.fromString(incident.getStatus());

        // Validate state machine transition
        if (!IncidentStatus.isValidTransition(currentStatus, targetStatus)) {
            throw new InvalidStatusTransitionException("Cannot transition incident status from '" + currentStatus.getValue() + "' to '" + targetStatus.getValue() + "'.");
        }

        // Verify team authorization if teamId is provided and incident is assigned to a specific squad
        if (teamId != null && incident.getAssignedTeamId() != null
                && !incident.getAssignedTeamId().equalsIgnoreCase(teamId)
                && !teamId.equalsIgnoreCase("command_centre")
                && !teamId.equalsIgnoreCase("admin")
                && !teamId.equalsIgnoreCase("system")) {
            throw new UnauthorizedMissionUpdateException("Rescue team '" + teamId + "' is not authorized to update mission assigned to '" + incident.getAssignedTeamName() + "'.");
        }

        String now = Instant.now().toString();
        incident.setStatus(targetStatus.getValue());
        incident.setUpdatedAt(now);

        if (targetStatus == IncidentStatus.ON_SCENE && incident.getArrivedAt() == null) {
            incident.setArrivedAt(now);
        }
        if (targetStatus == IncidentStatus.RESOLVED) {
            incident.setResolvedAt(now);
        }

        String noteText = notes != null && !notes.isBlank()
                ? notes
                : switch (targetStatus) {
                    case EN_ROUTE -> "Rescue unit is en route to incident location.";
                    case ON_SCENE -> "Rescue unit has arrived on scene.";
                    case TRANSPORTING -> "Victims/patients in transit to emergency triage hospital.";
                    case RESOLVED -> "Incident mission completed and resolved.";
                    default -> "Mission status updated to " + targetStatus.getValue();
                };

        if (incident.getUpdates() == null) {
            incident.setUpdates(new ArrayList<>());
        } else {
            incident.setUpdates(new ArrayList<>(incident.getUpdates()));
        }
        incident.getUpdates().add(new Incident.IncidentUpdate(
            UUID.randomUUID().toString(),
            targetStatus.getValue(),
            noteText,
            responderEmail != null ? responderEmail : "Rescue Team",
            now,
            false
        ));

        // Update corresponding RescueTeam entity
        String effectiveTeamId = teamId != null ? teamId : incident.getAssignedTeamId();
        if (effectiveTeamId != null) {
            rescueTeamRepository.findById(effectiveTeamId).ifPresent(team -> {
                if (targetStatus == IncidentStatus.RESOLVED) {
                    team.setStatus("available");
                    team.setCurrentIncident(null);
                } else if (targetStatus == IncidentStatus.ON_SCENE) {
                    team.setStatus("on_scene");
                } else if (targetStatus == IncidentStatus.EN_ROUTE) {
                    team.setStatus("en_route");
                }
                team.setLastUpdated(now);
                rescueTeamRepository.save(team);
            });
        }

        logAudit(responderEmail, "UPDATE_MISSION_STATUS", "incident", incident.getId(),
                "Status updated to " + targetStatus.getValue() + ": " + noteText);

        Incident saved = incidentRepository.save(incident);
        SseController.broadcastIncidentUpdated(saved);
        return saved;
    }

    public Incident cancel(String incidentId, String reason, String citizenEmail) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new IllegalArgumentException("Incident not found: " + incidentId));

        incident.setStatus("cancelled");
        incident.setUpdatedAt(Instant.now().toString());

        if (incident.getUpdates() != null) {
            incident.getUpdates().add(new Incident.IncidentUpdate(
                UUID.randomUUID().toString(),
                "cancelled",
                "Incident cancelled by reporter: " + (reason != null ? reason : "False alarm"),
                citizenEmail != null ? citizenEmail : "Citizen",
                Instant.now().toString(),
                false
            ));
        }

        logAudit(citizenEmail, "CANCEL_INCIDENT", "incident", incidentId, "Cancelled: " + reason);
        Incident saved = incidentRepository.save(incident);
        SseController.broadcastIncidentUpdated(saved);
        return saved;
    }

    public Incident escalate(String id, String reason, String officerEmail) {
        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Incident not found: " + id));

        String currentSeverity = incident.getSeverity() != null ? incident.getSeverity() : "medium";
        String newSeverity = switch (currentSeverity) {
            case "low" -> "medium";
            case "medium" -> "high";
            case "high" -> "critical";
            default -> "critical";
        };
        incident.setSeverity(newSeverity);
        incident.setUpdatedAt(Instant.now().toString());

        logAudit(officerEmail, "ESCALATE_INCIDENT", "incident", id,
            "Escalated from " + currentSeverity + " to " + newSeverity + ". Reason: " + reason);

        Incident escalated = incidentRepository.save(incident);
        SseController.broadcastIncidentUpdated(escalated);
        return escalated;
    }

    public Incident resolve(String id, String resolution, String officerEmail) {
        Incident incident = incidentRepository.findById(id)
                .orElseGet(() -> incidentRepository.findByTrackingCode(id)
                        .orElseThrow(() -> new IncidentNotFoundException("Incident not found: " + id)));

        // Validate state machine transition
        IncidentStatus currentStatus = IncidentStatus.fromString(incident.getStatus());
        if (!IncidentStatus.isValidTransition(currentStatus, IncidentStatus.RESOLVED)) {
            throw new InvalidStatusTransitionException("Cannot resolve incident from status '" + currentStatus.getValue() + "'.");
        }

        String now = Instant.now().toString();
        incident.setStatus(IncidentStatus.RESOLVED.getValue());
        incident.setResolvedAt(now);
        incident.setUpdatedAt(now);

        if (incident.getUpdates() == null) {
            incident.setUpdates(new ArrayList<>());
        } else {
            incident.setUpdates(new ArrayList<>(incident.getUpdates()));
        }
        incident.getUpdates().add(new Incident.IncidentUpdate(
            UUID.randomUUID().toString(),
            IncidentStatus.RESOLVED.getValue(),
            resolution != null ? resolution : "Incident resolved",
            officerEmail != null ? officerEmail : "Command Center",
            now,
            false
        ));

        // Release the assigned rescue team atomically
        String assignedTeamId = incident.getAssignedTeamId();
        if (assignedTeamId != null) {
            rescueTeamRepository.findById(assignedTeamId).ifPresent(team -> {
                team.setStatus("available");
                team.setCurrentIncident(null);
                team.setLastUpdated(now);
                rescueTeamRepository.save(team);
                log.info("Rescue team {} released after incident {} resolved", assignedTeamId, id);
            });
        }

        logAudit(officerEmail, "RESOLVE_INCIDENT", "incident", id, "Incident resolved: " + resolution);
        Incident resolved = incidentRepository.save(incident);
        SseController.broadcastIncidentUpdated(resolved);
        return resolved;
    }


    // ─── Media File Storage & Retrieval ──────────────────────────
    // File storage and retrieval is handled by GridFsStorageService — no local filesystem access.

    // ─── Helpers: Haversine & Responders ─────────────────────────


    private void assignNearestResponders(Incident incident, double lat, double lng) {
        try {
            List<RescueTeam> teams = rescueTeamRepository.findAll();
            RescueTeam nearestTeam = null;
            double nearestTeamDist = Double.MAX_VALUE;

            for (RescueTeam t : teams) {
                if (t.getLocation() != null && t.getLocation().getLat() != 0) {
                    double d = haversineKm(lat, lng, t.getLocation().getLat(), t.getLocation().getLng());
                    if (d < nearestTeamDist) {
                        nearestTeamDist = d;
                        nearestTeam = t;
                    }
                }
            }

            List<Hospital> hospitals = hospitalRepository.findAll();
            Hospital nearestHospital = null;
            double nearestHospDist = Double.MAX_VALUE;

            for (Hospital h : hospitals) {
                if (h.getLocation() != null && h.getLocation().getLat() != 0) {
                    double d = haversineKm(lat, lng, h.getLocation().getLat(), h.getLocation().getLng());
                    if (d < nearestHospDist) {
                        nearestHospDist = d;
                        nearestHospital = h;
                    }
                }
            }

            List<Incident.AssignedUnit> units = new ArrayList<>();
            double dist = nearestTeamDist != Double.MAX_VALUE ? nearestTeamDist : 3.8;
            int eta = Math.max(3, (int) Math.round((dist / 40.0) * 60.0) + 2);

            if (nearestTeam != null) {
                incident.setAssignedTeam(nearestTeam.getId());
                incident.setAssignedTeamId(nearestTeam.getId());
                incident.setAssignedTeamName(nearestTeam.getName());
                incident.setEta(eta);
                units.add(new Incident.AssignedUnit(
                    "unit-" + nearestTeam.getId(),
                    "rescue_team",
                    nearestTeam.getName(),
                    nearestTeam.getCode() != null ? nearestTeam.getCode() : "ALPHA-01",
                    Math.round(dist * 100.0) / 100.0,
                    eta,
                    "assigned"
                ));
            } else {
                incident.setAssignedTeamName("SL Emergency Rescue Unit Alpha");
                incident.setEta(eta);
            }

            if (nearestHospital != null) {
                incident.setAssignedHospital(nearestHospital.getName());
                double hDist = nearestHospDist != Double.MAX_VALUE ? nearestHospDist : 5.2;
                int hEta = Math.max(4, (int) Math.round((hDist / 40.0) * 60.0) + 2);
                units.add(new Incident.AssignedUnit(
                    "unit-hosp-" + nearestHospital.getId(),
                    "ambulance",
                    "1990 Suwa Seriya (" + nearestHospital.getName() + ")",
                    "AMB-" + (int)(Math.random() * 900 + 100),
                    Math.round(hDist * 100.0) / 100.0,
                    hEta,
                    "assigned"
                ));
            }
            incident.setAssignedUnits(units);
        } catch (Exception e) {
            log.warn("Could not calculate nearest responders: {}", e.getMessage());
            incident.setEta(8);
        }
    }

    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371; // Earth radius in km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private String determineDefaultSeverity(String type, Boolean isSilentSos) {
        if (Boolean.TRUE.equals(isSilentSos)) return "critical";
        return switch (type) {
            case "building_collapse", "fire", "landslide" -> "critical";
            case "road_accident", "flood", "crime", "medical" -> "high";
            case "missing_person", "severe_weather" -> "medium";
            default -> "medium";
        };
    }

    private String formatAgencyName(String agency) {
        return switch (agency) {
            case "command_centre" -> "NOVA Command Centre";
            case "police" -> "Emergency Police Dispatch";
            case "ambulance" -> "1990 Emergency Ambulance";
            case "fire_rescue" -> "Fire & Rescue Service";
            case "hospital" -> "Emergency Triage & Hospital Unit";
            case "disaster_response" -> "Disaster Management Centre (DMC)";
            case "search_rescue" -> "Special Task Rescue Forces";
            default -> agency.replace('_', ' ').toUpperCase();
        };
    }

    private void logAudit(String userEmail, String action, String resource, String resourceId, String details) {
        AuditLog log = new AuditLog();
        log.setId(UUID.randomUUID().toString());
        log.setUserId(userEmail != null ? userEmail : "system");
        log.setUserName(userEmail != null ? userEmail : "system");
        log.setAction(action);
        log.setResource(resource);
        log.setResourceId(resourceId);
        log.setDetails(details);
        log.setTimestamp(Instant.now().toString());
        log.setSeverity("info");
        auditLogRepository.save(log);
    }
}
