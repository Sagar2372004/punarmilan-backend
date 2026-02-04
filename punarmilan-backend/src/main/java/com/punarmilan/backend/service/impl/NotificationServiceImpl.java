package com.punarmilan.backend.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.punarmilan.backend.dto.NotificationDto;
import com.punarmilan.backend.dto.NotificationStatsDto;
import com.punarmilan.backend.entity.Notification;
import com.punarmilan.backend.entity.User;
import com.punarmilan.backend.exception.ResourceNotFoundException;
import com.punarmilan.backend.repository.*;
import com.punarmilan.backend.service.NotificationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service

@RequiredArgsConstructor
@Transactional
@EnableScheduling
public class NotificationServiceImpl implements NotificationService {

    private final ConversationRepository conversationRepository;

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final MessageRepository messageRepository;

    private boolean hasRun = false;

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
        User currentUser = getCurrentUser();

        // Find notification and verify ownership
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + notificationId));

        // Check if notification belongs to current user
        if (!notification.getUser().getId().equals(currentUser.getId())) {
            throw new ResourceNotFoundException("You don't have permission to access this notification");
        }

        notification.setRead(true);
        notificationRepository.save(notification);
        log.info("Notification {} marked as read by user {}", notificationId, currentUser.getEmail());
    }

    @Override
    public void markAllAsRead() {
        User currentUser = getCurrentUser();
        int updated = notificationRepository.markAllAsRead(currentUser);
        log.info("Marked {} notifications as read for user: {}", updated, currentUser.getEmail());
    }

    @Override
    public void markAllAsSeen() {
        User currentUser = getCurrentUser();
        int updated = notificationRepository.markAllAsSeen(currentUser);
        log.info("Marked {} notifications as seen for user: {}", updated, currentUser.getEmail());
    }

    @Override
    public NotificationStatsDto getNotificationStats() {
        User currentUser = getCurrentUser();

        long totalUnread = notificationRepository.countByUserAndReadFalse(currentUser);
        long totalUnseen = notificationRepository.countByUserAndSeenFalse(currentUser);

        long connectionRequests = notificationRepository.countByUserAndTypeAndReadFalse(
                currentUser, Notification.NotificationType.CONNECTION_REQUEST);

        long profileViews = notificationRepository.countByUserAndTypeAndReadFalse(
                currentUser, Notification.NotificationType.PROFILE_VIEW);

        long newMatches = notificationRepository.countByUserAndTypeAndReadFalse(
                currentUser, Notification.NotificationType.NEW_MATCH);

        // Multi-type count (simplified here but could be multiple calls or a custom
        // query)
        long verificationUpdates = notificationRepository.countByUserAndTypeAndReadFalse(
                currentUser, Notification.NotificationType.VERIFICATION_APPROVED) +
                notificationRepository.countByUserAndTypeAndReadFalse(
                        currentUser, Notification.NotificationType.VERIFICATION_REJECTED);

        long systemAlerts = notificationRepository.countByUserAndTypeAndReadFalse(
                currentUser, Notification.NotificationType.SYSTEM_ALERT);

        return NotificationStatsDto.builder()
                .totalUnread(totalUnread)
                .totalUnseen(totalUnseen)
                .connectionRequests(connectionRequests)
                .profileViews(profileViews)
                .newMatches(newMatches)
                .verificationUpdates(verificationUpdates)
                .systemAlerts(systemAlerts)
                .build();
    }

    @Override
    public void createNotification(Long userId, String type, String title,
            String message, Long relatedId, String relatedType) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Notification.NotificationType notificationType;
        try {
            notificationType = Notification.NotificationType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.error("Invalid notification type: {}", type);
            throw new IllegalArgumentException("Invalid notification type: " + type);
        }

        // Check for duplicate notifications (same type and relatedId within last 5
        // minutes)
        if (relatedId != null) {
            LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);
            List<Notification> similarRecentNotifications = notificationRepository
                    .findSimilarNotifications(user, notificationType, relatedId)
                    .stream()
                    .filter(n -> n.getCreatedAt().isAfter(fiveMinutesAgo))
                    .collect(Collectors.toList());

            if (!similarRecentNotifications.isEmpty()) {
                // Update the most recent similar notification instead of creating new one
                Notification existing = similarRecentNotifications.get(0);
                existing.setTitle(title);
                existing.setMessage(message);
                existing.setRead(false);
                existing.setSeen(false);
                existing.setCreatedAt(LocalDateTime.now());
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
                .createdAt(LocalDateTime.now())
                .build();

        notificationRepository.save(notification);
        log.info("Notification created for user {} ({}): {}",
                user.getEmail(), notificationType, title);
    }

    @Override
    @Scheduled(cron = "0 0 2 * * *") // Run daily at 2 AM
    public void cleanupOldNotifications() {
        cleanupOldNotifications(30); // Keep notifications for 30 days by default
    }

    // TESTING ONLY: One-time deletion today at specific time
    // Change cron to your current time + 2 minutes for testing
    // format: "0 minute hour * * *"
    // Example for 4:05 PM: "0 5 16 * * *"
    @Scheduled(cron = "0 22 14 * * *")
    public void testOneTimeDeletion() {
        if (!hasRun) {
            notificationRepository.deleteAll();
            messageRepository.deleteAll();
            conversationRepository.deleteAll();
            log.info("Notifications & Messages Deleted");
            System.out.println("Notifications & Messages Deleted");
            hasRun = true;
        }
    }

    @Override
    public void cleanupOldNotifications(int daysToKeep) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysToKeep);

        List<User> users = userRepository.findAll();
        int totalDeleted = 0;

        for (User user : users) {
            int deleted = notificationRepository.deleteOldNotifications(user, cutoffDate);
            totalDeleted += deleted;
            if (deleted > 0) {
                log.debug("Deleted {} old notifications for user: {}", deleted, user.getEmail());
            }
        }

        if (totalDeleted > 0) {
            log.info("Total {} old notifications deleted for all users", totalDeleted);
        }
    }

    @Override
    public void deleteNotification(Long notificationId) {
        User currentUser = getCurrentUser();

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + notificationId));

        // Check if notification belongs to current user
        if (!notification.getUser().getId().equals(currentUser.getId())) {
            throw new ResourceNotFoundException("You don't have permission to delete this notification");
        }

        notificationRepository.delete(notification);
        log.info("Notification {} deleted by user {}", notificationId, currentUser.getEmail());
    }

    // ==================== ADDITIONAL HELPER METHODS ====================

    /**
     * Send connection request notification
     */
    public void sendConnectionRequestNotification(User sender, User receiver) {
        String title = "New Connection Request";
        String message = sender.getEmail() + " wants to connect with you";

        createNotification(
                receiver.getId(),
                Notification.NotificationType.CONNECTION_REQUEST.name(),
                title,
                message,
                sender.getId(),
                "USER");
    }

    /**
     * Send profile view notification
     */
    public void sendProfileViewNotification(User viewer, User profileOwner) {
        String title = "Profile Viewed";
        String message = viewer.getEmail() + " viewed your profile";

        createNotification(
                profileOwner.getId(),
                Notification.NotificationType.PROFILE_VIEW.name(),
                title,
                message,
                viewer.getId(),
                "USER");
    }

    /**
     * Send match notification
     */
    public void sendMatchNotification(User user1, User user2, int matchPercentage) {
        String title = "New Match Found!";
        String message = "You have a " + matchPercentage + "% match with " + user2.getEmail();

        createNotification(
                user1.getId(),
                Notification.NotificationType.NEW_MATCH.name(),
                title,
                message,
                user2.getId(),
                "USER");
    }

    /**
     * Send verification status notification
     */
    public void sendVerificationNotification(User user, boolean approved, String notes) {
        String type = approved ? Notification.NotificationType.VERIFICATION_APPROVED.name()
                : Notification.NotificationType.VERIFICATION_REJECTED.name();
        String title = approved ? "Profile Verified!" : "Verification Rejected";
        String message = approved ? "Your profile has been verified successfully"
                : "Your profile verification was rejected: " + notes;

        createNotification(
                user.getId(),
                type,
                title,
                message,
                user.getId(),
                "PROFILE");
    }

    /**
     * Send profile completion notification
     */
    public void sendProfileCompletionNotification(User user) {
        String title = "Profile Completed!";
        String message = "Your profile is now complete and visible to others";

        createNotification(
                user.getId(),
                Notification.NotificationType.PROFILE_COMPLETION.name(),
                title,
                message,
                user.getId(),
                "PROFILE");
    }

    /**
     * Send message received notification
     */
    public void sendMessageNotification(User sender, User receiver, String messagePreview) {
        String title = "New Message from " + sender.getEmail();
        String message = messagePreview.length() > 100 ? messagePreview.substring(0, 100) + "..." : messagePreview;

        createNotification(
                receiver.getId(),
                Notification.NotificationType.MESSAGE_RECEIVED.name(),
                title,
                message,
                sender.getId(),
                "MESSAGE");
    }

    // ==================== PRIVATE HELPER METHODS ====================

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    private NotificationDto mapToDto(Notification notification) {
        // Fetch additional data based on notification type
        Object data = fetchAdditionalData(notification);

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

    private Object fetchAdditionalData(Notification notification) {
        // Implement based on your requirements
        // For example, fetch user profile for CONNECTION_REQUEST
        // or fetch message content for MESSAGE_RECEIVED

        // This is a placeholder - implement as needed
        return null;
    }

    /**
     * Get recent notifications (last N days)
     */
    public List<NotificationDto> getRecentNotifications(int days) {
        User currentUser = getCurrentUser();
        LocalDateTime since = LocalDateTime.now().minusDays(days);

        List<Notification> notifications = notificationRepository.findByUser(currentUser)
                .stream()
                .filter(n -> n.getCreatedAt().isAfter(since))
                .sorted((n1, n2) -> n2.getCreatedAt().compareTo(n1.getCreatedAt()))
                .collect(Collectors.toList());

        return notifications.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Clear all notifications for current user
     */
    public void clearAllNotifications() {
        User currentUser = getCurrentUser();
        List<Notification> notifications = notificationRepository.findByUser(currentUser);

        if (!notifications.isEmpty()) {
            notificationRepository.deleteAll(notifications);
            log.info("Cleared all {} notifications for user: {}",
                    notifications.size(), currentUser.getEmail());
        }
    }

    /**
     * Get notifications by type
     */
    public Page<NotificationDto> getNotificationsByType(String type, Pageable pageable) {
        User currentUser = getCurrentUser();

        try {
            Notification.NotificationType notificationType = Notification.NotificationType.valueOf(type.toUpperCase());

            Page<Notification> notifications = (Page<Notification>) notificationRepository
                    .findByUserOrderByCreatedAtDesc(currentUser, pageable)
                    .filter(n -> n.getType() == notificationType);

            return notifications.map(this::mapToDto);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid notification type: " + type);
        }
    }

    @Override
    public com.punarmilan.backend.dto.NotificationPreferenceDto getPreferences() {
        User user = getCurrentUser();
        com.punarmilan.backend.entity.NotificationPreference prefs = preferenceRepository.findByUser(user)
                .orElseGet(() -> createDefaultPreferences(user));

        return mapToPreferenceDto(prefs);
    }

    @Override
    public com.punarmilan.backend.dto.NotificationPreferenceDto updatePreferences(
            com.punarmilan.backend.dto.NotificationPreferenceDto dto) {
        User user = getCurrentUser();
        com.punarmilan.backend.entity.NotificationPreference prefs = preferenceRepository.findByUser(user)
                .orElseGet(() -> createDefaultPreferences(user));

        prefs.setEmailAlerts(dto.isEmailAlerts());
        prefs.setEmailAlerts(dto.isEmailAlerts());
        prefs.setWebNotifications(dto.isWebNotifications());
        prefs.setMatchMail(dto.isMatchMail());
        prefs.setVisitorAlerts(dto.isVisitorAlerts());
        prefs.setMessageAlerts(dto.isMessageAlerts());
        prefs.setShortlistAlerts(dto.isShortlistAlerts());

        com.punarmilan.backend.entity.NotificationPreference saved = preferenceRepository.save(prefs);
        return mapToPreferenceDto(saved);
    }

    private com.punarmilan.backend.entity.NotificationPreference createDefaultPreferences(User user) {
        com.punarmilan.backend.entity.NotificationPreference prefs = com.punarmilan.backend.entity.NotificationPreference
                .builder()
                .user(user)
                .emailAlerts(true)
                .emailAlerts(false)
                .webNotifications(true)
                .matchMail(true)
                .visitorAlerts(true)
                .messageAlerts(true)
                .shortlistAlerts(true)
                .build();
        return preferenceRepository.save(prefs);
    }

    private com.punarmilan.backend.dto.NotificationPreferenceDto mapToPreferenceDto(
            com.punarmilan.backend.entity.NotificationPreference prefs) {
        return com.punarmilan.backend.dto.NotificationPreferenceDto.builder()
                .emailAlerts(prefs.isEmailAlerts())
                .emailAlerts(prefs.isEmailAlerts())
                .webNotifications(prefs.isWebNotifications())
                .matchMail(prefs.isMatchMail())
                .visitorAlerts(prefs.isVisitorAlerts())
                .messageAlerts(prefs.isMessageAlerts())
                .shortlistAlerts(prefs.isShortlistAlerts())
                .build();
    }

}