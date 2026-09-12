package com.nova.emergency.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GoogleRegisterRequest {

    @NotBlank(message = "Registration intent token is required")
    private String registrationIntent;

    @NotBlank(message = "Role selection is required")
    private String role;

    private String phone;
    private String district;
    private String organization;
    private String language = "en";

    public GoogleRegisterRequest() {}

    public String getRegistrationIntent() { return registrationIntent; }
    public void setRegistrationIntent(String registrationIntent) { this.registrationIntent = registrationIntent; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }
    public String getOrganization() { return organization; }
    public void setOrganization(String organization) { this.organization = organization; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
}
