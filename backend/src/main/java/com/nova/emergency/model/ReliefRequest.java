package com.nova.emergency.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * ReliefRequest — Created when a disaster incident requires food/water relief.
 * Linked to an Incident document. Triggers AI relief planning.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "relief_requests")
public class ReliefRequest {

    @Id
    private String id;

    // Reference to the source Incident
    private String incidentId;
    private String incidentTrackingCode;

    private GeoLocation disasterLocation;

    private int peopleAffected;
    private int requiredMeals;
    private int requiredWater; // bottles

    // CRITICAL | HIGH | MEDIUM | LOW
    private String priority;

    // FOOD_AND_WATER | FOOD_ONLY | WATER_ONLY | MEDICAL_SUPPLIES
    private String requestType = "FOOD_AND_WATER";

    // PENDING | ASSIGNED | IN_PROGRESS | COMPLETED | CANCELLED
    private String status = "PENDING";

    private String createdBy;
    private String createdAt;
    private String updatedAt;

    // ─── Nested types ────────────────────────────────────────

    @Data
    public static class GeoLocation {
        private double lat;
        private double lng;
        private String address;
        private String district;

        public GeoLocation() {}
        public GeoLocation(double lat, double lng, String address, String district) {
            this.lat = lat;
            this.lng = lng;
            this.address = address;
            this.district = district;
        }

        public double getLat() { return lat; }
        public void setLat(double lat) { this.lat = lat; }
        public double getLng() { return lng; }
        public void setLng(double lng) { this.lng = lng; }
        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }
        public String getDistrict() { return district; }
        public void setDistrict(String district) { this.district = district; }
    }

    // Explicit getters/setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getIncidentId() { return incidentId; }
    public void setIncidentId(String incidentId) { this.incidentId = incidentId; }
    public String getIncidentTrackingCode() { return incidentTrackingCode; }
    public void setIncidentTrackingCode(String incidentTrackingCode) { this.incidentTrackingCode = incidentTrackingCode; }
    public GeoLocation getDisasterLocation() { return disasterLocation; }
    public void setDisasterLocation(GeoLocation disasterLocation) { this.disasterLocation = disasterLocation; }
    public int getPeopleAffected() { return peopleAffected; }
    public void setPeopleAffected(int peopleAffected) { this.peopleAffected = peopleAffected; }
    public int getRequiredMeals() { return requiredMeals; }
    public void setRequiredMeals(int requiredMeals) { this.requiredMeals = requiredMeals; }
    public int getRequiredWater() { return requiredWater; }
    public void setRequiredWater(int requiredWater) { this.requiredWater = requiredWater; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public String getRequestType() { return requestType; }
    public void setRequestType(String requestType) { this.requestType = requestType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
