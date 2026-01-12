package com.punarmilan.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Profile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ================= USER LINK =================
    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    // ================= PERSONAL DETAILS =================
    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String gender;

    @Column(nullable = false)
    private LocalDate dateOfBirth;

    private Integer age;          // calculated
    private String height;        // 5'8"
    private Double weight;        // in kg

    private String maritalStatus;
    private String motherTongue;
    
    // ================= PROFILE PHOTOS =================
    @Column(name = "profile_photo_url")
    private String profilePhotoUrl;  // Main profile photo
    
    @Column(name = "photo_url_2")
    private String photoUrl2;
    
    @Column(name = "photo_url_3")
    private String photoUrl3;
    
    @Column(name = "photo_url_4")
    private String photoUrl4;
    
    @Column(name = "photo_url_5")
    private String photoUrl5;
    
    @Column(name = "photo_url_6")
    private String photoUrl6;
    
    @Builder.Default
    private Integer photoCount = 0;

    // ================= VERIFICATION SYSTEM =================
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private VerificationStatus verificationStatus = VerificationStatus.UNVERIFIED;
    
    @Column(name = "id_proof_url")
    private String idProofUrl; // Aadhar/PAN photo URL
    
    @Column(name = "id_proof_type")
    private String idProofType; // AADHAR, PAN, PASSPORT
    
    @Column(name = "id_proof_number")
    private String idProofNumber; // Aadhar/PAN number
    
    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;
    
    @Column(name = "verified_by")
    private String verifiedBy; // Admin email who verified
    
    @Column(name = "verification_notes", length = 500)
    private String verificationNotes;

    // ================= RELIGION DETAILS =================
    private String religion;
    private String caste;
    private String subCaste;
    private String gotra;

    // ================= EDUCATION & CAREER =================
    private String educationLevel;
    private String educationField;
    private String college;

    private String occupation;
    private String company;
    private String annualIncome;
    private String workingCity;

    // ================= LIFESTYLE =================
    private String diet;           // Veg / Non-Veg
    private String drinkingHabit;  // Yes / No / Occasionally
    private String smokingHabit;   // Yes / No

    // ================= LOCATION =================
    private String country;
    private String state;
    private String city;

    @Column(length = 500)
    private String address;

    // ================= ABOUT =================
    @Column(length = 2000)
    private String aboutMe;

    // ================= PROFILE SETTINGS =================
    private String profileCreatedBy;   // Self / Parents
    private String profileVisibility;  // Public / Private

    private boolean profileComplete = false;

    // ================= AUDIT =================
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ================= ENUMS =================
    public enum VerificationStatus {
        UNVERIFIED,     // No verification requested
        PENDING,        // Verification submitted, waiting for admin
        VERIFIED,       // Admin approved
        REJECTED        // Admin rejected
    }
    
    // ================= HELPER METHODS =================
    public boolean isVerified() {
        return verificationStatus == VerificationStatus.VERIFIED;
    }

    // ================= JPA CALLBACKS =================
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