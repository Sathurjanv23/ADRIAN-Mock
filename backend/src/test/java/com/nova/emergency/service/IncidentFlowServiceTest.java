package com.nova.emergency.service;

import com.nova.emergency.exception.IncidentNotFoundException;
import com.nova.emergency.exception.InvalidStatusException;
import com.nova.emergency.exception.InvalidStatusTransitionException;
import com.nova.emergency.exception.UnauthorizedMissionUpdateException;
import com.nova.emergency.model.Incident;
import com.nova.emergency.model.RescueTeam;
import com.nova.emergency.repository.AuditLogRepository;
import com.nova.emergency.repository.HospitalRepository;
import com.nova.emergency.repository.IncidentRepository;
import com.nova.emergency.repository.RescueTeamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IncidentFlowServiceTest {

    @Mock
    private IncidentRepository incidentRepository;

    @Mock
    private RescueTeamRepository rescueTeamRepository;

    @Mock
    private HospitalRepository hospitalRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private IncidentService incidentService;

    private RescueTeam alphaSquad;
    private RescueTeam bravoSquad;

    @BeforeEach
    void setUp() {
        alphaSquad = new RescueTeam();
        alphaSquad.setId("RT-ALPHA-02");
        alphaSquad.setName("Alpha Heavy Rescue");
        alphaSquad.setStatus("available");
        alphaSquad.setLocation(new RescueTeam.GeoLocation(6.9271, 79.8612, "Colombo Base", "Colombo"));

        bravoSquad = new RescueTeam();
        bravoSquad.setId("RT-BRAVO-01");
        bravoSquad.setName("Bravo Marine Unit");
        bravoSquad.setStatus("available");
        bravoSquad.setLocation(new RescueTeam.GeoLocation(6.9350, 79.8500, "Port Base", "Colombo"));
    }

    @Test
    @DisplayName("reportEmergency sets initial status to submitted and creates multi-agency notifications")
    void testReportEmergencyInitialStatusAndRouting() {
        when(rescueTeamRepository.findAll()).thenReturn(List.of(alphaSquad));
        when(incidentRepository.save(any(Incident.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Incident incident = incidentService.reportEmergency(
                "road_accident",
                "Severe vehicle collision on Baseline Road",
                6.9271,
                79.8612,
                15.0,
                "2026-09-03T09:00:00Z",
                "Baseline Road, Dematagoda",
                "Kasun Perera",
                "+94771234567",
                "citizen@gmail.com",
                false,
                null,
                null
        );

        assertNotNull(incident);
        assertEquals("submitted", incident.getStatus());
        assertNotNull(incident.getTrackingCode());
        assertTrue(incident.getTrackingCode().startsWith("NOV-"));
        assertEquals("high", incident.getSeverity());
        assertNotNull(incident.getRecommendedAgencies());
        assertTrue(incident.getRecommendedAgencies().contains("search_rescue"));
        assertTrue(incident.getRecommendedAgencies().contains("police"));
        assertTrue(incident.getRecommendedAgencies().contains("ambulance"));
        assertNotNull(incident.getNotifiedAgencies());
        assertFalse(incident.getNotifiedAgencies().isEmpty());
    }

    @Test
    @DisplayName("resolveIncident finds incident by database ID")
    void testResolveIncidentById() {
        Incident incident = new Incident();
        incident.setId("66d6a1b2c3d4e5f6");
        incident.setTrackingCode("NOV-1042");

        when(incidentRepository.findById("66d6a1b2c3d4e5f6")).thenReturn(Optional.of(incident));

        Incident resolved = incidentService.resolveIncident("66d6a1b2c3d4e5f6");
        assertNotNull(resolved);
        assertEquals("66d6a1b2c3d4e5f6", resolved.getId());
    }

    @Test
    @DisplayName("resolveIncident finds incident by trackingCode when ID does not match")
    void testResolveIncidentByTrackingCode() {
        Incident incident = new Incident();
        incident.setId("66d6a1b2c3d4e5f6");
        incident.setTrackingCode("NOV-1042");

        when(incidentRepository.findById("NOV-1042")).thenReturn(Optional.empty());
        when(incidentRepository.findByTrackingCode("NOV-1042")).thenReturn(Optional.of(incident));

        Incident resolved = incidentService.resolveIncident("NOV-1042");
        assertNotNull(resolved);
        assertEquals("NOV-1042", resolved.getTrackingCode());
    }

    @Test
    @DisplayName("resolveIncident throws ResourceNotFoundException when incident does not exist")
    void testResolveIncidentNotFound() {
        when(incidentRepository.findById("NOV-9999")).thenReturn(Optional.empty());
        when(incidentRepository.findByTrackingCode("NOV-9999")).thenReturn(Optional.empty());

        assertThrows(com.nova.emergency.exception.ResourceNotFoundException.class, () -> {
            incidentService.resolveIncident("NOV-9999");
        });
    }

    @Test
    @DisplayName("acceptMission assigns team, sets status to acknowledged, and updates squad")
    void testAcceptMissionSuccess() {
        Incident incident = new Incident();
        incident.setId("NOV-5050");
        incident.setTrackingCode("NOV-5050");
        incident.setStatus("reported");
        incident.setTitle("Building collapse in Colombo 03");
        incident.setUpdates(new ArrayList<>());

        when(incidentRepository.findById("NOV-5050")).thenReturn(Optional.of(incident));
        when(incidentRepository.save(any(Incident.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(rescueTeamRepository.findById("RT-ALPHA-02")).thenReturn(Optional.of(alphaSquad));

        Incident accepted = incidentService.acceptMission("NOV-5050", "RT-ALPHA-02", "Alpha Heavy Rescue", "officer@rescue.lk");

        assertEquals("acknowledged", accepted.getStatus());
        assertEquals("RT-ALPHA-02", accepted.getAssignedTeamId());
        assertEquals("Alpha Heavy Rescue", accepted.getAssignedTeamName());
        assertNotNull(accepted.getAcknowledgedAt());
        assertEquals("officer@rescue.lk", accepted.getAcknowledgedBy());
        assertEquals("assigned", alphaSquad.getStatus());
        assertEquals("NOV-5050", alphaSquad.getCurrentIncident());
    }

    @Test
    @DisplayName("acceptMission rejects missing or empty teamId with IllegalArgumentException")
    void testAcceptMissionMissingTeamId() {
        Incident incident = new Incident();
        incident.setId("NOV-5050");
        incident.setStatus("reported");

        when(incidentRepository.findById("NOV-5050")).thenReturn(Optional.of(incident));

        assertThrows(IllegalArgumentException.class, () -> {
            incidentService.acceptMission("NOV-5050", "", "Alpha Heavy Rescue", "officer@rescue.lk");
        });
    }

    @Test
    @DisplayName("acceptMission returns idempotent success on repeated accept by same team")
    void testAcceptMissionIdempotentSameTeam() {
        Incident incident = new Incident();
        incident.setId("NOV-5050");
        incident.setStatus("acknowledged");
        incident.setAssignedTeamId("RT-ALPHA-02");
        incident.setAssignedTeamName("Alpha Heavy Rescue");

        when(incidentRepository.findById("NOV-5050")).thenReturn(Optional.of(incident));

        Incident accepted = incidentService.acceptMission("NOV-5050", "RT-ALPHA-02", "Alpha Heavy Rescue", "officer@rescue.lk");
        assertEquals("acknowledged", accepted.getStatus());
        assertEquals("RT-ALPHA-02", accepted.getAssignedTeamId());
    }

    @Test
    @DisplayName("acceptMission prevents concurrent conflicting team assignment")
    void testAcceptMissionConflictRejection() {
        Incident incident = new Incident();
        incident.setId("NOV-5050");
        incident.setStatus("acknowledged");
        incident.setAssignedTeamId("RT-ALPHA-02");
        incident.setAssignedTeamName("Alpha Heavy Rescue");

        when(incidentRepository.findById("NOV-5050")).thenReturn(Optional.of(incident));

        assertThrows(IllegalStateException.class, () -> {
            incidentService.acceptMission("NOV-5050", "RT-BRAVO-01", "Bravo Marine Unit", "officer2@rescue.lk");
        });
    }

    @Test
    @DisplayName("updateMissionStatus throws IncidentNotFoundException for non-existent incident")
    void testUpdateMissionStatusNotFound() {
        when(incidentRepository.findById("NOV-NONEXISTENT")).thenReturn(Optional.empty());
        when(incidentRepository.findByTrackingCode("NOV-NONEXISTENT")).thenReturn(Optional.empty());

        assertThrows(IncidentNotFoundException.class, () -> {
            incidentService.updateMissionStatus("NOV-NONEXISTENT", "on_scene", "RT-ALPHA-02", null, "officer@rescue.lk");
        });
    }

    @Test
    @DisplayName("updateMissionStatus throws InvalidStatusException for unknown status string")
    void testUpdateMissionStatusInvalidEnum() {
        Incident incident = new Incident();
        incident.setId("NOV-5050");
        incident.setStatus("acknowledged");

        when(incidentRepository.findById("NOV-5050")).thenReturn(Optional.of(incident));

        assertThrows(InvalidStatusException.class, () -> {
            incidentService.updateMissionStatus("NOV-5050", "flying_in_sky", "RT-ALPHA-02", null, "officer@rescue.lk");
        });
    }

    @Test
    @DisplayName("updateMissionStatus throws InvalidStatusTransitionException for illegal jump (submitted -> on_scene)")
    void testUpdateMissionStatusInvalidTransition() {
        Incident incident = new Incident();
        incident.setId("NOV-5050");
        incident.setStatus("submitted");

        when(incidentRepository.findById("NOV-5050")).thenReturn(Optional.of(incident));

        assertThrows(InvalidStatusTransitionException.class, () -> {
            incidentService.updateMissionStatus("NOV-5050", "on_scene", "RT-ALPHA-02", null, "officer@rescue.lk");
        });
    }

    @Test
    @DisplayName("updateMissionStatus rejects unauthorized rescue team updating another squad's mission")
    void testUpdateMissionStatusUnauthorizedTeam() {
        Incident incident = new Incident();
        incident.setId("NOV-5050");
        incident.setStatus("en_route");
        incident.setAssignedTeamId("RT-ALPHA-02");
        incident.setAssignedTeamName("Alpha Heavy Rescue");

        when(incidentRepository.findById("NOV-5050")).thenReturn(Optional.of(incident));

        assertThrows(UnauthorizedMissionUpdateException.class, () -> {
            incidentService.updateMissionStatus("NOV-5050", "on_scene", "RT-BRAVO-01", null, "officer@rescue.lk");
        });
    }

    @Test
    @DisplayName("updateMissionStatus transitions lifecycle correctly: en_route -> on_scene -> transporting -> resolved")
    void testUpdateMissionStatusLifecycle() {
        Incident incident = new Incident();
        incident.setId("NOV-5050");
        incident.setStatus("acknowledged");
        incident.setAssignedTeamId("RT-ALPHA-02");
        incident.setAssignedTeamName("Alpha Heavy Rescue");
        incident.setUpdates(new ArrayList<>());

        alphaSquad.setCurrentIncident("NOV-5050");
        alphaSquad.setStatus("assigned");

        when(incidentRepository.findById("NOV-5050")).thenReturn(Optional.of(incident));
        when(incidentRepository.save(any(Incident.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(rescueTeamRepository.findById("RT-ALPHA-02")).thenReturn(Optional.of(alphaSquad));

        // Step 1: Start journey -> en_route
        Incident enRoute = incidentService.updateMissionStatus("NOV-5050", "en_route", "RT-ALPHA-02", null, "officer@rescue.lk");
        assertEquals("en_route", enRoute.getStatus());
        assertEquals("en_route", alphaSquad.getStatus());

        // Step 2: Idempotent repeat en_route -> en_route
        Incident enRouteRepeat = incidentService.updateMissionStatus("NOV-5050", "en_route", "RT-ALPHA-02", null, "officer@rescue.lk");
        assertEquals("en_route", enRouteRepeat.getStatus());

        // Step 3: Arrive on scene -> on_scene
        Incident onScene = incidentService.updateMissionStatus("NOV-5050", "on_scene", "RT-ALPHA-02", "Arrived at building collapse perimeter", "officer@rescue.lk");
        assertEquals("on_scene", onScene.getStatus());
        assertNotNull(onScene.getArrivedAt());
        assertEquals("on_scene", alphaSquad.getStatus());

        // Step 4: Transport victims -> transporting
        Incident transporting = incidentService.updateMissionStatus("NOV-5050", "transporting", "RT-ALPHA-02", "Transporting 2 rescued victims to National Hospital", "officer@rescue.lk");
        assertEquals("transporting", transporting.getStatus());

        // Step 5: Complete mission -> resolved
        Incident resolved = incidentService.updateMissionStatus("NOV-5050", "resolved", "RT-ALPHA-02", "All victims safely admitted. Operations cleared.", "officer@rescue.lk");
        assertEquals("resolved", resolved.getStatus());
        assertNotNull(resolved.getResolvedAt());
        assertEquals("available", alphaSquad.getStatus());
        assertNull(alphaSquad.getCurrentIncident());
    }
}
