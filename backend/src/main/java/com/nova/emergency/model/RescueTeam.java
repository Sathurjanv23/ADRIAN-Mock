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
@Document(collection = "rescue_teams")
public class RescueTeam {

    @Id
    private String id;

    private String name;
    private String code;

    // available | assigned | en_route | on_scene | unavailable
    private String status;

    private GeoLocation location;
    private List<TeamMember> members;
    private List<Equipment> equipment;
    private List<String> capabilities;

    private String district;
    private String contact;
    private String vehicleType;

    private String currentIncident;
    private Integer eta;
    private String lastUpdated;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public GeoLocation getLocation() { return location; }
    public void setLocation(GeoLocation location) { this.location = location; }
    public List<TeamMember> getMembers() { return members; }
    public void setMembers(List<TeamMember> members) { this.members = members; }
    public List<Equipment> getEquipment() { return equipment; }
    public void setEquipment(List<Equipment> equipment) { this.equipment = equipment; }
    public List<String> getCapabilities() { return capabilities; }
    public void setCapabilities(List<String> capabilities) { this.capabilities = capabilities; }
    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }
    public String getContact() { return contact; }
    public void setContact(String contact) { this.contact = contact; }
    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }
    public String getCurrentIncident() { return currentIncident; }
    public void setCurrentIncident(String currentIncident) { this.currentIncident = currentIncident; }
    public Integer getEta() { return eta; }
    public void setEta(Integer eta) { this.eta = eta; }
    public String getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(String lastUpdated) { this.lastUpdated = lastUpdated; }

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

    @Data
    public static class TeamMember {
        private String id;
        private String name;
        private String role;
        private String status; // active | standby | off_duty

        public TeamMember() {}
        public TeamMember(String id, String name, String role, String status) {
            this.id = id;
            this.name = name;
            this.role = role;
            this.status = status;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    @Data
    public static class Equipment {
        private String type;
        private int quantity;
        private String status; // available | in_use | maintenance

        public Equipment() {}
        public Equipment(String type, int quantity, String status) {
            this.type = type;
            this.quantity = quantity;
            this.status = status;
        }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
}
