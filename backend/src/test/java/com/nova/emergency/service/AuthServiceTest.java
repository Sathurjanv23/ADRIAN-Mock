package com.nova.emergency.service;

import com.nova.emergency.dto.AuthResponse;
import com.nova.emergency.dto.GoogleRegisterRequest;
import com.nova.emergency.dto.RegisterRequest;
import com.nova.emergency.dto.VerifyOtpRequest;
import com.nova.emergency.exception.AccountConflictException;
import com.nova.emergency.model.User;
import com.nova.emergency.repository.UserRepository;
import com.nova.emergency.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "otpExpirySeconds", 30);
    }

    private String hashOtp(String otp) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(otp.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ─── Test 1: New Email Registration ───────────────────────
    @Test
    @DisplayName("Test 1: New email -> registration succeeds -> OTP sent, user is unverified")
    void test1_newEmailRegistrationSucceeds() {
        RegisterRequest req = new RegisterRequest();
        req.setName("Kamal Silva");
        req.setEmail("kamal.silva@nova.lk");
        req.setPassword("SecurePass123");
        req.setRole("citizen");
        req.setDistrict("Colombo");

        when(userRepository.findByEmail("kamal.silva@nova.lk")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("SecurePass123")).thenReturn("encodedPasswordHash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId("u-123");
            return u;
        });

        AuthResponse res = authService.register(req);

        assertNotNull(res);
        assertNull(res.getAccessToken(), "Access token must be null before OTP verification");
        assertFalse(res.getUser().isVerified(), "User must be unverified initially");
        assertEquals("kamal.silva@nova.lk", res.getUser().getEmail());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertFalse(savedUser.isVerified());
        assertFalse(savedUser.isActive());
        assertEquals("PENDING_VERIFICATION", savedUser.getStatus());
        assertNotNull(savedUser.getOtpHash());
        assertNotNull(savedUser.getOtpExpiresAt());

        // OTP expiration must be ~30 seconds in the future
        long secondsUntilExpiry = ChronoUnit.SECONDS.between(Instant.now(), savedUser.getOtpExpiresAt());
        assertTrue(secondsUntilExpiry >= 28 && secondsUntilExpiry <= 30, "OTP must expire in ~30 seconds");

        verify(emailService, times(1)).sendOtpEmail(eq("kamal.silva@nova.lk"), anyString(), eq("Kamal Silva"));
    }

    // ─── Test 2: Duplicate Email ──────────────────────────────
    @Test
    @DisplayName("Test 2: Duplicate email -> registration rejected with 409 AccountConflictException")
    void test2_duplicateEmailRejected() {
        RegisterRequest req = new RegisterRequest();
        req.setName("Kamal Silva");
        req.setEmail("kamal.silva@nova.lk");
        req.setPassword("SecurePass123");
        req.setRole("citizen");

        User existingUser = new User();
        existingUser.setEmail("kamal.silva@nova.lk");
        when(userRepository.findByEmail("kamal.silva@nova.lk")).thenReturn(Optional.of(existingUser));

        AccountConflictException ex = assertThrows(AccountConflictException.class, () -> authService.register(req));
        assertEquals("An account with this email already exists.", ex.getMessage());
        verify(userRepository, never()).save(any());
        verify(emailService, never()).sendOtpEmail(any(), any(), any());
    }

    // ─── Test 3: Case-Insensitive Duplicate ───────────────────
    @Test
    @DisplayName("Test 3: Case-insensitive email duplicates are rejected")
    void test3_caseInsensitiveDuplicateRejected() {
        RegisterRequest req1 = new RegisterRequest();
        req1.setName("User One");
        req1.setEmail("TestUser@Gmail.Com");
        req1.setPassword("SecurePass123");
        req1.setRole("citizen");

        User existingUser = new User();
        existingUser.setEmail("testuser@gmail.com");
        when(userRepository.findByEmail("testuser@gmail.com")).thenReturn(Optional.of(existingUser));

        AccountConflictException ex = assertThrows(AccountConflictException.class, () -> authService.register(req1));
        assertEquals("An account with this email already exists.", ex.getMessage());

        // Verify normalized email was queried
        verify(userRepository).findByEmail("testuser@gmail.com");
    }

    // ─── Test 4: Correct OTP Within 30 Seconds ────────────────
    @Test
    @DisplayName("Test 4: Correct OTP within 30 seconds -> SUCCESS, account activated and verified, JWT returned")
    void test4_correctOtpWithin30SecondsSuccess() {
        String email = "testuser@gmail.com";
        String rawOtp = "458921";
        String hash = hashOtp(rawOtp);

        User user = new User();
        user.setId("u-001");
        user.setEmail(email);
        user.setRole("citizen");
        user.setVerified(false);
        user.setActive(false);
        user.setOtpHash(hash);
        user.setOtpExpiresAt(Instant.now().plus(25, ChronoUnit.SECONDS)); // Valid within 30s window

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtUtil.generateToken(email, "citizen", "u-001")).thenReturn("mock-jwt-token-12345");
        when(jwtUtil.getExpirationMs()).thenReturn(86400000L);

        VerifyOtpRequest verifyReq = new VerifyOtpRequest(email, rawOtp);
        AuthResponse res = authService.verifyOtp(verifyReq);

        assertNotNull(res);
        assertEquals("mock-jwt-token-12345", res.getAccessToken());
        assertTrue(res.getUser().isVerified());
        assertTrue(user.isVerified());
        assertTrue(user.isActive());
        assertEquals("ACTIVE", user.getStatus());
        assertNull(user.getOtpHash(), "OTP hash must be immediately invalidated");
        assertNull(user.getOtpExpiresAt(), "OTP expiry must be cleared");
        verify(userRepository).save(user);
    }

    // ─── Test 5: Correct OTP After 30 Seconds (Expired) ──────
    @Test
    @DisplayName("Test 5: Correct OTP after 30 seconds -> FAIL: OTP has expired. Please request a new OTP.")
    void test5_correctOtpAfter30SecondsFails() {
        String email = "testuser@gmail.com";
        String rawOtp = "458921";
        String hash = hashOtp(rawOtp);

        User user = new User();
        user.setId("u-001");
        user.setEmail(email);
        user.setVerified(false);
        user.setOtpHash(hash);
        user.setOtpExpiresAt(Instant.now().minus(5, ChronoUnit.SECONDS)); // Expired >30s ago

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        VerifyOtpRequest verifyReq = new VerifyOtpRequest(email, rawOtp);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> authService.verifyOtp(verifyReq));

        assertEquals("OTP has expired. Please request a new OTP.", ex.getMessage());
        assertFalse(user.isVerified());
        assertNull(user.getOtpHash(), "Expired OTP hash must be wiped");
        verify(jwtUtil, never()).generateToken(any(), any(), any());
        verify(userRepository).save(user);
    }

    // ─── Test 6: Wrong OTP ────────────────────────────────────
    @Test
    @DisplayName("Test 6: Wrong OTP -> FAIL: Invalid OTP.")
    void test6_wrongOtpFails() {
        String email = "testuser@gmail.com";
        String correctOtp = "458921";
        String wrongOtp = "999999";
        String hash = hashOtp(correctOtp);

        User user = new User();
        user.setId("u-001");
        user.setEmail(email);
        user.setVerified(false);
        user.setOtpHash(hash);
        user.setOtpExpiresAt(Instant.now().plus(20, ChronoUnit.SECONDS));

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        VerifyOtpRequest verifyReq = new VerifyOtpRequest(email, wrongOtp);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> authService.verifyOtp(verifyReq));

        assertEquals("Invalid OTP.", ex.getMessage());
        assertFalse(user.isVerified());
        verify(jwtUtil, never()).generateToken(any(), any(), any());
    }

    // ─── Test 7: Reuse OTP ────────────────────────────────────
    @Test
    @DisplayName("Test 7: Reuse OTP -> Submitting same OTP again fails")
    void test7_reuseOtpFails() {
        String email = "testuser@gmail.com";
        String rawOtp = "458921";
        String hash = hashOtp(rawOtp);

        User user = new User();
        user.setId("u-001");
        user.setEmail(email);
        user.setRole("citizen");
        user.setVerified(false);
        user.setOtpHash(hash);
        user.setOtpExpiresAt(Instant.now().plus(25, ChronoUnit.SECONDS));

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtUtil.generateToken(any(), any(), any())).thenReturn("mock-jwt-token");

        // 1st submission: SUCCESS
        VerifyOtpRequest verifyReq1 = new VerifyOtpRequest(email, rawOtp);
        AuthResponse res1 = authService.verifyOtp(verifyReq1);
        assertNotNull(res1);
        assertTrue(user.isVerified());
        assertNull(user.getOtpHash());

        // 2nd submission with same OTP: FAILS (already verified)
        VerifyOtpRequest verifyReq2 = new VerifyOtpRequest(email, rawOtp);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> authService.verifyOtp(verifyReq2));
        assertEquals("Account is already verified. Please sign in.", ex.getMessage());
    }

    // ─── Test 8: Resend OTP Invalidation ──────────────────────
    @Test
    @DisplayName("Test 8: Resend OTP -> OTP #1 is invalidated immediately, OTP #2 valid for 30s")
    void test8_resendOtpInvalidatesPreviousOtp() {
        String email = "testuser@gmail.com";
        String otp1 = "111111";
        String hash1 = hashOtp(otp1);

        User user = new User();
        user.setId("u-001");
        user.setName("Test User");
        user.setEmail(email);
        user.setRole("citizen");
        user.setVerified(false);
        user.setOtpHash(hash1);
        user.setOtpExpiresAt(Instant.now().plus(20, ChronoUnit.SECONDS));
        user.setOtpAttempts(1);
        user.setOtpRequestedAt(Instant.now());

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        // User requests Resend OTP
        authService.resendOtp(email);

        // Verify that OTP hash changed and expiry reset to 30s
        assertNotEquals(hash1, user.getOtpHash(), "OTP hash must be replaced with new OTP hash");
        long secondsUntilExpiry = ChronoUnit.SECONDS.between(Instant.now(), user.getOtpExpiresAt());
        assertTrue(secondsUntilExpiry >= 28 && secondsUntilExpiry <= 30, "Expiry must be reset to ~30s");

        // Attempting to verify with old OTP #1 must fail
        VerifyOtpRequest verifyWithOtp1 = new VerifyOtpRequest(email, otp1);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> authService.verifyOtp(verifyWithOtp1));
        assertEquals("Invalid OTP.", ex.getMessage());

        verify(emailService, times(1)).sendOtpEmail(eq(email), anyString(), eq("Test User"));
    }

    // ─── Test 9: Multiple Registration Requests ──────────────
    @Test
    @DisplayName("Test 9: Multiple registration requests -> First creates user, subsequent requests are rejected")
    void test9_multipleRegistrationRequestsRejected() {
        RegisterRequest req = new RegisterRequest();
        req.setName("Kamal Silva");
        req.setEmail("kamal.silva@nova.lk");
        req.setPassword("SecurePass123");
        req.setRole("citizen");

        // 1st request succeeds
        when(userRepository.findByEmail("kamal.silva@nova.lk")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("SecurePass123")).thenReturn("encodedHash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthResponse res1 = authService.register(req);
        assertNotNull(res1);

        // 2nd request arrives for the same email: findByEmail now returns the existing user
        User createdUser = new User();
        createdUser.setEmail("kamal.silva@nova.lk");
        when(userRepository.findByEmail("kamal.silva@nova.lk")).thenReturn(Optional.of(createdUser));

        AccountConflictException ex = assertThrows(AccountConflictException.class, () -> authService.register(req));
        assertEquals("An account with this email already exists.", ex.getMessage());
    }

    // ─── Google OAuth Tests ───────────────────────────────────

    @Test
    @DisplayName("Google OAuth: Existing user logs in -> returns authCode and updates lastActive")
    void testGoogleOAuth_existingUser() {
        User existing = new User();
        existing.setId("user-google-1");
        existing.setEmail("existing@gmail.com");
        existing.setName("Existing User");
        existing.setRole("citizen");
        existing.setActive(true);
        existing.setStatus("ACTIVE");

        when(userRepository.findByEmail("existing@gmail.com")).thenReturn(Optional.of(existing));
        when(userRepository.findById("user-google-1")).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        AuthService.GoogleAuthResult result = authService.processGoogleIdentity("google-sub-123", "existing@gmail.com", "Existing User");

        assertNotNull(result);
        assertFalse(result.isNewUser());
        assertNotNull(result.getAuthCode());

        // Exchange code for JWT
        when(jwtUtil.generateToken("existing@gmail.com", "citizen", "user-google-1")).thenReturn("mock-jwt-token");
        AuthResponse response = authService.exchangeOAuthCode(result.getAuthCode());

        assertNotNull(response);
        assertEquals("mock-jwt-token", response.getAccessToken());
        assertEquals("existing@gmail.com", response.getUser().getEmail());
    }

    @Test
    @DisplayName("Google OAuth: New user initiates -> generates intent ticket -> completes registration")
    void testGoogleOAuth_newUserRegistrationFlow() {
        when(userRepository.findByEmail("newgoogle@gmail.com")).thenReturn(Optional.empty());

        AuthService.GoogleAuthResult result = authService.processGoogleIdentity("google-sub-456", "newgoogle@gmail.com", "New Google User");

        assertNotNull(result);
        assertTrue(result.isNewUser());
        assertNotNull(result.getRegistrationIntent());
        assertEquals("newgoogle@gmail.com", result.getEmail());

        // Complete registration
        GoogleRegisterRequest regReq = new GoogleRegisterRequest();
        regReq.setRegistrationIntent(result.getRegistrationIntent());
        regReq.setRole("citizen");
        regReq.setDistrict("Colombo");
        regReq.setPhone("0771234567");

        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId("new-u-999");
            return u;
        });
        when(jwtUtil.generateToken(eq("newgoogle@gmail.com"), eq("citizen"), any())).thenReturn("mock-new-jwt-token");

        AuthResponse regResponse = authService.completeGoogleRegistration(regReq);

        assertNotNull(regResponse);
        assertEquals("mock-new-jwt-token", regResponse.getAccessToken());
        assertEquals("newgoogle@gmail.com", regResponse.getUser().getEmail());
        assertEquals("citizen", regResponse.getUser().getRole());
        assertTrue(regResponse.getUser().isVerified());
    }

    @Test
    @DisplayName("Google OAuth: Invalid intent token throws IllegalArgumentException")
    void testGoogleOAuth_invalidIntentThrowsException() {
        GoogleRegisterRequest regReq = new GoogleRegisterRequest();
        regReq.setRegistrationIntent("non-existent-intent-token");
        regReq.setRole("citizen");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
            authService.completeGoogleRegistration(regReq)
        );
        assertTrue(ex.getMessage().contains("expired or is invalid"));
    }
}
