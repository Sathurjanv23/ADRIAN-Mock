package com.nova.emergency.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "resources")
public class Resource {

    @Id
    private String id;

    // ambulance | rescue_boat | fire_vehicle | medical_kit | food_pack | water_unit | medicine | shelter | rescue_personnel
    private String type;
    private String category;
    private String name;

    private int quantity;
    private int available;
    private int reserved;
    private int deployed;

    // available | reserved | deployed | low_stock | critical_stock
    private String status;

    private GeoLocation location;
    private String lastRestocked;
    private String unit;
    private int lowStockThreshold;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public int getAvailable() { return available; }
    public void setAvailable(int available) { this.available = available; }
    public int getReserved() { return reserved; }
    public void setReserved(int reserved) { this.reserved = reserved; }
    public int getDeployed() { return deployed; }
    public void setDeployed(int deployed) { this.deployed = deployed; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public GeoLocation getLocation() { return location; }
    public void setLocation(GeoLocation location) { this.location = location; }
    public String getLastRestocked() { return lastRestocked; }
    public void setLastRestocked(String lastRestocked) { this.lastRestocked = lastRestocked; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public int getLowStockThreshold() { return lowStockThreshold; }
    public void setLowStockThreshold(int lowStockThreshold) { this.lowStockThreshold = lowStockThreshold; }

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
}
