package com.nova.emergency.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

/**
 * FoodSource — Represents a food/water supply source (restaurant, hotel, supermarket, warehouse).
 * Part of the ADRN AI Relief Logistics module.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "food_sources")
public class FoodSource {

    @Id
    private String id;

    private String name;

    // RESTAURANT | HOTEL | SUPERMARKET | WAREHOUSE
    private String type;

    private GeoLocation location;

    private int availableMeals;
    private int waterBottles;

    // ISO 8601 timestamp — when the food will expire/become unavailable
    private String expiryTime;

    private String contact;

    // ACTIVE | INACTIVE | DEPLETED
    private String status = "ACTIVE";

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

    // Explicit getters/setters for Lombok compatibility
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public GeoLocation getLocation() { return location; }
    public void setLocation(GeoLocation location) { this.location = location; }
    public int getAvailableMeals() { return availableMeals; }
    public void setAvailableMeals(int availableMeals) { this.availableMeals = availableMeals; }
    public int getWaterBottles() { return waterBottles; }
    public void setWaterBottles(int waterBottles) { this.waterBottles = waterBottles; }
    public String getExpiryTime() { return expiryTime; }
    public void setExpiryTime(String expiryTime) { this.expiryTime = expiryTime; }
    public String getContact() { return contact; }
    public void setContact(String contact) { this.contact = contact; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
