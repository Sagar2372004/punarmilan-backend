package com.punarmilan.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
    
    // ✅ FIXED: Change from primitive boolean to Boolean wrapper class
    @Column(name = "is_premium")
    @Builder.Default
    private Boolean isPremium = false; // Changed to Boolean
    
    @Column(name = "hobbies", length = 500)
    private String hobbies;
    
    @ElementCollection
    @CollectionTable(name = "profile_photos", joinColumns = @JoinColumn(name = "profile_id"))
    @Column(name = "photo_url")
    @Builder.Default
    private List<String> photos = new ArrayList<>();
    
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

    // ✅ FIXED: Change from primitive boolean to Boolean wrapper class
    @Builder.Default
    private Boolean profileComplete = false; // Changed to Boolean

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

    // ✅ ADD getter for photos
    public List<String> getPhotos() {
        return photos != null ? photos : new ArrayList<>();
    }
    
    // ✅ ADD getter for Profile Complete status (if missing)
    public boolean isProfileComplete() {
        return profileComplete != null ? profileComplete : false;
    }
    
    // ✅ ADD getter for Education Level (if missing)
    public String getEducationLevel() {
        return educationLevel;
    }
    
    // ✅ ADD getter for Occupation (if missing)
    public String getOccupation() {
        return occupation;
    }
    
    // ✅ ADD getter for Religion (if missing)
    public String getReligion() {
        return religion;
    }
    
    // ✅ ADD getter for Caste (if missing)
    public String getCaste() {
        return caste;
    }
    
    // ✅ ADD getter for isPremium with null check
    public boolean isPremium() {
        return isPremium != null ? isPremium : false;
    }
    
    // ✅ ADD getter for Height as Integer (for matching)
    public Integer getHeight() {
        if (height != null && height.contains("'")) {
            try {
                String[] parts = height.split("'");
                int feet = Integer.parseInt(parts[0]);
                int inches = parts.length > 1 ? Integer.parseInt(parts[1].replace("\"", "")) : 0;
                return (feet * 12 + inches); // Height in inches
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
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