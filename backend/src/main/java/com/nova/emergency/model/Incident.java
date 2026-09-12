package com.nova.emergency.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;
import java.util.Map;

@Data
@Document(collection = "incidents")
public class Incident {

    @Id
    private String id;

    private String trackingCode;

    // flood | landslide | fire | road_accident | medical | crime | missing_person | building_collapse | severe_weather | other | unknown
    private String type;

    // critical | high | medium | low
    private String severity;

    // reported | submitted | analysing | verified | prioritized | assigned | en_route | responding | on_scene | transporting | resolved | cancelled | closed
    private String status;

    private String title;
    private String description;
    private GeoLocation location;

    private String reportedBy;
    private String reporterName;
    private String reporterPhone;
    private String reportedAt;
    private String updatedAt;

    private int peopleAffected = 1;

    private AIAnalysis aiAnalysis;

    private String assignedTeam;
    private String assignedTeamId;
    private String assignedTeamName;
    private String assignedHospital;

    private Integer eta; // minutes
    private Integer priority; // 1 = highest

    private String audioUrl;
    private String audioTranscript;
    private List<String> photoUrls;
    private String manualAddress;
    private Double locationAccuracy;
    private String locationCapturedAt;
    private boolean isSilentSos = false;

    private List<String> recommendedAgencies;
    private List<AgencyNotification> notifiedAgencies;
    private List<AssignedUnit> assignedUnits;

    private String acknowledgedAt;
    private String acknowledgedBy;
    private String arrivedAt;
    private String resolvedAt;

    private List<IncidentNote> notes;
    private List<Attachment> attachments;
    private List<IncidentUpdate> updates;
    private List<ResourceAllocation> resourcesAllocated;

    private boolean isSimulation = true;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTrackingCode() { return trackingCode; }
    public void setTrackingCode(String trackingCode) { this.trackingCode = trackingCode; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public GeoLocation getLocation() { return location; }
    public void setLocation(GeoLocation location) { this.location = location; }
    public String getReportedBy() { return reportedBy; }
    public void setReportedBy(String reportedBy) { this.reportedBy = reportedBy; }
    public String getReporterName() { return reporterName; }
    public void setReporterName(String reporterName) { this.reporterName = reporterName; }
    public String getReporterPhone() { return reporterPhone; }
    public void setReporterPhone(String reporterPhone) { this.reporterPhone = reporterPhone; }
    public String getReportedAt() { return reportedAt; }
    public void setReportedAt(String reportedAt) { this.reportedAt = reportedAt; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    public int getPeopleAffected() { return peopleAffected; }
    public void setPeopleAffected(int peopleAffected) { this.peopleAffected = peopleAffected; }
    public AIAnalysis getAiAnalysis() { return aiAnalysis; }
    public void setAiAnalysis(AIAnalysis aiAnalysis) { this.aiAnalysis = aiAnalysis; }
    public String getAssignedTeam() { return assignedTeam; }
    public void setAssignedTeam(String assignedTeam) { this.assignedTeam = assignedTeam; }
    public String getAssignedTeamId() { return assignedTeamId; }
    public void setAssignedTeamId(String assignedTeamId) { this.assignedTeamId = assignedTeamId; }
    public String getAssignedTeamName() { return assignedTeamName; }
    public void setAssignedTeamName(String assignedTeamName) { this.assignedTeamName = assignedTeamName; }
    public String getAssignedHospital() { return assignedHospital; }
    public void setAssignedHospital(String assignedHospital) { this.assignedHospital = assignedHospital; }
    public Integer getEta() { return eta; }
    public void setEta(Integer eta) { this.eta = eta; }
    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }
    public String getAudioUrl() { return audioUrl; }
    public void setAudioUrl(String audioUrl) { this.audioUrl = audioUrl; }
    public String getAudioTranscript() { return audioTranscript; }
    public void setAudioTranscript(String audioTranscript) { this.audioTranscript = audioTranscript; }
    public List<String> getPhotoUrls() { return photoUrls; }
    public void setPhotoUrls(List<String> photoUrls) { this.photoUrls = photoUrls; }
    public String getManualAddress() { return manualAddress; }
    public void setManualAddress(String manualAddress) { this.manualAddress = manualAddress; }
    public Double getLocationAccuracy() { return locationAccuracy; }
    public void setLocationAccuracy(Double locationAccuracy) { this.locationAccuracy = locationAccuracy; }
    public String getLocationCapturedAt() { return locationCapturedAt; }
    public void setLocationCapturedAt(String locationCapturedAt) { this.locationCapturedAt = locationCapturedAt; }
    public boolean isSilentSos() { return isSilentSos; }
    public void setSilentSos(boolean silentSos) { isSilentSos = silentSos; }
    public List<String> getRecommendedAgencies() { return recommendedAgencies; }
    public void setRecommendedAgencies(List<String> recommendedAgencies) { this.recommendedAgencies = recommendedAgencies; }
    public List<AgencyNotification> getNotifiedAgencies() { return notifiedAgencies; }
    public void setNotifiedAgencies(List<AgencyNotification> notifiedAgencies) { this.notifiedAgencies = notifiedAgencies; }
    public List<AssignedUnit> getAssignedUnits() { return assignedUnits; }
    public void setAssignedUnits(List<AssignedUnit> assignedUnits) { this.assignedUnits = assignedUnits; }
    public String getAcknowledgedAt() { return acknowledgedAt; }
    public void setAcknowledgedAt(String acknowledgedAt) { this.acknowledgedAt = acknowledgedAt; }
    public String getAcknowledgedBy() { return acknowledgedBy; }
    public void setAcknowledgedBy(String acknowledgedBy) { this.acknowledgedBy = acknowledgedBy; }
    public String getArrivedAt() { return arrivedAt; }
    public void setArrivedAt(String arrivedAt) { this.arrivedAt = arrivedAt; }
    public String getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(String resolvedAt) { this.resolvedAt = resolvedAt; }
    public List<IncidentNote> getNotes() { return notes; }
    public void setNotes(List<IncidentNote> notes) { this.notes = notes; }
    public List<Attachment> getAttachments() { return attachments; }
    public void setAttachments(List<Attachment> attachments) { this.attachments = attachments; }
    public List<IncidentUpdate> getUpdates() { return updates; }
    public void setUpdates(List<IncidentUpdate> updates) { this.updates = updates; }
    public List<ResourceAllocation> getResourcesAllocated() { return resourcesAllocated; }
    public void setResourcesAllocated(List<ResourceAllocation> resourcesAllocated) { this.resourcesAllocated = resourcesAllocated; }
    public boolean isSimulation() { return isSimulation; }
    public void setSimulation(boolean simulation) { isSimulation = simulation; }

    // ─── Nested types ────────────────────────────────────────

    @Data
            public static class GeoLocation {
        private double lat;
        private double lng;
        private String address;
        private String district;
        private String zone;
        private Double accuracy;
        private String capturedAt;

        public GeoLocation() {}
        public GeoLocation(double lat, double lng, String address, String district) {
            this.lat = lat;
            this.lng = lng;
            this.address = address;
            this.district = district;
        }
        public GeoLocation(double lat, double lng, String address, String district, String zone, Double accuracy, String capturedAt) {
            this.lat = lat;
            this.lng = lng;
            this.address = address;
            this.district = district;
            this.zone = zone;
            this.accuracy = accuracy;
            this.capturedAt = capturedAt;
        }

        public double getLat() { return lat; }
        public void setLat(double lat) { this.lat = lat; }
        public double getLng() { return lng; }
        public void setLng(double lng) { this.lng = lng; }
        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }
        public String getDistrict() { return district; }
        public void setDistrict(String district) { this.district = district; }
        public String getZone() { return zone; }
        public void setZone(String zone) { this.zone = zone; }
        public Double getAccuracy() { return accuracy; }
        public void setAccuracy(Double accuracy) { this.accuracy = accuracy; }
        public String getCapturedAt() { return capturedAt; }
        public void setCapturedAt(String capturedAt) { this.capturedAt = capturedAt; }
    }

    @Data
            public static class AgencyNotification {
        private String id;
        private String agency;
        private String agencyName;
        private String destination;
        private String channel;
        private String sentAt;
        private String status; // queued | sent | delivered | failed | acknowledged
        private String failureReason;
        private String acknowledgedAt;
        private String acknowledgedBy;

        public AgencyNotification() {}
        public AgencyNotification(String id, String agency, String agencyName, String destination, String channel,
                                  String sentAt, String status, String failureReason, String acknowledgedAt, String acknowledgedBy) {
            this.id = id;
            this.agency = agency;
            this.agencyName = agencyName;
            this.destination = destination;
            this.channel = channel;
            this.sentAt = sentAt;
            this.status = status;
            this.failureReason = failureReason;
            this.acknowledgedAt = acknowledgedAt;
            this.acknowledgedBy = acknowledgedBy;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getAgency() { return agency; }
        public void setAgency(String agency) { this.agency = agency; }
        public String getAgencyName() { return agencyName; }
        public void setAgencyName(String agencyName) { this.agencyName = agencyName; }
        public String getDestination() { return destination; }
        public void setDestination(String destination) { this.destination = destination; }
        public String getChannel() { return channel; }
        public void setChannel(String channel) { this.channel = channel; }
        public String getSentAt() { return sentAt; }
        public void setSentAt(String sentAt) { this.sentAt = sentAt; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getFailureReason() { return failureReason; }
        public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
        public String getAcknowledgedAt() { return acknowledgedAt; }
        public void setAcknowledgedAt(String acknowledgedAt) { this.acknowledgedAt = acknowledgedAt; }
        public String getAcknowledgedBy() { return acknowledgedBy; }
        public void setAcknowledgedBy(String acknowledgedBy) { this.acknowledgedBy = acknowledgedBy; }
    }

    @Data
            public static class AssignedUnit {
        private String id;
        private String unitType;
        private String name;
        private String callSign;
        private double distanceKm;
        private int etaMinutes;
        private String status;

        public AssignedUnit() {}
        public AssignedUnit(String id, String unitType, String name, String callSign, double distanceKm, int etaMinutes, String status) {
            this.id = id;
            this.unitType = unitType;
            this.name = name;
            this.callSign = callSign;
            this.distanceKm = distanceKm;
            this.etaMinutes = etaMinutes;
            this.status = status;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getUnitType() { return unitType; }
        public void setUnitType(String unitType) { this.unitType = unitType; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getCallSign() { return callSign; }
        public void setCallSign(String callSign) { this.callSign = callSign; }
        public double getDistanceKm() { return distanceKm; }
        public void setDistanceKm(double distanceKm) { this.distanceKm = distanceKm; }
        public int getEtaMinutes() { return etaMinutes; }
        public void setEtaMinutes(int etaMinutes) { this.etaMinutes = etaMinutes; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    @Data
            public static class AIAnalysis {
        private String id;
        private String incidentId;
        private String emergencyType;
        private String severity;
        private int confidenceScore;
        private int peopleAffected;
        private List<VulnerablePerson> vulnerablePersons;
        private List<ResourceRequirement> requiredResources;
        private String recommendedAction;
        private String detectedLanguage;
        private String processedAt;
        private List<String> inputModalities;
        private List<String> riskFactors;
        private int estimatedResponseTime;

        public AIAnalysis() {}
        public AIAnalysis(String id, String incidentId, String emergencyType, String severity, int confidenceScore,
                          int peopleAffected, List<VulnerablePerson> vulnerablePersons, List<ResourceRequirement> requiredResources,
                          String recommendedAction, String detectedLanguage, String processedAt, List<String> inputModalities,
                          List<String> riskFactors, int estimatedResponseTime) {
            this.id = id;
            this.incidentId = incidentId;
            this.emergencyType = emergencyType;
            this.severity = severity;
            this.confidenceScore = confidenceScore;
            this.peopleAffected = peopleAffected;
            this.vulnerablePersons = vulnerablePersons;
            this.requiredResources = requiredResources;
            this.recommendedAction = recommendedAction;
            this.detectedLanguage = detectedLanguage;
            this.processedAt = processedAt;
            this.inputModalities = inputModalities;
            this.riskFactors = riskFactors;
            this.estimatedResponseTime = estimatedResponseTime;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getIncidentId() { return incidentId; }
        public void setIncidentId(String incidentId) { this.incidentId = incidentId; }
        public String getEmergencyType() { return emergencyType; }
        public void setEmergencyType(String emergencyType) { this.emergencyType = emergencyType; }
        public String getSeverity() { return severity; }
        public void setSeverity(String severity) { this.severity = severity; }
        public int getConfidenceScore() { return confidenceScore; }
        public void setConfidenceScore(int confidenceScore) { this.confidenceScore = confidenceScore; }
        public int getPeopleAffected() { return peopleAffected; }
        public void setPeopleAffected(int peopleAffected) { this.peopleAffected = peopleAffected; }
        public List<VulnerablePerson> getVulnerablePersons() { return vulnerablePersons; }
        public void setVulnerablePersons(List<VulnerablePerson> vulnerablePersons) { this.vulnerablePersons = vulnerablePersons; }
        public List<ResourceRequirement> getRequiredResources() { return requiredResources; }
        public void setRequiredResources(List<ResourceRequirement> requiredResources) { this.requiredResources = requiredResources; }
        public String getRecommendedAction() { return recommendedAction; }
        public void setRecommendedAction(String recommendedAction) { this.recommendedAction = recommendedAction; }
        public String getDetectedLanguage() { return detectedLanguage; }
        public void setDetectedLanguage(String detectedLanguage) { this.detectedLanguage = detectedLanguage; }
        public String getProcessedAt() { return processedAt; }
        public void setProcessedAt(String processedAt) { this.processedAt = processedAt; }
        public List<String> getInputModalities() { return inputModalities; }
        public void setInputModalities(List<String> inputModalities) { this.inputModalities = inputModalities; }
        public List<String> getRiskFactors() { return riskFactors; }
        public void setRiskFactors(List<String> riskFactors) { this.riskFactors = riskFactors; }
        public int getEstimatedResponseTime() { return estimatedResponseTime; }
        public void setEstimatedResponseTime(int estimatedResponseTime) { this.estimatedResponseTime = estimatedResponseTime; }
    }

    @Data
            public static class VulnerablePerson {
        private String type; // elderly | child | disabled | pregnant | medical
        private int count;

        public VulnerablePerson() {}
        public VulnerablePerson(String type, int count) {
            this.type = type;
            this.count = count;
        }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public int getCount() { return count; }
        public void setCount(int count) { this.count = count; }
    }

    @Data
            public static class ResourceRequirement {
        private String type;
        private int quantity;
        private String priority; // immediate | urgent | normal

        public ResourceRequirement() {}
        public ResourceRequirement(String type, int quantity, String priority) {
            this.type = type;
            this.quantity = quantity;
            this.priority = priority;
        }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
        public String getPriority() { return priority; }
        public void setPriority(String priority) { this.priority = priority; }
    }

    @Data
            public static class IncidentNote {
        private String id;
        private String author;
        private String content;
        private String createdAt;
        private boolean isAI;

        public IncidentNote() {}
        public IncidentNote(String id, String author, String content, String createdAt, boolean isAI) {
            this.id = id;
            this.author = author;
            this.content = content;
            this.createdAt = createdAt;
            this.isAI = isAI;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getAuthor() { return author; }
        public void setAuthor(String author) { this.author = author; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
        public boolean isAI() { return isAI; }
        public void setAI(boolean isAI) { this.isAI = isAI; }
    }

    @Data
            public static class Attachment {
        private String id;
        private String type; // image | audio | video | document
        private String url;
        private String name;
        private long size;

        public Attachment() {}
        public Attachment(String id, String type, String url, String name, long size) {
            this.id = id;
            this.type = type;
            this.url = url;
            this.name = name;
            this.size = size;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public long getSize() { return size; }
        public void setSize(long size) { this.size = size; }
    }

    @Data
            public static class IncidentUpdate {
        private String id;
        private String status;
        private String message;
        private String updatedBy;
        private String updatedAt;
        private boolean isAI;

        public IncidentUpdate() {}
        public IncidentUpdate(String id, String status, String message, String updatedBy, String updatedAt, boolean isAI) {
            this.id = id;
            this.status = status;
            this.message = message;
            this.updatedBy = updatedBy;
            this.updatedAt = updatedAt;
            this.isAI = isAI;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getUpdatedBy() { return updatedBy; }
        public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
        public String getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
        public boolean isAI() { return isAI; }
        public void setAI(boolean isAI) { this.isAI = isAI; }
    }

    @Data
            public static class ResourceAllocation {
        private String resourceId;
        private String resourceType;
        private int quantity;
        private String allocatedAt;

        public ResourceAllocation() {}
        public ResourceAllocation(String resourceId, String resourceType, int quantity, String allocatedAt) {
            this.resourceId = resourceId;
            this.resourceType = resourceType;
            this.quantity = quantity;
            this.allocatedAt = allocatedAt;
        }

        public String getResourceId() { return resourceId; }
        public void setResourceId(String resourceId) { this.resourceId = resourceId; }
        public String getResourceType() { return resourceType; }
        public void setResourceType(String resourceType) { this.resourceType = resourceType; }
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
        public String getAllocatedAt() { return allocatedAt; }
        public void setAllocatedAt(String allocatedAt) { this.allocatedAt = allocatedAt; }
    }
}
