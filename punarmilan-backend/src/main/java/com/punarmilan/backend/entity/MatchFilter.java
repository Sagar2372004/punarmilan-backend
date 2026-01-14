package com.punarmilan.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "match_filters")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchFilter {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    // Age range
    @Column(name = "min_age")
    private Integer minAge;
    
    @Column(name = "max_age")
    private Integer maxAge;
    
    // Location
    @Column(name = "city", length = 100)
    private String city;
    
    @Column(name = "max_distance_km")
    private Integer maxDistanceKm;
    
    // Education & Profession
    @Column(name = "education_level", length = 100)
    private String educationLevel;
    
    @Column(name = "occupation", length = 200)
    private String occupation;
    
    // Height (if applicable)
    @Column(name = "min_height_cm")
    private Integer minHeightCm;
    
    @Column(name = "max_height_cm")
    private Integer maxHeightCm;
    
    // Marital Status
    @Column(name = "marital_status", length = 50)
    private String maritalStatus;
    
    // Preferences
    @Column(name = "preferred_gender", length = 20)
    private String preferredGender;
    
    @Column(name = "preferred_religion", length = 100)
    private String preferredReligion;
    
    @Column(name = "preferred_caste", length = 100)
    private String preferredCaste;
    
    @Column(name = "preferred_mother_tongue", length = 100)
    private String preferredMotherTongue;
    
    @Column(name = "preferred_income_min")
    private Integer preferredIncomeMin;
    
    @Column(name = "preferred_income_max")
    private Integer preferredIncomeMax;
    
    // Filter options
    @Column(name = "only_verified", nullable = false)
    @Builder.Default
    private boolean onlyVerified = false;
    
    @Column(name = "only_with_photos", nullable = false)
    @Builder.Default
    private boolean onlyWithPhotos = false;
    
    @Column(name = "only_online", nullable = false)
    @Builder.Default
    private boolean onlyOnline = false;
    
    @Column(name = "exclude_already_liked", nullable = false)
    @Builder.Default
    private boolean excludeAlreadyLiked = true;
    
    @Column(name = "exclude_viewed", nullable = false)
    @Builder.Default
    private boolean excludeViewed = false;
    
    @Column(name = "exclude_matched", nullable = false)
    @Builder.Default
    private boolean excludeMatched = true;
    
    // Sorting
    @Column(name = "sort_by", length = 50)
    @Builder.Default
    private String sortBy = "compatibility";
    
    @Column(name = "sort_order", length = 10)
    @Builder.Default
    private String sortOrder = "DESC";
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
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