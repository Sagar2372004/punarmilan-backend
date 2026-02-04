package com.punarmilan.backend.event;

import com.punarmilan.backend.entity.NotificationPreference;
import com.punarmilan.backend.entity.User;
import com.punarmilan.backend.repository.NotificationPreferenceRepository;
import com.punarmilan.backend.service.EmailService;
import com.punarmilan.backend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {

    private final EmailService emailService;
    private final NotificationService notificationService;
    private final NotificationPreferenceRepository preferenceRepository;

    @Async
    @EventListener
    public void handleNotificationEvent(NotificationEvent event) {
        User recipient = event.getRecipient();
        String type = event.getType();

        NotificationPreference prefs = preferenceRepository.findByUser(recipient)
                .orElse(getDefaultPreferences(recipient));

        // 1. Web Notification (In-app)
        if (prefs.isWebNotifications()) {
            notificationService.createNotification(
                    recipient.getId(),
                    type,
                    "New Notification",
                    event.getMessage(),
                    null, // relatedId
                    "SYSTEM");
        }

        // 2. Email Delivery
        if (shouldSendEmail(type, prefs)) {
            sendEmail(event);
        }
    }

    private boolean shouldSendEmail(String type, NotificationPreference prefs) {
        if (!prefs.isEmailAlerts())
            return false;

        return switch (type) {
            case "MATCH_FOUND" -> prefs.isMatchMail();
            case "PROFILE_VIEW" -> prefs.isVisitorAlerts();
            case "SHORTLISTED" -> prefs.isShortlistAlerts();
            case "MESSAGE_RECEIVED" -> prefs.isMessageAlerts();
            default -> false;
        };
    }

    private void sendEmail(NotificationEvent event) {
        try {
            switch (event.getType()) {
                case "SHORTLISTED" -> emailService.sendShortlistEmail(event.getSender(), event.getRecipient());
                case "MESSAGE_RECEIVED" -> {
                    if (event.getRelatedObject() instanceof com.punarmilan.backend.entity.Message msg) {
                        emailService.sendMessageNotificationEmail(event.getSender(), event.getRecipient(), msg);
                    }
                }
                // Other real-time emails...
            }
        } catch (Exception e) {
            log.error("Error sending notification email", e);
        }
    }

    private NotificationPreference getDefaultPreferences(User user) {
        return NotificationPreference.builder()
                .user(user)
                .emailAlerts(true)
                .matchMail(true)
                .visitorAlerts(true)
                .shortlistAlerts(true)
                .messageAlerts(true)
                .webNotifications(true)
                .build();
    }
}
