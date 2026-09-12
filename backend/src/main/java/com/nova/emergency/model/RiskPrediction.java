package com.nova.emergency.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;

@Data
@Document(collection = "risk_predictions")
public class RiskPrediction {

    @Id
    private String id;

    private String zone;
    private String district;
    private GeoLocation location;

    private int floodRisk;      // 0-100
    private int landslideRisk;
    private int fireRisk;
    private int overallRisk;

    // critical | high | medium | low
    private String riskLevel;

    private double rainfall;        // mm
    private double riverLevel;      // meters
    private double temperature;     // celsius
    private double windSpeed;

    private int populationDensity;
    private int predictionWindow;   // 1 | 3 | 6 hours
    private int confidence;

    private List<String> aiRecommendations;
    private String historicalComparison;
    private int affectedPopulation;
    private String updatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getZone() { return zone; }
    public void setZone(String zone) { this.zone = zone; }
    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }
    public GeoLocation getLocation() { return location; }
    public void setLocation(GeoLocation location) { this.location = location; }
    public int getFloodRisk() { return floodRisk; }
    public void setFloodRisk(int floodRisk) { this.floodRisk = floodRisk; }
    public int getLandslideRisk() { return landslideRisk; }
    public void setLandslideRisk(int landslideRisk) { this.landslideRisk = landslideRisk; }
    public int getFireRisk() { return fireRisk; }
    public void setFireRisk(int fireRisk) { this.fireRisk = fireRisk; }
    public int getOverallRisk() { return overallRisk; }
    public void setOverallRisk(int overallRisk) { this.overallRisk = overallRisk; }
    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    public double getRainfall() { return rainfall; }
    public void setRainfall(double rainfall) { this.rainfall = rainfall; }
    public double getRiverLevel() { return riverLevel; }
    public void setRiverLevel(double riverLevel) { this.riverLevel = riverLevel; }
    public double getTemperature() { return temperature; }
    public void setTemperature(double temperature) { this.temperature = temperature; }
    public double getWindSpeed() { return windSpeed; }
    public void setWindSpeed(double windSpeed) { this.windSpeed = windSpeed; }
    public int getPopulationDensity() { return populationDensity; }
    public void setPopulationDensity(int populationDensity) { this.populationDensity = populationDensity; }
    public int getPredictionWindow() { return predictionWindow; }
    public void setPredictionWindow(int predictionWindow) { this.predictionWindow = predictionWindow; }
    public int getConfidence() { return confidence; }
    public void setConfidence(int confidence) { this.confidence = confidence; }
    public List<String> getAiRecommendations() { return aiRecommendations; }
    public void setAiRecommendations(List<String> aiRecommendations) { this.aiRecommendations = aiRecommendations; }
    public String getHistoricalComparison() { return historicalComparison; }
    public void setHistoricalComparison(String historicalComparison) { this.historicalComparison = historicalComparison; }
    public int getAffectedPopulation() { return affectedPopulation; }
    public void setAffectedPopulation(int affectedPopulation) { this.affectedPopulation = affectedPopulation; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    @Data
            public static class GeoLocation {
        private double lat;
        private double lng;

        public GeoLocation() {}
        public GeoLocation(double lat, double lng) {
            this.lat = lat;
            this.lng = lng;
        }

        public double getLat() { return lat; }
        public void setLat(double lat) { this.lat = lat; }
        public double getLng() { return lng; }
        public void setLng(double lng) { this.lng = lng; }
    }
}
