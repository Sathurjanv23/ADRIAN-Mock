package com.nova.emergency.controller;

import com.nova.emergency.dto.ApiResponse;
import com.nova.emergency.model.Resource;
import com.nova.emergency.service.ResourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/resources")
public class ResourceController {

    private final ResourceService resourceService;

    public ResourceController(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Resource>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(resourceService.getAll()));
    }

    @PostMapping("/allocate")
    public ResponseEntity<ApiResponse<Resource>> allocate(@RequestBody Map<String, Object> body) {
        String resourceId = (String) body.get("resourceId");
        int quantity = (Integer) body.get("quantity");
        String incidentId = (String) body.get("incidentId");
        Resource updated = resourceService.allocate(resourceId, quantity, incidentId);
        return ResponseEntity.ok(ApiResponse.ok(updated, "Resource allocated"));
    }
}
