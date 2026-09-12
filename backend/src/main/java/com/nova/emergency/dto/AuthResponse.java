package com.nova.emergency.dto;

import com.nova.emergency.model.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String accessToken;
    private String tokenType = "Bearer";
    private long expiresIn;   // seconds
    private UserDto user;

    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
    public String getTokenType() { return tokenType; }
    public void setTokenType(String tokenType) { this.tokenType = tokenType; }
    public long getExpiresIn() { return expiresIn; }
    public void setExpiresIn(long expiresIn) { this.expiresIn = expiresIn; }
    public UserDto getUser() { return user; }
    public void setUser(UserDto user) { this.user = user; }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserDto {
        private String id;
        private String name;
        private String email;
        private String role;
        private String approvalStatus;
        private String rescueTeamId;
        private String hospitalId;
        private String status;
        private String phone;
        private String district;
        private String organization;
        private String language;
        private String createdAt;
        private String lastActive;
        private boolean isActive;
        private boolean isVerified;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
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

        public static UserDto from(User user) {
            UserDto dto = new UserDto();
            dto.setId(user.getId());
            dto.setName(user.getName());
            dto.setEmail(user.getEmail());
            dto.setRole(user.getRole());
            dto.setApprovalStatus(user.getApprovalStatus());
            dto.setRescueTeamId(user.getRescueTeamId());
            dto.setHospitalId(user.getHospitalId());
            dto.setStatus(user.getStatus() != null ? user.getStatus() : (user.isActive() ? "ACTIVE" : "DEACTIVATED"));
            dto.setPhone(user.getPhone());
            dto.setDistrict(user.getDistrict());
            dto.setOrganization(user.getOrganization());
            dto.setLanguage(user.getLanguage());
            dto.setCreatedAt(user.getCreatedAt());
            dto.setLastActive(user.getLastActive());
            dto.setActive(user.isActive());
            dto.setVerified(user.isVerified());
            return dto;
        }
    }
}
