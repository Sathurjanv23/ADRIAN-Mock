package com.nova.emergency.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users")
public class User {

    @Id
    private String id;

    private String name;

    @Indexed(unique = true)
    private String email;

    private String passwordHash;

    // citizen | officer | rescue_team | hospital | admin
    private String role;

    // APPROVED for citizens and approved privileged users; PENDING_APPROVAL or REJECTED otherwise.
    private String approvalStatus = "APPROVED";
    private String rescueTeamId;
    private String hospitalId;

    // ACTIVE | PENDING_VERIFICATION | DEACTIVATED
    private String status = "ACTIVE";

    private String phone;
    private String district;
    private String organization;

    // en | ta | si
    private String language = "en";

    private String createdAt;
    private String lastActive;
    private boolean isActive = true;
    private boolean isVerified = false;

    /**
     * Stores SHA-256 hex hash of the OTP (never stored in plaintext).
     */
    private String otpHash;

    private Instant otpExpiresAt;

    /**
     * Tracks number of OTP resend attempts within the rate-limit window.
     */
    private int otpAttempts = 0;

    /**
     * Timestamp of when the last OTP was issued, used for rate limiting.
     */
    private Instant otpRequestedAt;

    /**
     * Google OAuth subject ID (sub claim), set when provider = "google".
     */
    private String googleId;

    // email | google
    private String provider = "email";

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getApprovalStatus() { return approvalStatus; }
    public void setApprovalStatus(String approvalStatus) { this.approvalStatus = approvalStatus; }
    public String getRescueTeamId() { return rescueTeamId; }
    public void setRescueTeamId(String rescueTeamId) { this.rescueTeamId = rescueTeamId; }
    public String getHospitalId() { return hospitalId; }
    public void setHospitalId(String hospitalId) { this.hospitalId = hospitalId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }
    public String getOrganization() { return organization; }
    public void setOrganization(String organization) { this.organization = organization; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getLastActive() { return lastActive; }
    public void setLastActive(String lastActive) { this.lastActive = lastActive; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    public boolean isVerified() { return isVerified; }
    public void setVerified(boolean verified) { isVerified = verified; }
    public String getOtpHash() { return otpHash; }
    public void setOtpHash(String otpHash) { this.otpHash = otpHash; }
    public Instant getOtpExpiresAt() { return otpExpiresAt; }
    public void setOtpExpiresAt(Instant otpExpiresAt) { this.otpExpiresAt = otpExpiresAt; }
    public int getOtpAttempts() { return otpAttempts; }
    public void setOtpAttempts(int otpAttempts) { this.otpAttempts = otpAttempts; }
    public Instant getOtpRequestedAt() { return otpRequestedAt; }
    public void setOtpRequestedAt(Instant otpRequestedAt) { this.otpRequestedAt = otpRequestedAt; }
    public String getGoogleId() { return googleId; }
    public void setGoogleId(String googleId) { this.googleId = googleId; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
}
