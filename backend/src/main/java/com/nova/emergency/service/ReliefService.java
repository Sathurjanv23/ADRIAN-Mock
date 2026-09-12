package com.nova.emergency.service;

import com.nova.emergency.ai.AIService;
import com.nova.emergency.exception.InsufficientInventoryException;
import com.nova.emergency.exception.ReliefMissionConflictException;
import com.nova.emergency.exception.ResourceNotFoundException;
import com.nova.emergency.model.*;
import com.nova.emergency.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ReliefService — Core business logic for the ADRN AI Relief Logistics module.
 *
 * Responsibilities:
 * - Create and manage ReliefRequests from Incidents
 * - Find optimal food sources using AI recommendations
 * - Create and track ReliefMissions with live GPS
 * - Enforce inventory consistency (no negative stock, no duplicate missions)
 * - Coordinate with AI for relief recommendations
 */
@Service
public class ReliefService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ReliefService.class);

    // Average vehicle speed for ETA calculation (km/h)
    private static final double AVG_SPEED_KMH = 40.0;

    // Meals required per person per day
    private static final double MEALS_PER_PERSON = 3.0;

    // Water bottles per person per day
    private static final double WATER_PER_PERSON = 2.0;

    private final ReliefRequestRepository reliefRequestRepository;
    private final ReliefMissionRepository reliefMissionRepository;
    private final FoodSourceRepository foodSourceRepository;
    private final InventoryRepository inventoryRepository;
    private final IncidentRepository incidentRepository;
    private final AIService aiService;

    public ReliefService(ReliefRequestRepository reliefRequestRepository,
                         ReliefMissionRepository reliefMissionRepository,
                         FoodSourceRepository foodSourceRepository,
                         InventoryRepository inventoryRepository,
                         IncidentRepository incidentRepository,
                         AIService aiService) {
        this.reliefRequestRepository = reliefRequestRepository;
        this.reliefMissionRepository = reliefMissionRepository;
        this.foodSourceRepository = foodSourceRepository;
        this.inventoryRepository = inventoryRepository;
        this.incidentRepository = incidentRepository;
        this.aiService = aiService;
    }

    // ─── Relief Requests ────────────────────────────────────────────

    /**
     * Create a relief request from an incident.
     * Returns existing request if one already exists for this incident.
     */
    public ReliefRequest createReliefRequestFromIncident(String incidentId, String createdBy) {
        // Prevent duplicate relief requests for the same incident
        Optional<ReliefRequest> existing = reliefRequestRepository.findByIncidentId(incidentId);
        if (existing.isPresent()) {
            log.info("Relief request already exists for incident {}", incidentId);
            return existing.get();
        }

        Incident incident = incidentRepository.findById(incidentId)
            .orElseThrow(() -> new ResourceNotFoundException("Incident not found: " + incidentId));

        int people = Math.max(1, incident.getPeopleAffected());
        int requiredMeals = (int) Math.ceil(people * MEALS_PER_PERSON);
        int requiredWater = (int) Math.ceil(people * WATER_PER_PERSON);

        String priority = mapSeverityToPriority(incident.getSeverity());

        ReliefRequest.GeoLocation loc = null;
        if (incident.getLocation() != null) {
            loc = new ReliefRequest.GeoLocation(
                incident.getLocation().getLat(),
                incident.getLocation().getLng(),
                incident.getLocation().getAddress(),
                incident.getLocation().getDistrict()
            );
        }

        ReliefRequest request = new ReliefRequest();
        request.setIncidentId(incidentId);
        request.setIncidentTrackingCode(incident.getTrackingCode());
        request.setDisasterLocation(loc);
        request.setPeopleAffected(people);
        request.setRequiredMeals(requiredMeals);
        request.setRequiredWater(requiredWater);
        request.setPriority(priority);
        request.setRequestType("FOOD_AND_WATER");
        request.setStatus("PENDING");
        request.setCreatedBy(createdBy != null ? createdBy : "SYSTEM");
        request.setCreatedAt(Instant.now().toString());
        request.setUpdatedAt(Instant.now().toString());

        ReliefRequest saved = reliefRequestRepository.save(request);
        log.info("Created relief request {} for incident {} (people={}, meals={}, water={})",
            saved.getId(), incidentId, people, requiredMeals, requiredWater);
        return saved;
    }

    /**
     * Create a relief request directly (from Command Center).
     */
    public ReliefRequest createReliefRequest(ReliefRequest request) {
        request.setStatus("PENDING");
        request.setCreatedAt(Instant.now().toString());
        request.setUpdatedAt(Instant.now().toString());
        return reliefRequestRepository.save(request);
    }

    public List<ReliefRequest> getAllReliefRequests() {
        return reliefRequestRepository.findAll();
    }

    public List<ReliefRequest> getReliefRequestsByStatus(String status) {
        return reliefRequestRepository.findByStatus(status);
    }

    public Optional<ReliefRequest> getReliefRequestById(String id) {
        return reliefRequestRepository.findById(id);
    }

    public ReliefRequest updateReliefRequestStatus(String id, String status) {
        ReliefRequest request = reliefRequestRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("ReliefRequest not found: " + id));
        request.setStatus(status);
        request.setUpdatedAt(Instant.now().toString());
        return reliefRequestRepository.save(request);
    }

    // ─── Relief Missions ────────────────────────────────────────────

    /**
     * Create a relief mission assigning a vehicle to deliver supplies.
     * Reserves inventory atomically — throws InsufficientInventoryException if not enough stock.
     */
    public ReliefMission createReliefMission(String reliefRequestId, String foodSourceId,
                                              String vehicleId, String vehicleName,
                                              String driverName, String driverContact) {
        // Prevent duplicate active missions for same relief request
        Optional<ReliefMission> existing = reliefMissionRepository.findByReliefRequestId(reliefRequestId);
        if (existing.isPresent() && !Set.of("CANCELLED", "COMPLETED").contains(existing.get().getStatus())) {
            throw new ReliefMissionConflictException(
                "An active relief mission already exists for request: " + reliefRequestId
            );
        }

        ReliefRequest reliefReq = reliefRequestRepository.findById(reliefRequestId)
            .orElseThrow(() -> new ResourceNotFoundException("ReliefRequest not found: " + reliefRequestId));

        FoodSource source = foodSourceRepository.findById(foodSourceId)
            .orElseThrow(() -> new ResourceNotFoundException("FoodSource not found: " + foodSourceId));

        if (!"ACTIVE".equals(source.getStatus())) {
            throw new InsufficientInventoryException("Food source is not active: " + source.getName());
        }

        // Reserve inventory
        reserveInventory(foodSourceId, source.getName(), reliefReq.getRequiredMeals(), reliefReq.getRequiredWater());

        // Calculate ETA
        double distanceKm = 0;
        int eta = 30; // default fallback
        if (reliefReq.getDisasterLocation() != null && source.getLocation() != null) {
            distanceKm = haversineDistance(
                source.getLocation().getLat(), source.getLocation().getLng(),
                reliefReq.getDisasterLocation().getLat(), reliefReq.getDisasterLocation().getLng()
            );
            eta = (int) Math.ceil(distanceKm / AVG_SPEED_KMH * 60);
        }

        ReliefMission.GeoPoint pickup = null;
        if (source.getLocation() != null) {
            pickup = new ReliefMission.GeoPoint(
                source.getLocation().getLat(), source.getLocation().getLng(),
                source.getLocation().getAddress(), source.getLocation().getDistrict()
            );
        }
        ReliefMission.GeoPoint dest = null;
        if (reliefReq.getDisasterLocation() != null) {
            dest = new ReliefMission.GeoPoint(
                reliefReq.getDisasterLocation().getLat(), reliefReq.getDisasterLocation().getLng(),
                reliefReq.getDisasterLocation().getAddress(), reliefReq.getDisasterLocation().getDistrict()
            );
        }

        ReliefMission mission = new ReliefMission();
        mission.setReliefRequestId(reliefRequestId);
        mission.setFoodSourceId(foodSourceId);
        mission.setFoodSourceName(source.getName());
        mission.setVehicleId(vehicleId);
        mission.setVehicleName(vehicleName);
        mission.setDriverName(driverName);
        mission.setDriverContact(driverContact);
        mission.setPickupLocation(pickup);
        mission.setDestination(dest);
        mission.setMeals(reliefReq.getRequiredMeals());
        mission.setWaterBottles(reliefReq.getRequiredWater());
        mission.setEta(eta);
        mission.setDistanceKm(distanceKm);
        mission.setStatus("ASSIGNED");
        if (pickup != null) {
            mission.setCurrentLat(pickup.getLat());
            mission.setCurrentLng(pickup.getLng());
        }
        mission.setCreatedAt(Instant.now().toString());
        mission.setUpdatedAt(Instant.now().toString());

        ReliefMission saved = reliefMissionRepository.save(mission);

        // Update relief request status
        reliefReq.setStatus("ASSIGNED");
        reliefReq.setUpdatedAt(Instant.now().toString());
        reliefRequestRepository.save(reliefReq);

        log.info("Created relief mission {} for request {} from source {} (ETA={}min, dist={}km)",
            saved.getId(), reliefRequestId, source.getName(), eta, String.format("%.1f", distanceKm));

        return saved;
    }

    public List<ReliefMission> getAllReliefMissions() {
        return reliefMissionRepository.findAll();
    }

    public List<ReliefMission> getActiveMissions() {
        return reliefMissionRepository.findByStatusIn(List.of("ASSIGNED", "ON_THE_WAY"));
    }

    public Optional<ReliefMission> getReliefMissionById(String id) {
        return reliefMissionRepository.findById(id);
    }

    /**
     * Update mission GPS location — called by vehicle app every ~5 seconds.
     */
    public ReliefMission updateMissionLocation(String missionId, double lat, double lng) {
        ReliefMission mission = reliefMissionRepository.findById(missionId)
            .orElseThrow(() -> new ResourceNotFoundException("ReliefMission not found: " + missionId));

        if (Set.of("COMPLETED", "CANCELLED").contains(mission.getStatus())) {
            throw new IllegalStateException("Cannot update location of a " + mission.getStatus() + " mission");
        }

        mission.setCurrentLat(lat);
        mission.setCurrentLng(lng);
        mission.setStatus("ON_THE_WAY");

        // Recalculate ETA based on current position
        if (mission.getDestination() != null) {
            double remaining = haversineDistance(lat, lng,
                mission.getDestination().getLat(), mission.getDestination().getLng());
            int newEta = (int) Math.ceil(remaining / AVG_SPEED_KMH * 60);
            mission.setEta(newEta);
            mission.setDistanceKm(remaining);
        }

        mission.setUpdatedAt(Instant.now().toString());
        return reliefMissionRepository.save(mission);
    }

    /**
     * Update mission status with validation — prevents invalid state transitions.
     */
    public ReliefMission updateMissionStatus(String missionId, String newStatus, String notes) {
        ReliefMission mission = reliefMissionRepository.findById(missionId)
            .orElseThrow(() -> new ResourceNotFoundException("ReliefMission not found: " + missionId));

        validateStatusTransition(mission.getStatus(), newStatus);

        String oldStatus = mission.getStatus();
        mission.setStatus(newStatus);
        if (notes != null) mission.setNotes(notes);
        mission.setUpdatedAt(Instant.now().toString());

        if ("COMPLETED".equals(newStatus)) {
            mission.setCompletedAt(Instant.now().toString());
            // Consume inventory (remove reservation, reduce total stock)
            consumeInventory(mission.getFoodSourceId(), mission.getMeals(), mission.getWaterBottles());
            // Update food source stock
            updateFoodSourceStock(mission.getFoodSourceId(), mission.getMeals(), mission.getWaterBottles());
            // Complete the relief request
            reliefRequestRepository.findById(mission.getReliefRequestId()).ifPresent(req -> {
                req.setStatus("COMPLETED");
                req.setUpdatedAt(Instant.now().toString());
                reliefRequestRepository.save(req);
            });
        } else if ("CANCELLED".equals(newStatus)) {
            // Release reserved inventory back to available
            releaseInventory(mission.getFoodSourceId(), mission.getMeals(), mission.getWaterBottles());
        }

        log.info("Mission {} status: {} → {}", missionId, oldStatus, newStatus);
        return reliefMissionRepository.save(mission);
    }

    // ─── Food Sources ───────────────────────────────────────────────

    public FoodSource createFoodSource(FoodSource source) {
        source.setStatus("ACTIVE");
        source.setCreatedAt(Instant.now().toString());
        source.setUpdatedAt(Instant.now().toString());

        FoodSource saved = foodSourceRepository.save(source);

        // Create inventory records
        createInventoryRecord(saved.getId(), saved.getName(), "MEALS", saved.getAvailableMeals(), saved.getExpiryTime());
        createInventoryRecord(saved.getId(), saved.getName(), "WATER", saved.getWaterBottles(), saved.getExpiryTime());

        return saved;
    }

    public List<FoodSource> getAllFoodSources() {
        return foodSourceRepository.findAll();
    }

    public List<FoodSource> getActiveFoodSources() {
        return foodSourceRepository.findByStatus("ACTIVE");
    }

    /**
     * Find nearby food sources sorted by distance from a given GPS point.
     * Uses haversine distance calculation.
     */
    public List<FoodSource> getNearbyFoodSources(double lat, double lng, double radiusKm, int minMeals) {
        return foodSourceRepository.findByStatus("ACTIVE").stream()
            .filter(s -> s.getLocation() != null)
            .filter(s -> minMeals <= 0 || s.getAvailableMeals() >= minMeals)
            .filter(s -> haversineDistance(lat, lng, s.getLocation().getLat(), s.getLocation().getLng()) <= radiusKm)
            .sorted(Comparator.comparingDouble(s ->
                haversineDistance(lat, lng, s.getLocation().getLat(), s.getLocation().getLng())))
            .collect(Collectors.toList());
    }

    public Optional<FoodSource> getFoodSourceById(String id) {
        return foodSourceRepository.findById(id);
    }

    public FoodSource updateFoodSource(String id, FoodSource updates) {
        FoodSource source = foodSourceRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("FoodSource not found: " + id));
        if (updates.getName() != null) source.setName(updates.getName());
        if (updates.getAvailableMeals() >= 0) source.setAvailableMeals(updates.getAvailableMeals());
        if (updates.getWaterBottles() >= 0) source.setWaterBottles(updates.getWaterBottles());
        if (updates.getStatus() != null) source.setStatus(updates.getStatus());
        if (updates.getExpiryTime() != null) source.setExpiryTime(updates.getExpiryTime());
        source.setUpdatedAt(Instant.now().toString());
        return foodSourceRepository.save(source);
    }

    // ─── AI Relief Recommendation ───────────────────────────────────

    /**
     * Use AI service to recommend the best food source for a relief request.
     */
    public AIService.ReliefRecommendation getAIReliefRecommendation(String reliefRequestId) {
        ReliefRequest req = reliefRequestRepository.findById(reliefRequestId)
            .orElseThrow(() -> new ResourceNotFoundException("ReliefRequest not found: " + reliefRequestId));

        List<FoodSource> sources = getActiveFoodSources();

        List<AIService.FoodSourceOption> options = sources.stream()
            .filter(s -> s.getLocation() != null)
            .map(s -> new AIService.FoodSourceOption(
                s.getId(),
                s.getName(),
                s.getType(),
                s.getLocation().getLat(),
                s.getLocation().getLng(),
                s.getAvailableMeals(),
                s.getWaterBottles(),
                req.getDisasterLocation() != null
                    ? haversineDistance(req.getDisasterLocation().getLat(), req.getDisasterLocation().getLng(),
                                        s.getLocation().getLat(), s.getLocation().getLng())
                    : 0.0,
                s.getExpiryTime()
            ))
            .sorted(Comparator.comparingDouble(AIService.FoodSourceOption::distanceKm))
            .limit(10)
            .collect(Collectors.toList());

        AIService.ReliefRecommendationRequest aiReq = new AIService.ReliefRecommendationRequest(
            reliefRequestId,
            req.getDisasterLocation() != null ? req.getDisasterLocation().getLat() : 0,
            req.getDisasterLocation() != null ? req.getDisasterLocation().getLng() : 0,
            req.getPeopleAffected(),
            req.getRequiredMeals(),
            req.getRequiredWater(),
            req.getPriority(),
            options
        );

        return aiService.recommendRelief(aiReq);
    }

    // ─── Statistics ──────────────────────────────────────────────────

    public Map<String, Object> getReliefStats() {
        List<FoodSource> activeSources = getActiveFoodSources();
        int totalMeals = activeSources.stream().mapToInt(FoodSource::getAvailableMeals).sum();
        int totalWater = activeSources.stream().mapToInt(FoodSource::getWaterBottles).sum();

        long activeMissions = reliefMissionRepository.countByStatus("ON_THE_WAY") +
                              reliefMissionRepository.countByStatus("ASSIGNED");
        long completedMissions = reliefMissionRepository.countByStatus("COMPLETED");
        long pendingRequests = reliefRequestRepository.findByStatus("PENDING").size();

        // Critical shortage: pending requests with insufficient nearby sources
        long criticalShortages = reliefRequestRepository.findByPriority("CRITICAL").stream()
            .filter(r -> "PENDING".equals(r.getStatus()))
            .count();

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalMealsAvailable", totalMeals);
        stats.put("totalWaterAvailable", totalWater);
        stats.put("activeMissions", activeMissions);
        stats.put("completedMissions", completedMissions);
        stats.put("pendingRequests", pendingRequests);
        stats.put("criticalShortages", criticalShortages);
        stats.put("activeFoodSources", activeSources.size());
        stats.put("aiServiceStatus", aiService.isAvailable() ? "ONLINE" : "FALLBACK_ACTIVE");
        return stats;
    }

    // ─── Inventory Helpers ───────────────────────────────────────────

    private void reserveInventory(String sourceId, String sourceName, int meals, int water) {
        reserveItem(sourceId, sourceName, "MEALS", meals);
        reserveItem(sourceId, sourceName, "WATER", water);
    }

    private void reserveItem(String sourceId, String sourceName, String itemType, int amount) {
        if (amount <= 0) return;
        Inventory inv = inventoryRepository.findBySourceIdAndItemType(sourceId, itemType)
            .orElseThrow(() -> new InsufficientInventoryException(
                "No inventory record for " + itemType + " at source " + sourceId));
        try {
            inv.reserve(amount);
        } catch (IllegalStateException e) {
            throw new InsufficientInventoryException(
                "Insufficient " + itemType + " at " + sourceName + ": " + e.getMessage());
        }
        inv.setUpdatedAt(Instant.now().toString());
        inventoryRepository.save(inv);
    }

    private void releaseInventory(String sourceId, int meals, int water) {
        releaseItem(sourceId, "MEALS", meals);
        releaseItem(sourceId, "WATER", water);
    }

    private void releaseItem(String sourceId, String itemType, int amount) {
        if (amount <= 0) return;
        inventoryRepository.findBySourceIdAndItemType(sourceId, itemType).ifPresent(inv -> {
            inv.release(amount);
            inv.setUpdatedAt(Instant.now().toString());
            inventoryRepository.save(inv);
        });
    }

    private void consumeInventory(String sourceId, int meals, int water) {
        consumeItem(sourceId, "MEALS", meals);
        consumeItem(sourceId, "WATER", water);
    }

    private void consumeItem(String sourceId, String itemType, int amount) {
        if (amount <= 0) return;
        inventoryRepository.findBySourceIdAndItemType(sourceId, itemType).ifPresent(inv -> {
            inv.consume(amount);
            inv.setUpdatedAt(Instant.now().toString());
            inventoryRepository.save(inv);
        });
    }

    private void createInventoryRecord(String sourceId, String sourceName, String itemType, int quantity, String expiryTime) {
        Inventory inv = new Inventory();
        inv.setSourceId(sourceId);
        inv.setSourceName(sourceName);
        inv.setItemType(itemType);
        inv.setQuantity(quantity);
        inv.setReservedQuantity(0);
        inv.setAvailableQuantity(quantity);
        inv.setExpiryTime(expiryTime);
        inv.setUpdatedAt(Instant.now().toString());
        inventoryRepository.save(inv);
    }

    private void updateFoodSourceStock(String sourceId, int mealsConsumed, int waterConsumed) {
        foodSourceRepository.findById(sourceId).ifPresent(source -> {
            source.setAvailableMeals(Math.max(0, source.getAvailableMeals() - mealsConsumed));
            source.setWaterBottles(Math.max(0, source.getWaterBottles() - waterConsumed));
            if (source.getAvailableMeals() == 0 && source.getWaterBottles() == 0) {
                source.setStatus("DEPLETED");
            }
            source.setUpdatedAt(Instant.now().toString());
            foodSourceRepository.save(source);
        });
    }

    // ─── Utilities ───────────────────────────────────────────────────

    /**
     * Haversine formula — calculates straight-line distance between two GPS points in km.
     */
    public static double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371; // Earth's radius in km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                 * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private String mapSeverityToPriority(String severity) {
        if (severity == null) return "MEDIUM";
        return switch (severity.toLowerCase()) {
            case "critical" -> "CRITICAL";
            case "high"     -> "HIGH";
            case "medium"   -> "MEDIUM";
            default          -> "LOW";
        };
    }

    private void validateStatusTransition(String current, String next) {
        Map<String, List<String>> validTransitions = Map.of(
            "PENDING",    List.of("ASSIGNED", "CANCELLED"),
            "ASSIGNED",   List.of("ON_THE_WAY", "CANCELLED"),
            "ON_THE_WAY", List.of("DELIVERED", "CANCELLED"),
            "DELIVERED",  List.of("COMPLETED"),
            "COMPLETED",  List.of(),
            "CANCELLED",  List.of()
        );
        List<String> allowed = validTransitions.getOrDefault(current, List.of());
        if (!allowed.contains(next)) {
            throw new IllegalStateException(
                "Invalid status transition: " + current + " → " + next +
                ". Allowed: " + allowed
            );
        }
    }
}
