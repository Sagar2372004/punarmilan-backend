package com.punarmilan.backend.service;

import com.punarmilan.backend.dto.NotificationDto;
import com.punarmilan.backend.dto.NotificationStatsDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface NotificationService {

    // Get notifications for current user
    Page<NotificationDto> getNotifications(Pageable pageable);
    
    // Get unread notifications
    List<NotificationDto> getUnreadNotifications();
    
    // Mark notification as read
    void markAsRead(Long notificationId);
    
    // Mark all notifications as read
    void markAllAsRead();
    
    // Mark all notifications as seen (for bell icon)
    void markAllAsSeen();
    
    // Get notification stats
    NotificationStatsDto getNotificationStats();
    
    // Create notification
    void createNotification(Long userId, String type, String title, 
                           String message, Long relatedId, String relatedType);
    
    // Delete notification
    void deleteNotification(Long notificationId);
    
    // Cleanup old notifications
    void cleanupOldNotifications();
    
    // Cleanup old notifications with custom days
    void cleanupOldNotifications(int daysToKeep);
    
    
    
    // Comment out unimplemented methods for now
    /*
    // Get notifications by type
    Page<NotificationDto> getNotificationsByType(String type, Pageable pageable);
    
    // Get recent notifications
    List<NotificationDto> getRecentNotifications(int days);
    
    // Clear all notifications for current user
    void clearAllNotifications();
    
    // Get notification count by type
    Map<String, Long> getNotificationCountByType();
    
    // Send push notification (for mobile)
    void sendPushNotification(Long userId, String title, String body);
    
    // Mark multiple notifications as read
    void markMultipleAsRead(List<Long> notificationIds);
    */
}