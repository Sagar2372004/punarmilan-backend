package com.punarmilan.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "partner_preferences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartnerPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "profile_id", nullable = false, unique = true)
    private Profile profile;

    // ================= AGE PREFERENCE =================
    private Integer minAge;
    private Integer maxAge;
    
    // ================= HEIGHT PREFERENCE =================
    private String minHeight;  // 5'2"
    private String maxHeight;  // 6'0"
    
    // ================= RELIGION PREFERENCE =================
    private String preferredReligion;
    private String preferredCaste;
    private String preferredSubCaste;
    
    // ================= EDUCATION PREFERENCE =================
    private String minEducationLevel;
    private String preferredEducationField;
    
    // ================= LOCATION PREFERENCE =================
    private String preferredCity;
    private String preferredState;
    private String preferredCountry;
    
    // ================= LIFESTYLE PREFERENCE =================
    private String preferredDiet;  // Veg, Non-Veg, No Preference
    private String drinkingHabit;  // Accept, Reject, No Preference
    private String smokingHabit;   // Accept, Reject, No Preference
    
    // ================= MARITAL STATUS =================
    private String maritalStatus;  // Single, Divorced, Widowed, No Preference
    
    // ================= PROFESSION PREFERENCE =================
    private String occupation;
    private String minAnnualIncome;  // 500000, 1000000, etc
    
    // ================= ADDITIONAL PREFERENCES =================
    private Boolean preferWorkingProfessional;
    private Boolean preferNri;  // Non-Resident Indian
    
    // ================= MATCH SETTINGS =================
    @Builder.Default
    private Boolean showVerifiedOnly = true;
    
    @Builder.Default
    private Boolean enableAutoMatch = true;
    
    private Integer matchScoreThreshold;  // Minimum match score (0-100)
    
    // ================= AUDIT =================
    private LocalDateTime createdAt;
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