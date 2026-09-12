package com.nova.emergency.service;

import com.nova.emergency.model.Resource;
import com.nova.emergency.repository.ResourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ResourceService {

    private final ResourceRepository resourceRepository;

    public ResourceService(ResourceRepository resourceRepository) {
        this.resourceRepository = resourceRepository;
    }

    public List<Resource> getAll() {
        return resourceRepository.findAll();
    }

    public Resource allocate(String resourceId, int quantity, String incidentId) {
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new IllegalArgumentException("Resource not found: " + resourceId));

        if (resource.getAvailable() < quantity) {
            throw new IllegalArgumentException("Insufficient available quantity. Available: " + resource.getAvailable());
        }

        resource.setAvailable(resource.getAvailable() - quantity);
        resource.setDeployed(resource.getDeployed() + quantity);

        // Update status based on available stock
        if (resource.getAvailable() <= 0) {
            resource.setStatus("deployed");
        } else if (resource.getAvailable() <= resource.getLowStockThreshold()) {
            resource.setStatus("low_stock");
        }

        return resourceRepository.save(resource);
    }

    public Resource update(String id, Map<String, Object> data) {
        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Resource not found: " + id));
        if (data.containsKey("available")) resource.setAvailable((Integer) data.get("available"));
        if (data.containsKey("deployed")) resource.setDeployed((Integer) data.get("deployed"));
        if (data.containsKey("status")) resource.setStatus((String) data.get("status"));
        return resourceRepository.save(resource);
    }
}
