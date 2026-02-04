package com.punarmilan.backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "notification_preferences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "email_alerts")
    @Builder.Default
    private boolean emailAlerts = true;

    @Column(name = "web_notifications")
    @Builder.Default
    private boolean webNotifications = true;

    @Column(name = "match_mail")
    @Builder.Default
    private boolean matchMail = true;

    @Column(name = "visitor_alerts")
    @Builder.Default
    private boolean visitorAlerts = true;

    @Column(name = "message_alerts")
    @Builder.Default
    private boolean messageAlerts = true;

    @Column(name = "shortlist_alerts")
    @Builder.Default
    private boolean shortlistAlerts = true;
}
