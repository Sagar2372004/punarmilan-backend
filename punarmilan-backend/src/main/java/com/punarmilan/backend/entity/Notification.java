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
    private Long relatedId;

    @Column(name = "related_type")
    private String relatedType;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private boolean read = false;

    @Column(name = "is_seen", nullable = false)
    @Builder.Default
    private boolean seen = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public enum NotificationType {
        CONNECTION_REQUEST,
        CONNECTION_ACCEPTED,
        CONNECTION_REJECTED,
        PROFILE_VIEW,
        NEW_MATCH,
        VERIFICATION_APPROVED,
        VERIFICATION_REJECTED,
        MESSAGE_RECEIVED,
        SYSTEM_ALERT,
        PROFILE_COMPLETION
    }
}