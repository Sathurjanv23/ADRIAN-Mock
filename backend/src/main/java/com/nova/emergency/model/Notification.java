package com.nova.emergency.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "notifications")
public class Notification {

    @Id
    private String id;

    /**
     * Exact recipient user ID. When set, this notification belongs exclusively to that user.
     * Security: the API will only return notifications where userId matches the authenticated principal.
     */
    @Indexed
    private String userId;

    // critical_incident | new_assignment | weather_alert | resource_shortage | hospital_capacity | prediction_alert | system_alert | team_status | simulation
    private String type;

    private String title;
    private String message;

    // critical | high | medium | low
    private String severity;

    private boolean read = false;
    private String createdAt;

    // Optional: links notification to an incident or other entity
    private String relatedId;
    private String relatedType;

    // Target roles for metadata/fallback filtering: citizen | officer | rescue_team | hospital | admin
    private List<String> targetRole;

    // ─── Getters & Setters ────────────────────────────────────────

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getRelatedId() { return relatedId; }
    public void setRelatedId(String relatedId) { this.relatedId = relatedId; }

    public String getRelatedType() { return relatedType; }
    public void setRelatedType(String relatedType) { this.relatedType = relatedType; }

    public List<String> getTargetRole() { return targetRole; }
    public void setTargetRole(List<String> targetRole) { this.targetRole = targetRole; }
}
