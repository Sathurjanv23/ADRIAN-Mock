package com.nova.emergency.controller;

import com.nova.emergency.dto.ApiResponse;
import com.nova.emergency.exception.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler({ResourceNotFoundException.class, IncidentNotFoundException.class})
    public ResponseEntity<ApiResponse<Void>> handleNotFound(ResourceNotFoundException ex) {
        log.warn("Incident not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("INCIDENT_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(InvalidStatusTransitionException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidStatusTransition(InvalidStatusTransitionException ex) {
        log.warn("Invalid status transition: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error("INVALID_STATUS_TRANSITION", ex.getMessage()));
    }

    @ExceptionHandler(InvalidStatusException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidStatus(InvalidStatusException ex) {
        log.warn("Invalid status: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(ApiResponse.error("INVALID_STATUS", ex.getMessage()));
    }

    @ExceptionHandler(UnauthorizedMissionUpdateException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnauthorizedMissionUpdate(UnauthorizedMissionUpdateException ex) {
        log.warn("Unauthorized mission update: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("UNAUTHORIZED_TEAM", ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalState(IllegalStateException ex) {
        String rawMsg = ex.getMessage() != null ? ex.getMessage() : "Conflict";
        // Extract structured conflict code prefix if present (e.g. "INCIDENT_ALREADY_ASSIGNED: ...")
        String code;
        String message;
        int colonIdx = rawMsg.indexOf(":");
        if (colonIdx > 0 && colonIdx < 50) {
            String prefix = rawMsg.substring(0, colonIdx).trim();
            if (prefix.matches("[A-Z_]+")) {
                code = prefix;
                message = rawMsg.substring(colonIdx + 1).trim();
            } else {
                code = "CONFLICT";
                message = rawMsg;
            }
        } else {
            code = "CONFLICT";
            message = rawMsg;
        }
        log.warn("Conflict / illegal state [{}]: {}", code, message);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(code, message));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(IllegalArgumentException ex) {
        log.warn("Bad request: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(ApiResponse.error("BAD_REQUEST", ex.getMessage()));
    }

    @ExceptionHandler(AccountConflictException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccountConflict(AccountConflictException ex) {
        log.warn("Account conflict: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error("ACCOUNT_CONFLICT", ex.getMessage()));
    }

    @ExceptionHandler({
        org.springframework.dao.DuplicateKeyException.class,
        org.springframework.dao.DataIntegrityViolationException.class,
        com.mongodb.MongoWriteException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleDuplicateKey(Exception ex) {
        log.warn("Database constraint / duplicate key violation: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error("An account with this email already exists."));
    }

    @ExceptionHandler(AccountDeactivatedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccountDeactivated(AccountDeactivatedException ex) {
        log.warn("Account deactivated: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error("Access denied. Insufficient privileges."));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentials(BadCredentialsException ex) {
        log.warn("Authentication bad credentials: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Invalid email or password."));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthError(AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Authentication failed: " + ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest().body(ApiResponse.error(errors));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadableRequest(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body(ApiResponse.error("BAD_REQUEST", "Request body must be valid JSON."));
    }

    @ExceptionHandler({
        org.springframework.web.servlet.resource.NoResourceFoundException.class,
        org.springframework.web.servlet.NoHandlerFoundException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleNotFoundResource(Exception ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneral(Exception ex) {
        String causeInfo = ex.getCause() != null
                ? ex.getCause().getClass().getSimpleName() + ": " + ex.getCause().getMessage()
                : "none";
        log.error("Unhandled error: {} (Caused by: {})", ex.getMessage(), causeInfo, ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("INTERNAL_ERROR", "An internal error occurred. Please try again."));
    }

    // ─── ADRN Relief Logistics Exceptions ────────────────────────────

    @ExceptionHandler(com.nova.emergency.exception.InsufficientInventoryException.class)
    public ResponseEntity<ApiResponse<Void>> handleInsufficientInventory(
            com.nova.emergency.exception.InsufficientInventoryException ex) {
        log.warn("Insufficient inventory: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error("INSUFFICIENT_INVENTORY", ex.getMessage()));
    }

    @ExceptionHandler(com.nova.emergency.exception.ReliefMissionConflictException.class)
    public ResponseEntity<ApiResponse<Void>> handleReliefMissionConflict(
            com.nova.emergency.exception.ReliefMissionConflictException ex) {
        log.warn("Relief mission conflict: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error("RELIEF_MISSION_CONFLICT", ex.getMessage()));
    }
}
