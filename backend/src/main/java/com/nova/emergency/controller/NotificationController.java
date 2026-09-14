package com.nova.emergency.controller;

import com.nova.emergency.dto.ApiResponse;
import com.nova.emergency.model.Notification;
import com.nova.emergency.model.User;
import com.nova.emergency.repository.NotificationRepository;
import com.nova.emergency.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * NotificationController — Serves only the authenticated user's own notifications.
 *
 * Security rules enforced here:
 *  1. All endpoints require authentication (JWT).
 *  2. GET /notifications returns only notifications where userId == principal.id
 *     OR (legacy) targetRole contains the principal's role.
 *  3. PATCH /{id}/read verifies the notification belongs to the caller before updating.
 *  4. POST /read-all only marks the caller's own unread notifications as read.
 *  5. No endpoint exposes another user's notifications.
 */
@RestController
@RequestMapping("/api/notifications")
@PreAuthorize("isAuthenticated()")
public class NotificationController {

    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(NotificationController.class);

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationController(NotificationRepository notificationRepository,
                                   UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    // ─── Resolve authenticated user from JWT principal ───────────────────

    private User resolveUser(Principal principal) {
        if (principal == null) {
            throw new IllegalStateException("Authentication required.");
        }
        return userRepository.findByEmail(principal.getName().toLowerCase().trim())
                .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found."));
    }

    // ─── GET /api/notifications — user's own notifications ───────────────

    /**
     * Returns all notifications belonging to the authenticated user.
     * Merges user-specific (userId match) with role-broadcast (targetRole match) notifications.
     * Sorted newest-first; deduplication by id prevents double entries.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<Notification>>> getAll(Principal principal) {
        User user = resolveUser(principal);
        String userId = user.getId();
        String role   = user.getRole();

        // User-specific notifications
        List<Notification> specific = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);

        // Role-broadcast notifications (no userId set — i.e. sent to all users of a role)
        List<Notification> broadcast = notificationRepository
                .findByTargetRoleContainingOrderByCreatedAtDesc(role)
                .stream()
                .filter(n -> n.getUserId() == null || n.getUserId().isBlank())
                .toList();

        // Merge deduplicated, newest first
        List<Notification> merged = Stream.concat(specific.stream(), broadcast.stream())
                .distinct()
                .sorted((a, b) -> {
                    String ta = a.getCreatedAt() != null ? a.getCreatedAt() : "";
                    String tb = b.getCreatedAt() != null ? b.getCreatedAt() : "";
                    return tb.compareTo(ta);
                })
                .toList();

        return ResponseEntity.ok(ApiResponse.ok(merged));
    }

    // ─── GET /api/notifications/unread — user's unread notifications ──────

    @GetMapping("/unread")
    public ResponseEntity<ApiResponse<List<Notification>>> getUnread(Principal principal) {
        User user = resolveUser(principal);
        String userId = user.getId();
        String role   = user.getRole();

        List<Notification> specific = notificationRepository
                .findByUserIdAndReadFalseOrderByCreatedAtDesc(userId);

        List<Notification> broadcast = notificationRepository
                .findByTargetRoleContainingAndReadFalseOrderByCreatedAtDesc(role)
                .stream()
                .filter(n -> n.getUserId() == null || n.getUserId().isBlank())
                .toList();

        List<Notification> merged = Stream.concat(specific.stream(), broadcast.stream())
                .distinct()
                .sorted((a, b) -> {
                    String ta = a.getCreatedAt() != null ? a.getCreatedAt() : "";
                    String tb = b.getCreatedAt() != null ? b.getCreatedAt() : "";
                    return tb.compareTo(ta);
                })
                .toList();

        return ResponseEntity.ok(ApiResponse.ok(merged));
    }

    // ─── GET /api/notifications/unread/count ─────────────────────────────

    @GetMapping("/unread/count")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(Principal principal) {
        User user = resolveUser(principal);
        String userId = user.getId();
        String role   = user.getRole();

        long specificCount   = notificationRepository.countByUserIdAndReadFalse(userId);
        long broadcastCount  = notificationRepository
                .findByTargetRoleContainingAndReadFalseOrderByCreatedAtDesc(role)
                .stream()
                .filter(n -> n.getUserId() == null || n.getUserId().isBlank())
                .count();

        return ResponseEntity.ok(ApiResponse.ok(specificCount + broadcastCount));
    }

    // ─── PATCH /api/notifications/{id}/read — mark one as read ───────────

    /**
     * Marks a single notification as read.
     * Security: verifies the notification belongs to the calling user before updating.
     * A user cannot mark another user's notification as read.
     */
    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Notification>> markRead(
            @PathVariable String id,
            Principal principal) {

        User user = resolveUser(principal);

        Notification notif = notificationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found: " + id));

        // Security check: the notification must belong to this user (userId match)
        // OR be a role-broadcast notification where userId is blank AND role matches
        boolean isOwner = user.getId().equals(notif.getUserId());
        boolean isBroadcast = (notif.getUserId() == null || notif.getUserId().isBlank())
                && notif.getTargetRole() != null
                && notif.getTargetRole().contains(user.getRole());

        if (!isOwner && !isBroadcast) {
            log.warn("Security violation: user {} attempted to mark notification {} (owner: {}) as read",
                    user.getId(), id, notif.getUserId());
            return ResponseEntity.status(403)
                    .body(ApiResponse.error("FORBIDDEN", "You do not have permission to modify this notification."));
        }

        if (!notif.isRead()) {
            notif.setRead(true);
            notificationRepository.save(notif);
        }

        return ResponseEntity.ok(ApiResponse.ok(notif, "Marked as read"));
    }

    // ─── POST /api/notifications/read-all — mark all user's notifications read ──

    /**
     * Marks all of the calling user's unread notifications as read.
     * Only affects notifications owned by (or broadcast to) the authenticated user.
     */
    @PostMapping("/read-all")
    public ResponseEntity<ApiResponse<Map<String, Long>>> markAllRead(Principal principal) {
        User user = resolveUser(principal);
        String userId = user.getId();
        String role   = user.getRole();

        // User-specific unread
        List<Notification> specificUnread =
                notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId);

        // Broadcast unread for user's role
        List<Notification> broadcastUnread = notificationRepository
                .findByTargetRoleContainingAndReadFalseOrderByCreatedAtDesc(role)
                .stream()
                .filter(n -> n.getUserId() == null || n.getUserId().isBlank())
                .toList();

        long count = specificUnread.size() + broadcastUnread.size();

        specificUnread.forEach(n -> n.setRead(true));
        broadcastUnread.forEach(n -> n.setRead(true));

        notificationRepository.saveAll(specificUnread);
        notificationRepository.saveAll(broadcastUnread);

        log.info("User {} marked {} notifications as read", userId, count);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("marked", count), count + " notification(s) marked as read"));
    }

    // ─── POST /api/notifications — internal notification creation ─────────

    /**
     * Creates a notification. This endpoint is for internal/admin use.
     * In practice, notifications are created by IncidentService, not by clients directly.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Notification>> create(
            @RequestBody Notification notification,
            Principal principal) {
        // Only allow admin or internal system to create notifications via API
        User user = resolveUser(principal);
        if (!"admin".equals(user.getRole()) && !"officer".equals(user.getRole())) {
            return ResponseEntity.status(403)
                    .body(ApiResponse.error("FORBIDDEN", "Only administrators can create notifications via API."));
        }
        Notification saved = notificationRepository.save(notification);
        return ResponseEntity.ok(ApiResponse.ok(saved, "Notification created"));
    }
}
