package com.punarmilan.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_view_history",
       indexes = {
           @Index(name = "idx_viewer_viewed", columnList = "viewer_id, viewed_user_id"),
           @Index(name = "idx_viewed_at", columnList = "viewed_at")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserViewHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "viewer_id", nullable = false)
    private User viewer;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "viewed_user_id", nullable = false)
    private User viewedUser;
    
    @Column(name = "viewed_at", nullable = false)
    private LocalDateTime viewedAt;
    
    @Column(name = "view_duration_seconds")
    private Integer viewDurationSeconds;
    
    @Column(name = "view_source", length = 50)
    private String viewSource; // "search", "suggestions", "profile", "matches"
    
    @Column(name = "device_type", length = 50)
    private String deviceType;
    
    @Column(name = "ip_address", length = 50)
    private String ipAddress;
    
    @Column(name = "is_profile_viewed", nullable = false)
    @Builder.Default
    private boolean profileViewed = false;
    
    @Column(name = "photos_viewed_count")
    @Builder.Default
    private Integer photosViewedCount = 0;
    
    @PrePersist
    protected void onCreate() {
        viewedAt = LocalDateTime.now();
    }
}