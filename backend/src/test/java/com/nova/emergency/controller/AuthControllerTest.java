package com.nova.emergency.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nova.emergency.dto.AuthResponse;
import com.nova.emergency.dto.RegisterRequest;
import com.nova.emergency.dto.VerifyOtpRequest;
import com.nova.emergency.exception.AccountConflictException;
import com.nova.emergency.security.JwtFilter;
import com.nova.emergency.service.AuthService;
import com.nova.emergency.service.GoogleOAuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {AuthController.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private com.nova.emergency.service.AuthService authService;

    @MockBean
    private com.nova.emergency.service.GoogleOAuthService googleOAuthService;

    @MockBean
    private com.nova.emergency.security.JwtFilter jwtFilter;

    @Test
    @DisplayName("API Test: Duplicate email returns 409 Conflict with proper error message")
    void testDuplicateEmailReturns409Conflict() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setName("Kamal Silva");
        req.setEmail("kamal@nova.lk");
        req.setPassword("SecurePass123");
        req.setRole("citizen");

        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new AccountConflictException("An account with this email already exists."));

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("An account with this email already exists."));
    }

    @Test
    @DisplayName("API Test: MongoDB DuplicateKeyException returns 409 Conflict")
    void testDatabaseDuplicateKeyExceptionReturns409Conflict() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setName("Kamal Silva");
        req.setEmail("kamal@nova.lk");
        req.setPassword("SecurePass123");
        req.setRole("citizen");

        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new DuplicateKeyException("E11000 duplicate key error collection: users index: email_1 dup key: { email: \"kamal@nova.lk\" }"));

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("An account with this email already exists."));
    }

    @Test
    @DisplayName("API Test: Expired OTP returns 400 Bad Request with clear expiration message")
    void testExpiredOtpReturns400BadRequest() throws Exception {
        VerifyOtpRequest verifyReq = new VerifyOtpRequest("kamal@nova.lk", "123456");

        when(authService.verifyOtp(any(VerifyOtpRequest.class)))
                .thenThrow(new IllegalArgumentException("OTP has expired. Please request a new OTP."));

        mockMvc.perform(post("/api/auth/verify-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(verifyReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("OTP has expired. Please request a new OTP."));
    }

    @Test
    @DisplayName("API Test: Invalid OTP returns 400 Bad Request with Invalid OTP message")
    void testInvalidOtpReturns400BadRequest() throws Exception {
        VerifyOtpRequest verifyReq = new VerifyOtpRequest("kamal@nova.lk", "999999");

        when(authService.verifyOtp(any(VerifyOtpRequest.class)))
                .thenThrow(new IllegalArgumentException("Invalid OTP."));

        mockMvc.perform(post("/api/auth/verify-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(verifyReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid OTP."));
    }

    @Test
    @DisplayName("API Test: Valid OTP within 30 seconds returns 200 OK with AuthResponse")
    void testValidOtpReturns200Ok() throws Exception {
        VerifyOtpRequest verifyReq = new VerifyOtpRequest("kamal@nova.lk", "654321");

        AuthResponse authRes = new AuthResponse();
        authRes.setAccessToken("valid-jwt-token");
        authRes.setTokenType("Bearer");
        authRes.setExpiresIn(86400);

        when(authService.verifyOtp(any(VerifyOtpRequest.class))).thenReturn(authRes);

        mockMvc.perform(post("/api/auth/verify-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(verifyReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("valid-jwt-token"));
    }

    @Test
    @DisplayName("API Test: Initiate Google OAuth returns 302 Redirect to Google Auth URL")
    void testInitiateGoogleOAuthReturns302() throws Exception {
        when(googleOAuthService.isConfigured()).thenReturn(true);
        when(googleOAuthService.buildGoogleAuthUrl()).thenReturn("https://accounts.google.com/o/oauth2/v2/auth?client_id=test");

        mockMvc.perform(get("/api/auth/oauth2/google"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://accounts.google.com/o/oauth2/v2/auth?client_id=test"));
    }

    @Test
    @DisplayName("API Test: Google Callback with existing user redirects to frontend with auth code")
    void testGoogleCallbackExistingUserRedirectsWithCode() throws Exception {
        AuthService.GoogleAuthResult result = AuthService.GoogleAuthResult.existingUser("auth-code-12345");
        when(googleOAuthService.handleCallback(eq("mock-google-code"), any())).thenReturn(result);

        mockMvc.perform(get("/api/auth/oauth2/callback/google")
                .param("code", "mock-google-code"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/auth/callback?code=auth-code-12345")));
    }

    @Test
    @DisplayName("API Test: Google Callback with new user redirects to frontend with intent ticket")
    void testGoogleCallbackNewUserRedirectsWithIntent() throws Exception {
        AuthService.GoogleAuthResult result = AuthService.GoogleAuthResult.newUser("intent-ticket-abc", "newuser@gmail.com", "New User");
        when(googleOAuthService.handleCallback(eq("mock-google-code"), any())).thenReturn(result);

        mockMvc.perform(get("/api/auth/oauth2/callback/google")
                .param("code", "mock-google-code"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/auth/callback?intent=intent-ticket-abc")));
    }

    @Test
    @DisplayName("API Test: Exchange OAuth code returns JWT token")
    void testExchangeOAuthCodeReturnsJwt() throws Exception {
        com.nova.emergency.dto.OAuthExchangeRequest exchangeReq = new com.nova.emergency.dto.OAuthExchangeRequest();
        exchangeReq.setCode("valid-auth-code");

        AuthResponse authRes = new AuthResponse();
        authRes.setAccessToken("jwt-token-google");
        authRes.setTokenType("Bearer");

        when(authService.exchangeOAuthCode("valid-auth-code")).thenReturn(authRes);

        mockMvc.perform(post("/api/auth/oauth2/exchange")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(exchangeReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("jwt-token-google"));
    }
}
