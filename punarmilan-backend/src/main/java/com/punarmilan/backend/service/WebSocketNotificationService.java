package com.punarmilan.backend.service;

import com.punarmilan.backend.dto.NotificationDto;
import com.punarmilan.backend.dto.PaymentResponseDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    public void sendNotificationToUser(Long userId, NotificationDto notification) {
        String destination = "/user/" + userId + "/queue/notifications";
        messagingTemplate.convertAndSend(destination, notification);
        log.info("Real-time notification sent to user {}: {}", userId, notification.getTitle());
    }

    public void sendPaymentNotification(Long userId, PaymentResponseDto payment) {
        NotificationDto notification = NotificationDto.builder()
                .type("PAYMENT_SUCCESS")
                .title("Payment Successful")
                .message("Your payment of ₹" + payment.getAmount() + " was successful")
                .relatedId(payment.getSubscriptionId())
                .relatedType("PAYMENT")
                .build();
        
        sendNotificationToUser(userId, notification);
    }

    public void sendSubscriptionNotification(Long userId, String title, String message, Long subscriptionId) {
        NotificationDto notification = NotificationDto.builder()
                .type("SUBSCRIPTION_UPDATE")
                .title(title)
                .message(message)
                .relatedId(subscriptionId)
                .relatedType("SUBSCRIPTION")
                .build();
        
        sendNotificationToUser(userId, notification);
    }
}