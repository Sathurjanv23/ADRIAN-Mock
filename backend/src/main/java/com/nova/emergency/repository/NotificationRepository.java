package com.nova.emergency.repository;

import com.nova.emergency.model.Notification;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface NotificationRepository extends MongoRepository<Notification, String> {

    // ─── User-specific queries (primary — uses userId) ────────────────────

    /** All notifications for a specific user, newest first. */
    List<Notification> findByUserIdOrderByCreatedAtDesc(String userId);

    /** Unread notifications for a specific user, newest first. */
    List<Notification> findByUserIdAndReadFalseOrderByCreatedAtDesc(String userId);

    /** Count of unread notifications for a specific user. */
    long countByUserIdAndReadFalse(String userId);

    // ─── Role-based queries (fallback — for legacy/broadcast notifications) ──

    /** Notifications targeting a specific role (broadcast), newest first. */
    List<Notification> findByTargetRoleContainingOrderByCreatedAtDesc(String role);

    /** Unread notifications for a role (broadcast), newest first. */
    List<Notification> findByTargetRoleContainingAndReadFalseOrderByCreatedAtDesc(String role);

    /** Count of unread notifications for a role (broadcast). */
    long countByTargetRoleContainingAndReadFalse(String role);

    // ─── Legacy — kept for existing code compatibility ────────────────────

    /** @deprecated Use findByUserIdAndReadFalseOrderByCreatedAtDesc for user-specific queries. */
    @Deprecated
    List<Notification> findByReadFalseOrderByCreatedAtDesc();

    /** @deprecated Use countByUserIdAndReadFalse for user-specific queries. */
    @Deprecated
    long countByReadFalse();
}
