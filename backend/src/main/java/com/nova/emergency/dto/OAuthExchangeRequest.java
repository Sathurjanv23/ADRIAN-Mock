package com.nova.emergency.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OAuthExchangeRequest {
    @NotBlank(message = "Authorization exchange code is required")
    private String code;

    public OAuthExchangeRequest() {}

    public OAuthExchangeRequest(String code) {
        this.code = code;
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
}
