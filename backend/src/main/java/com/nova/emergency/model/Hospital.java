package com.nova.emergency.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;

@Data
@Document(collection = "hospitals")
public class Hospital {

    @Id
    private String id;

    private String name;
    // general | specialist | field | teaching
    private String type;
    private String district;
    private GeoLocation location;
    private String contact;

    private int totalBeds;
    private int availableBeds;
    private int icuTotal;
    private int icuAvailable;
    private int emergencyTeams;

    private List<AmbulanceUnit> ambulances;
    private List<IncomingCase> incomingCases;
    private List<String> specializations;

    private boolean isActive = true;
    private String lastUpdated;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }
    public GeoLocation getLocation() { return location; }
    public void setLocation(GeoLocation location) { this.location = location; }
    public String getContact() { return contact; }
    public void setContact(String contact) { this.contact = contact; }
    public int getTotalBeds() { return totalBeds; }
    public void setTotalBeds(int totalBeds) { this.totalBeds = totalBeds; }
    public int getAvailableBeds() { return availableBeds; }
    public void setAvailableBeds(int availableBeds) { this.availableBeds = availableBeds; }
    public int getIcuTotal() { return icuTotal; }
    public void setIcuTotal(int icuTotal) { this.icuTotal = icuTotal; }
    public int getIcuAvailable() { return icuAvailable; }
    public void setIcuAvailable(int icuAvailable) { this.icuAvailable = icuAvailable; }
    public int getEmergencyTeams() { return emergencyTeams; }
    public void setEmergencyTeams(int emergencyTeams) { this.emergencyTeams = emergencyTeams; }
    public List<AmbulanceUnit> getAmbulances() { return ambulances; }
    public void setAmbulances(List<AmbulanceUnit> ambulances) { this.ambulances = ambulances; }
    public List<IncomingCase> getIncomingCases() { return incomingCases; }
    public void setIncomingCases(List<IncomingCase> incomingCases) { this.incomingCases = incomingCases; }
    public List<String> getSpecializations() { return specializations; }
    public void setSpecializations(List<String> specializations) { this.specializations = specializations; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
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
            public static class AmbulanceUnit {
        private String id;
        private String code;
        private String status; // available | dispatched | maintenance
        private Integer eta;
        private String assignedCase;

        public AmbulanceUnit() {}
        public AmbulanceUnit(String id, String code, String status, Integer eta, String assignedCase) {
            this.id = id;
            this.code = code;
            this.status = status;
            this.eta = eta;
            this.assignedCase = assignedCase;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public Integer getEta() { return eta; }
        public void setEta(Integer eta) { this.eta = eta; }
        public String getAssignedCase() { return assignedCase; }
        public void setAssignedCase(String assignedCase) { this.assignedCase = assignedCase; }
    }

    @Data
            public static class IncomingCase {
        private String id;
        private String incidentId;
        private String severity;
        private int eta;
        private int patientCount;
        private String condition;
        private List<String> requiredCare;
        private String ambulanceId;

        public IncomingCase() {}
        public IncomingCase(String id, String incidentId, String severity, int eta, int patientCount,
                            String condition, List<String> requiredCare, String ambulanceId) {
            this.id = id;
            this.incidentId = incidentId;
            this.severity = severity;
            this.eta = eta;
            this.patientCount = patientCount;
            this.condition = condition;
            this.requiredCare = requiredCare;
            this.ambulanceId = ambulanceId;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getIncidentId() { return incidentId; }
        public void setIncidentId(String incidentId) { this.incidentId = incidentId; }
        public String getSeverity() { return severity; }
        public void setSeverity(String severity) { this.severity = severity; }
        public int getEta() { return eta; }
        public void setEta(int eta) { this.eta = eta; }
        public int getPatientCount() { return patientCount; }
        public void setPatientCount(int patientCount) { this.patientCount = patientCount; }
        public String getCondition() { return condition; }
        public void setCondition(String condition) { this.condition = condition; }
        public List<String> getRequiredCare() { return requiredCare; }
        public void setRequiredCare(List<String> requiredCare) { this.requiredCare = requiredCare; }
        public String getAmbulanceId() { return ambulanceId; }
        public void setAmbulanceId(String ambulanceId) { this.ambulanceId = ambulanceId; }
    }
}
