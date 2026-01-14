package com.punarmilan.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(length = 1000)
    private String message;

    @Column(name = "related_id")
    private Long relatedId; // e.g., connection request ID, profile ID

    @Column(name = "related_type")
    private String relatedType; // CONNECTION_REQUEST, PROFILE_VIEW, etc.

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private boolean read = false;

    @Column(name = "is_seen", nullable = false)
    @Builder.Default
    private boolean seen = false; // For notification bell indicator

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum NotificationType {
        CONNECTION_REQUEST,      // Someone sent you a connection request
        CONNECTION_ACCEPTED,     // Your connection request was accepted
        CONNECTION_REJECTED,     // Your connection request was rejected
        PROFILE_VIEW,            // Someone viewed your profile
        NEW_MATCH,               // New match found for you
        VERIFICATION_APPROVED,   // Your profile verification approved
        VERIFICATION_REJECTED,   // Your profile verification rejected
        MESSAGE_RECEIVED,        // New message received (if chat implemented)
        SYSTEM_ALERT,            // System notification
        PROFILE_COMPLETION       // Reminder to complete profile
    }
}