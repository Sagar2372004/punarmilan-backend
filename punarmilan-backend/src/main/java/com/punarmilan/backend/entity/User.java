package com.punarmilan.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_user_active_verified", columnList = "is_active, is_verified"),
        @Index(name = "idx_user_email", columnList = "email"),
        @Index(name = "idx_user_profile_id", columnList = "profile_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    public static final String PROFILE_ID_PREFIX = "PM";
    public static final long PROFILE_ID_OFFSET = 28249810L;

    public static String generateProfileId(Long id) {
        if (id == null)
            return null;
        return PROFILE_ID_PREFIX + (id + PROFILE_ID_OFFSET);
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 15)
    private String mobileNumber;

    @Column(name = "profile_id", unique = true)
    private String profileId;

    @Column(nullable = false)
    @Builder.Default
    private String role = "USER";

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "is_verified", nullable = false)
    @Builder.Default
    private boolean verified = false;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Column(name = "is_premium")
    @Builder.Default
    private Boolean premium = false;

    @Column(name = "premium_since")
    private LocalDateTime premiumSince;

    @Column(name = "is_hidden", nullable = false)
    @Builder.Default
    private boolean hidden = false;

    @Column(name = "hidden_until")
    private LocalDateTime hiddenUntil;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}