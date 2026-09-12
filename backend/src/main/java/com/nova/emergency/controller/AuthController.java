package com.nova.emergency.controller;

import com.nova.emergency.dto.*;
import com.nova.emergency.service.AuthService;
import com.nova.emergency.service.GoogleOAuthService;
import com.nova.emergency.repository.UserRepository;
import com.nova.emergency.model.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;
    private final GoogleOAuthService googleOAuthService;
    @Autowired(required = false)
    private UserRepository userRepository;

    public AuthController(AuthService authService, GoogleOAuthService googleOAuthService) {
        this.authService = authService;
        this.googleOAuthService = googleOAuthService;
    }

    @Value("${spring.security.oauth2.client.registration.google.client-id:NOT_SET}")
    private String googleClientId;

    @Value("${nova.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    // ─── Standard Email/Password Register ──────────────────────

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse auth = authService.register(request);
        return ResponseEntity.ok(ApiResponse.ok(auth, "Registration successful. Check your email for a verification code."));
    }

    // ─── Standard Email/Password Login ─────────────────────────

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse auth = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok(auth, "Login successful"));
    }

    // ─── Email-only OTP Request (Command Center login) ─────────

    @PostMapping("/request-otp")
    public ResponseEntity<ApiResponse<String>> requestOtp(@Valid @RequestBody OtpRequestDto request) {
        authService.requestLoginOtp(request.getEmail());
        return ResponseEntity.ok(ApiResponse.ok(
            "sent",
            "A 6-digit verification code has been sent to your email. It expires in 30 seconds."
        ));
    }

    // ─── OTP Verification ──────────────────────────────────────

    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        AuthResponse auth = authService.verifyOtp(request);
        return ResponseEntity.ok(ApiResponse.ok(auth, "Email verification successful"));
    }

    // ─── Resend OTP ────────────────────────────────────────────

    @PostMapping("/resend-otp")
    public ResponseEntity<ApiResponse<String>> resendOtp(@RequestParam String email) {
        authService.resendOtp(email);
        return ResponseEntity.ok(ApiResponse.ok(
            "sent",
            "A new verification code has been sent. It expires in 30 seconds."
        ));
    }

    // ─── Forgot Password Request ──────────────────────────────

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<String>> forgotPassword(@Valid @RequestBody OtpRequestDto request) {
        authService.requestPasswordReset(request.getEmail());
        return ResponseEntity.ok(ApiResponse.ok(
            "sent",
            "If an account is associated with this email, a verification code has been sent."
        ));
    }

    // ─── Reset Password ───────────────────────────────────────

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<AuthResponse>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        AuthResponse auth = authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.ok(auth, "Password has been successfully reset."));
    }

    // ─── Google OAuth — Initiate ────────────────────────────────

    @GetMapping("/oauth2/google")
    public ResponseEntity<Void> initiateGoogleOAuth() {
        if (!googleOAuthService.isConfigured()) {
            log.warn("Google OAuth is not configured. Returning 503.");
            return ResponseEntity.status(503).build();
        }
        String googleAuthUrl = googleOAuthService.buildGoogleAuthUrl();
        return ResponseEntity.status(302)
            .header("Location", googleAuthUrl)
            .build();
    }

    // ─── Google OAuth — Callback Handler ────────────────────────

    /**
     * Receives OAuth authorization code from Google.
     * Redirects to the frontend callback without exposing JWTs or sensitive secrets in URLs.
     */
    @GetMapping("/oauth2/callback/google")
    public ResponseEntity<Void> handleGoogleCallback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String redirect_uri,
            @RequestParam(required = false) String state) {

        if (error != null || code == null) {
            log.warn("Google OAuth error from provider: {}", error);
            String redirectUrl = frontendUrl + "/login?error=google_auth_failed";
            return ResponseEntity.status(302).header("Location", redirectUrl).build();
        }

        try {
            AuthService.GoogleAuthResult result = googleOAuthService.handleCallback(code, redirect_uri);

            if (result.isNewUser()) {
                // New user: Redirect to frontend role-selection screen with short-lived intent ticket
                String redirectUrl = frontendUrl + "/auth/callback?intent=" + result.getRegistrationIntent()
                    + "&email=" + URLEncoder.encode(result.getEmail(), StandardCharsets.UTF_8)
                    + "&name=" + URLEncoder.encode(result.getName() != null ? result.getName() : "", StandardCharsets.UTF_8);
                return ResponseEntity.status(302).header("Location", redirectUrl).build();
            } else {
                // Existing user: Redirect with temporary single-use exchange code (NO JWT in URL)
                String redirectUrl = frontendUrl + "/auth/callback?code=" + result.getAuthCode();
                return ResponseEntity.status(302).header("Location", redirectUrl).build();
            }
        } catch (Exception e) {
            log.error("Google OAuth callback processing failed: {}", e.getMessage());
            String redirectUrl = frontendUrl + "/login?error=google_auth_failed";
            return ResponseEntity.status(302).header("Location", redirectUrl).build();
        }
    }

    // ─── Exchange OAuth Code for JWT Session ────────────────────

    /**
     * Frontend exchanges single-use code for the authenticated JWT in response body.
     */
    @PostMapping("/oauth2/exchange")
    public ResponseEntity<ApiResponse<AuthResponse>> exchangeOAuthCode(
            @Valid @RequestBody OAuthExchangeRequest request) {
        AuthResponse auth = authService.exchangeOAuthCode(request.getCode());
        return ResponseEntity.ok(ApiResponse.ok(auth, "Authentication successful"));
    }

    // ─── Complete Google Registration (New User) ───────────────

    /**
     * Completes registration for new Google users after mandatory role selection.
     */
    @PostMapping("/google/register")
    public ResponseEntity<ApiResponse<AuthResponse>> completeGoogleRegistration(
            @Valid @RequestBody GoogleRegisterRequest request) {
        AuthResponse auth = authService.completeGoogleRegistration(request);
        return ResponseEntity.ok(ApiResponse.ok(auth, "Google registration completed successfully"));
    }

    // ─── Logout ────────────────────────────────────────────────

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout() {
        return ResponseEntity.ok(ApiResponse.ok("logged_out", "Logout successful"));
    }

    // ─── Me ────────────────────────────────────────────────────

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<AuthResponse.UserDto>> me(Principal principal) {
        if (userRepository == null) {
            throw new IllegalStateException("User repository is unavailable.");
        }
        User user = userRepository.findByEmail(principal.getName().toLowerCase().trim())
            .orElseThrow(() -> new IllegalArgumentException("Authenticated user account not found."));
        return ResponseEntity.ok(ApiResponse.ok(AuthResponse.UserDto.from(user), "OK"));
    }
}
