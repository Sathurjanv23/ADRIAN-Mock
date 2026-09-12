package com.nova.emergency.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;

/**
 * ReliefMission — A dispatched vehicle mission carrying food/water from a source to a disaster zone.
 * Supports live GPS tracking via SSE updates every 5 seconds.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "relief_missions")
public class ReliefMission {

    @Id
    private String id;

    private String reliefRequestId;
    private String foodSourceId;
    private String foodSourceName;

    private String vehicleId;
    private String vehicleName;
    private String driverName;
    private String driverContact;

    private GeoPoint pickupLocation;
    private GeoPoint destination;

    private int meals;
    private int waterBottles;

    // Estimated time of arrival in minutes
    private int eta;

    // PENDING | ASSIGNED | ON_THE_WAY | DELIVERED | COMPLETED | CANCELLED
    private String status = "PENDING";

    // Live vehicle GPS (updated via PUT /api/relief-missions/{id}/location)
    private double currentLat;
    private double currentLng;

    // Route waypoints for map display
    private List<GeoPoint> route;

    // Distance to destination in km
    private double distanceKm;

    private String notes;
    private String createdAt;
    private String updatedAt;
    private String completedAt;

    // ─── Nested types ────────────────────────────────────────

    @Data
    public static class GeoPoint {
        private double lat;
        private double lng;
        private String address;
        private String district;

        public GeoPoint() {}
        public GeoPoint(double lat, double lng, String address, String district) {
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
    public String getReliefRequestId() { return reliefRequestId; }
    public void setReliefRequestId(String reliefRequestId) { this.reliefRequestId = reliefRequestId; }
    public String getFoodSourceId() { return foodSourceId; }
    public void setFoodSourceId(String foodSourceId) { this.foodSourceId = foodSourceId; }
    public String getFoodSourceName() { return foodSourceName; }
    public void setFoodSourceName(String foodSourceName) { this.foodSourceName = foodSourceName; }
    public String getVehicleId() { return vehicleId; }
    public void setVehicleId(String vehicleId) { this.vehicleId = vehicleId; }
    public String getVehicleName() { return vehicleName; }
    public void setVehicleName(String vehicleName) { this.vehicleName = vehicleName; }
    public String getDriverName() { return driverName; }
    public void setDriverName(String driverName) { this.driverName = driverName; }
    public String getDriverContact() { return driverContact; }
    public void setDriverContact(String driverContact) { this.driverContact = driverContact; }
    public GeoPoint getPickupLocation() { return pickupLocation; }
    public void setPickupLocation(GeoPoint pickupLocation) { this.pickupLocation = pickupLocation; }
    public GeoPoint getDestination() { return destination; }
    public void setDestination(GeoPoint destination) { this.destination = destination; }
    public int getMeals() { return meals; }
    public void setMeals(int meals) { this.meals = meals; }
    public int getWaterBottles() { return waterBottles; }
    public void setWaterBottles(int waterBottles) { this.waterBottles = waterBottles; }
    public int getEta() { return eta; }
    public void setEta(int eta) { this.eta = eta; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public double getCurrentLat() { return currentLat; }
    public void setCurrentLat(double currentLat) { this.currentLat = currentLat; }
    public double getCurrentLng() { return currentLng; }
    public void setCurrentLng(double currentLng) { this.currentLng = currentLng; }
    public List<GeoPoint> getRoute() { return route; }
    public void setRoute(List<GeoPoint> route) { this.route = route; }
    public double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(double distanceKm) { this.distanceKm = distanceKm; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    public String getCompletedAt() { return completedAt; }
    public void setCompletedAt(String completedAt) { this.completedAt = completedAt; }
}
