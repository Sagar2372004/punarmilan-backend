package com.punarmilan.backend.service;

import com.punarmilan.backend.dto.NotificationDto;
import com.punarmilan.backend.dto.NotificationStatsDto;
import com.punarmilan.backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface NotificationService {

    // Core methods
    Page<NotificationDto> getNotifications(Pageable pageable);

    List<NotificationDto> getUnreadNotifications();

    void markAsRead(Long notificationId);

    void markAllAsRead();

    void markAllAsSeen();

    NotificationStatsDto getNotificationStats();

    void createNotification(Long userId, String type, String title,
            String message, Long relatedId, String relatedType);

    void cleanupOldNotifications();

    void cleanupOldNotifications(int daysToKeep);

    void deleteNotification(Long notificationId);

    // Additional query methods
    List<NotificationDto> getRecentNotifications(int days);

    void clearAllNotifications();

    Page<NotificationDto> getNotificationsByType(String type, Pageable pageable);

    // Helper methods for common notification types
    void sendConnectionRequestNotification(User sender, User receiver);

    void sendProfileViewNotification(User viewer, User profileOwner);

    void sendMatchNotification(User user1, User user2, int matchPercentage);

    void sendVerificationNotification(User user, boolean approved, String notes);

    void sendProfileCompletionNotification(User user);

    void sendMessageNotification(User sender, User receiver, String messagePreview);

    // Preference management
    com.punarmilan.backend.dto.NotificationPreferenceDto getPreferences();

    com.punarmilan.backend.dto.NotificationPreferenceDto updatePreferences(
            com.punarmilan.backend.dto.NotificationPreferenceDto dto);
}