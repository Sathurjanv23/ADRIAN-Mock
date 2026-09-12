package com.nova.emergency.controller;

import com.nova.emergency.dto.ApiResponse;
import com.nova.emergency.model.Notification;
import com.nova.emergency.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationRepository notificationRepository;

    public NotificationController(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Notification>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(notificationRepository.findAll()));
    }

    @GetMapping("/unread")
    public ResponseEntity<ApiResponse<List<Notification>>> getUnread() {
        return ResponseEntity.ok(ApiResponse.ok(notificationRepository.findByReadFalseOrderByCreatedAtDesc()));
    }

    @GetMapping("/unread/count")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount() {
        return ResponseEntity.ok(ApiResponse.ok(notificationRepository.countByReadFalse()));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Notification>> markRead(@PathVariable String id) {
        Notification notif = notificationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found: " + id));
        notif.setRead(true);
        return ResponseEntity.ok(ApiResponse.ok(notificationRepository.save(notif), "Marked as read"));
    }

    @PostMapping("/read-all")
    public ResponseEntity<ApiResponse<String>> markAllRead() {
        List<Notification> all = notificationRepository.findByReadFalseOrderByCreatedAtDesc();
        all.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(all);
        return ResponseEntity.ok(ApiResponse.ok("All marked read"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Notification>> create(@RequestBody Notification notification) {
        return ResponseEntity.ok(ApiResponse.ok(notificationRepository.save(notification), "Notification created"));
    }
}
