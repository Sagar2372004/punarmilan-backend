package com.punarmilan.backend.service.impl;

import com.punarmilan.backend.dto.NotificationDto;
import com.punarmilan.backend.dto.NotificationStatsDto;
import com.punarmilan.backend.entity.Notification;
import com.punarmilan.backend.entity.User;
import com.punarmilan.backend.exception.ResourceNotFoundException;
import com.punarmilan.backend.repository.NotificationRepository;
import com.punarmilan.backend.repository.UserRepository;
import com.punarmilan.backend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
@EnableScheduling
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Override
    public Page<NotificationDto> getNotifications(Pageable pageable) {
        User currentUser = getCurrentUser();
        Page<Notification> notifications = notificationRepository
                .findByUserOrderByCreatedAtDesc(currentUser, pageable);
        
        return notifications.map(this::mapToDto);
    }

    @Override
    public List<NotificationDto> getUnreadNotifications() {
        User currentUser = getCurrentUser();
        List<Notification> notifications = notificationRepository
                .findByUserAndReadFalseOrderByCreatedAtDesc(currentUser);
        
        return notifications.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public void markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        
        User currentUser = getCurrentUser();
        if (!notification.getUser().getId().equals(currentUser.getId())) {
            throw new ResourceNotFoundException("Notification not found");
        }
        
        notification.setRead(true);
        notificationRepository.save(notification);
        log.debug("Notification {} marked as read", notificationId);
    }

    @Override
    public void markAllAsRead() {
        User currentUser = getCurrentUser();
        int updated = notificationRepository.markAllAsRead(currentUser);
        log.info("Marked {} notifications as read for user {}", updated, currentUser.getEmail());
    }

    @Override
    public void markAllAsSeen() {
        User currentUser = getCurrentUser();
        int updated = notificationRepository.markAllAsSeen(currentUser);
        log.info("Marked {} notifications as seen for user {}", updated, currentUser.getEmail());
    }

    @Override
    public NotificationStatsDto getNotificationStats() {
        User currentUser = getCurrentUser();
        
        long unreadCount = notificationRepository.countByUserAndReadFalse(currentUser);
        long unseenCount = notificationRepository.countByUserAndSeenFalse(currentUser);
        
        return NotificationStatsDto.builder()
                .totalUnread(unreadCount)
                .totalUnseen(unseenCount)
                .connectionRequests(countByType(currentUser, Notification.NotificationType.CONNECTION_REQUEST))
                .profileViews(countByType(currentUser, Notification.NotificationType.PROFILE_VIEW))
                .newMatches(countByType(currentUser, Notification.NotificationType.NEW_MATCH))
                .verificationUpdates(countByType(currentUser, Notification.NotificationType.VERIFICATION_APPROVED) +
                                    countByType(currentUser, Notification.NotificationType.VERIFICATION_REJECTED))
                .systemAlerts(countByType(currentUser, Notification.NotificationType.SYSTEM_ALERT))
                .build();
    }

    @Override
    public void createNotification(Long userId, String type, String title, 
                                   String message, Long relatedId, String relatedType) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        Notification.NotificationType notificationType;
        try {
            notificationType = Notification.NotificationType.valueOf(type);
        } catch (IllegalArgumentException e) {
            log.error("Invalid notification type: {}", type);
            return;
        }
        
        // Avoid duplicate notifications for same event
        if (relatedId != null) {
            List<Notification> similar = notificationRepository.findSimilarNotifications(
                    user, notificationType, relatedId);
            if (!similar.isEmpty()) {
                // Update existing notification instead of creating new one
                Notification existing = similar.get(0);
                existing.setTitle(title);
                existing.setMessage(message);
                existing.setRead(false);
                existing.setSeen(false);
                notificationRepository.save(existing);
                log.debug("Updated existing notification for user {}: {}", userId, title);
                return;
            }
        }
        
        Notification notification = Notification.builder()
                .user(user)
                .type(notificationType)
                .title(title)
                .message(message)
                .relatedId(relatedId)
                .relatedType(relatedType)
                .read(false)
                .seen(false)
                .build();
        
        notificationRepository.save(notification);
        log.debug("Notification created for user {}: {}", userId, title);
    }

    @Override
    @Scheduled(cron = "0 0 2 * * *") // Run daily at 2 AM
    public void cleanupOldNotifications() {
        cleanupOldNotifications(30); // Keep notifications for 30 days
    }

    @Override
    public void cleanupOldNotifications(int daysToKeep) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysToKeep);
        
        List<User> users = userRepository.findAll();
        int totalDeleted = 0;
        
        for (User user : users) {
            int deleted = notificationRepository.deleteOldNotifications(user, cutoffDate);
            if (deleted > 0) {
                totalDeleted += deleted;
                log.debug("Deleted {} old notifications for user {}", deleted, user.getEmail());
            }
        }
        
        if (totalDeleted > 0) {
            log.info("Total {} old notifications deleted for all users", totalDeleted);
        }
    }

    @Override
    public void deleteNotification(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        
        User currentUser = getCurrentUser();
        if (!notification.getUser().getId().equals(currentUser.getId())) {
            throw new ResourceNotFoundException("Notification not found");
        }
        
        notificationRepository.delete(notification);
        log.debug("Notification {} deleted by user {}", notificationId, currentUser.getEmail());
    }

    // ==================== PRIVATE HELPER METHODS ====================

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private NotificationDto mapToDto(Notification notification) {
        // Additional data based on notification type
        Object data = null;
        
        // You can add logic here to fetch additional data based on notification type
        // For example, if it's a connection request, fetch connection details
        // if (notification.getType() == Notification.NotificationType.CONNECTION_REQUEST) {
        //     data = fetchConnectionRequestData(notification.getRelatedId());
        // }
        
        return NotificationDto.builder()
                .id(notification.getId())
                .type(notification.getType().name())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .relatedId(notification.getRelatedId())
                .relatedType(notification.getRelatedType())
                .read(notification.isRead())
                .seen(notification.isSeen())
                .createdAt(notification.getCreatedAt())
                .data(data)
                .build();
    }

    private long countByType(User user, Notification.NotificationType type) {
        // Simple implementation - count from all notifications
        List<Notification> allNotifications = notificationRepository
                .findByUserOrderByCreatedAtDesc(user, Pageable.unpaged())
                .getContent();
        
        return allNotifications.stream()
                .filter(n -> n.getType() == type && !n.isRead())
                .count();
    }
    
    // Helper method to count multiple notification types
    private long countByTypes(User user, Notification.NotificationType... types) {
        List<Notification.NotificationType> typeList = Arrays.asList(types);
        List<Notification> allNotifications = notificationRepository
                .findByUserOrderByCreatedAtDesc(user, Pageable.unpaged())
                .getContent();
        
        return allNotifications.stream()
                .filter(n -> typeList.contains(n.getType()) && !n.isRead())
                .count();
    }
}