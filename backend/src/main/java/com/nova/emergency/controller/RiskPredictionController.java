package com.nova.emergency.controller;

import com.nova.emergency.dto.ApiResponse;
import com.nova.emergency.model.RiskPrediction;
import com.nova.emergency.repository.RiskPredictionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/predictions")
public class RiskPredictionController {

    private final RiskPredictionRepository riskPredictionRepository;

    public RiskPredictionController(RiskPredictionRepository riskPredictionRepository) {
        this.riskPredictionRepository = riskPredictionRepository;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<RiskPrediction>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(riskPredictionRepository.findAll()));
    }

    @GetMapping("/zone/{zone}")
    public ResponseEntity<ApiResponse<RiskPrediction>> getByZone(@PathVariable String zone) {
        return riskPredictionRepository.findByZone(zone)
                .map(rp -> ResponseEntity.ok(ApiResponse.ok(rp)))
                .orElse(ResponseEntity.notFound().build());
    }
}
