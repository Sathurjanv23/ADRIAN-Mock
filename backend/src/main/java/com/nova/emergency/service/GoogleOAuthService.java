package com.nova.emergency.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

/**
 * GoogleOAuthService — Handles Google OAuth2 authorization code flow.
 * Supports both direct backend callback and frontend callback proxying.
 */
@Service
public class GoogleOAuthService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GoogleOAuthService.class);

    private final AuthService authService;

    public GoogleOAuthService(AuthService authService) {
        this.authService = authService;
    }

    @Value("${spring.security.oauth2.client.registration.google.client-id:NOT_SET}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret:NOT_SET}")
    private String clientSecret;

    @Value("${spring.security.oauth2.client.registration.google.redirect-uri:http://localhost:8080/api/auth/oauth2/callback/google}")
    private String configuredRedirectUri;

    @Value("${nova.backend.url:http://localhost:8080}")
    private String backendUrl;

    private static final String GOOGLE_TOKEN_ENDPOINT  = "https://oauth2.googleapis.com/token";
    private static final String GOOGLE_USERINFO_ENDPOINT = "https://www.googleapis.com/oauth2/v3/userinfo";
    private static final String GOOGLE_AUTH_ENDPOINT   = "https://accounts.google.com/o/oauth2/v2/auth";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public boolean isConfigured() {
        return hasValue(clientId) && hasValue(clientSecret);
    }

    private boolean hasValue(String value) {
        return value != null && !value.isBlank()
            && !"NOT_SET".equalsIgnoreCase(value.trim())
            && !value.trim().startsWith("placeholder-");
    }

    public String getEffectiveRedirectUri() {
        if (configuredRedirectUri != null && !configuredRedirectUri.isBlank()) {
            return configuredRedirectUri.trim();
        }
        return backendUrl.replaceAll("/+$", "") + "/api/auth/oauth2/callback/google";
    }

    /**
     * Builds the Google OAuth authorization URL using the exact redirect URI.
     */
    public String buildGoogleAuthUrl() {
        if (!isConfigured()) {
            throw new IllegalStateException("Google OAuth is not configured. Client ID is missing.");
        }
        String redirectUri = getEffectiveRedirectUri();
        return GOOGLE_AUTH_ENDPOINT
            + "?client_id=" + encode(clientId.trim())
            + "&redirect_uri=" + encode(redirectUri)
            + "&response_type=code"
            + "&scope=" + encode("openid email profile")
            + "&access_type=offline"
            + "&prompt=select_account";
    }

    /**
     * Exchanges code for Google User Profile and processes identity via AuthService.
     * Returns GoogleAuthResult indicating whether the account is existing or new.
     */
    public AuthService.GoogleAuthResult handleCallback(String code, String customRedirectUri) throws IOException, InterruptedException {
        if (!isConfigured() || clientSecret == null || clientSecret.isBlank() || "NOT_SET".equalsIgnoreCase(clientSecret.trim())) {
            throw new IllegalStateException("Google OAuth credentials are not fully configured.");
        }

        String redirectUri = getEffectiveRedirectUri();

        log.info("Exchanging Google code with redirect_uri: {}", redirectUri);

        String tokenResponse = exchangeCodeForToken(code, redirectUri);
        JsonNode tokenJson = objectMapper.readTree(tokenResponse);

        String accessToken = tokenJson.path("access_token").asText();
        if (accessToken == null || accessToken.isBlank()) {
            log.error("Google token exchange failed response: {}", tokenResponse);
            throw new IllegalArgumentException("Failed to obtain access token from Google: " + tokenResponse);
        }

        JsonNode userInfo = fetchUserInfo(accessToken);
        String googleId = userInfo.path("sub").asText();
        String email    = userInfo.path("email").asText();
        String name     = userInfo.path("name").asText();

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Google account did not provide an email address.");
        }

        log.info("Google OAuth identity verified: {} ({})", email, googleId);
        return authService.processGoogleIdentity(googleId, email, name);
    }

    public AuthService.GoogleAuthResult handleCallback(String code) throws IOException, InterruptedException {
        return handleCallback(code, null);
    }

    // ─── Private Helpers ───────────────────────────────────────

    private String exchangeCodeForToken(String code, String redirectUri) throws IOException, InterruptedException {
        String formBody = "code=" + encode(code)
            + "&client_id=" + encode(clientId.trim())
            + "&client_secret=" + encode(clientSecret.trim())
            + "&redirect_uri=" + encode(redirectUri)
            + "&grant_type=authorization_code";

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(GOOGLE_TOKEN_ENDPOINT))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(formBody))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }

    private JsonNode fetchUserInfo(String accessToken) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(GOOGLE_USERINFO_ENDPOINT))
            .header("Authorization", "Bearer " + accessToken)
            .GET()
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return objectMapper.readTree(response.body());
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
