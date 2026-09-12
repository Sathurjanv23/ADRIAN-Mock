package com.nova.emergency.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "shelters")
public class Shelter {

    @Id
    private String id;

    private String name;
    private GeoLocation location;

    private int capacity;
    private int currentOccupancy;

    // active | standby | full | closed
    private String status;

    private List<String> amenities;
    private String contact;
    private String district;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public GeoLocation getLocation() { return location; }
    public void setLocation(GeoLocation location) { this.location = location; }
    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }
    public int getCurrentOccupancy() { return currentOccupancy; }
    public void setCurrentOccupancy(int currentOccupancy) { this.currentOccupancy = currentOccupancy; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public List<String> getAmenities() { return amenities; }
    public void setAmenities(List<String> amenities) { this.amenities = amenities; }
    public String getContact() { return contact; }
    public void setContact(String contact) { this.contact = contact; }
    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }

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
