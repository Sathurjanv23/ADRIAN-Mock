package com.nova.emergency.controller;

import com.nova.emergency.dto.ApiResponse;
import com.nova.emergency.model.Hospital;
import com.nova.emergency.repository.HospitalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/hospitals")
public class HospitalController {

    private final HospitalRepository hospitalRepository;

    public HospitalController(HospitalRepository hospitalRepository) {
        this.hospitalRepository = hospitalRepository;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Hospital>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(hospitalRepository.findAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Hospital>> getById(@PathVariable String id) {
        return hospitalRepository.findById(id)
                .map(h -> ResponseEntity.ok(ApiResponse.ok(h)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/capacity")
    public ResponseEntity<ApiResponse<Hospital>> updateCapacity(
            @PathVariable String id,
            @RequestBody Map<String, Object> data) {
        Hospital hospital = hospitalRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Hospital not found: " + id));

        if (data.containsKey("availableBeds"))
            hospital.setAvailableBeds((Integer) data.get("availableBeds"));
        if (data.containsKey("icuAvailable"))
            hospital.setIcuAvailable((Integer) data.get("icuAvailable"));
        hospital.setLastUpdated(Instant.now().toString());

        return ResponseEntity.ok(ApiResponse.ok(hospitalRepository.save(hospital), "Capacity updated"));
    }

    @PostMapping("/{hospitalId}/ambulances/dispatch")
    public ResponseEntity<ApiResponse<Hospital>> dispatchAmbulance(
            @PathVariable String hospitalId,
            @RequestBody Map<String, String> body) {
        Hospital hospital = hospitalRepository.findById(hospitalId)
                .orElseThrow(() -> new IllegalArgumentException("Hospital not found: " + hospitalId));

        String incidentId = body.get("incidentId");

        // Find first available ambulance and dispatch it
        hospital.getAmbulances().stream()
                .filter(a -> "available".equals(a.getStatus()))
                .findFirst()
                .ifPresent(a -> {
                    a.setStatus("dispatched");
                    a.setAssignedCase(incidentId);
                });

        hospital.setLastUpdated(Instant.now().toString());
        return ResponseEntity.ok(ApiResponse.ok(hospitalRepository.save(hospital), "Ambulance dispatched"));
    }
}
