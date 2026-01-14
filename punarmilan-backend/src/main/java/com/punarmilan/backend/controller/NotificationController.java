package com.punarmilan.backend.controller;

import com.punarmilan.backend.dto.NotificationDto;
import com.punarmilan.backend.dto.NotificationStatsDto;
import com.punarmilan.backend.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notification Management", description = "Manage user notifications")
@PreAuthorize("hasRole('USER')")
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "Get all notifications")
    @GetMapping
    public ResponseEntity<Page<NotificationDto>> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<NotificationDto> notifications = notificationService.getNotifications(pageable);
        return ResponseEntity.ok(notifications);
    }

    @Operation(summary = "Get unread notifications")
    @GetMapping("/unread")
    public ResponseEntity<List<NotificationDto>> getUnreadNotifications() {
        List<NotificationDto> notifications = notificationService.getUnreadNotifications();
        return ResponseEntity.ok(notifications);
    }

    @Operation(summary = "Get notification statistics")
    @GetMapping("/stats")
    public ResponseEntity<NotificationStatsDto> getNotificationStats() {
        NotificationStatsDto stats = notificationService.getNotificationStats();
        return ResponseEntity.ok(stats);
    }

    @Operation(summary = "Mark notification as read")
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long notificationId) {
        notificationService.markAsRead(notificationId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Mark all notifications as read")
    @PatchMapping("/mark-all-read")
    public ResponseEntity<Void> markAllAsRead() {
        notificationService.markAllAsRead();
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Mark all notifications as seen")
    @PatchMapping("/mark-all-seen")
    public ResponseEntity<Void> markAllAsSeen() {
        notificationService.markAllAsSeen();
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Delete notification")
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<Void> deleteNotification(@PathVariable Long notificationId) {
        notificationService.deleteNotification(notificationId);
        return ResponseEntity.noContent().build();
    }

    // Comment out unimplemented methods for now
    /*
    @Operation(summary = "Get notifications by type")
    @GetMapping("/type/{type}")
    public ResponseEntity<Page<NotificationDto>> getNotificationsByType(
            @PathVariable String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<NotificationDto> notifications = notificationService.getNotificationsByType(type, pageable);
        return ResponseEntity.ok(notifications);
    }

    @Operation(summary = "Get recent notifications (last 7 days)")
    @GetMapping("/recent")
    public ResponseEntity<List<NotificationDto>> getRecentNotifications() {
        List<NotificationDto> notifications = notificationService.getRecentNotifications(7);
        return ResponseEntity.ok(notifications);
    }

    @Operation(summary = "Clear all notifications")
    @DeleteMapping("/clear-all")
    public ResponseEntity<Void> clearAllNotifications() {
        notificationService.clearAllNotifications();
        return ResponseEntity.noContent().build();
    }
    */
}