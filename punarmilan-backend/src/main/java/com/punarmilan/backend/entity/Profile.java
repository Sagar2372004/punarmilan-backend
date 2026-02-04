package com.punarmilan.backend.entity;

import com.punarmilan.backend.entity.enums.AlbumPhotoVisibility;
import com.punarmilan.backend.entity.enums.ProfilePhotoVisibility;
import jakarta.persistence.*;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "profiles", indexes = {
        @Index(name = "idx_profile_user", columnList = "user_id"),
        @Index(name = "idx_profile_search_basic", columnList = "gender, religion, caste"),
        @Index(name = "idx_profile_location", columnList = "city, state"),
        @Index(name = "idx_profile_marital", columnList = "marital_status"),
        @Index(name = "idx_profile_dob", columnList = "date_of_birth")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Profile {

    public enum VerificationStatus {
        UNVERIFIED, PENDING, VERIFIED, REJECTED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    // Personal Information
    @Column(name = "full_name")
    private String fullName;

    @Column(name = "gender")
    private String gender;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Transient
    public Integer getAge() {
        if (dateOfBirth == null)
            return null;
        return LocalDate.now().getYear() - dateOfBirth.getYear();
    }

    @Column(name = "marital_status")
    private String maritalStatus;

    @Column(name = "mother_tongue")
    private String motherTongue;

    @Column(name = "height")
    private String height;

    @Column(name = "weight")
    private Double weight;

    // Religious Information
    @Column(name = "religion")
    private String religion;

    @Column(name = "caste")
    private String caste;

    @Column(name = "sub_caste")
    private String subCaste;

    @Column(name = "gotra")
    private String gotra;

    // Education & Career
    @Column(name = "education_level")
    private String educationLevel;

    @Column(name = "education_field")
    private String educationField;

    @Column(name = "college")
    private String college;

    @Column(name = "occupation")
    private String occupation;

    @Column(name = "company")
    private String company;

    @Column(name = "working_with")
    private String workingWith;

    @Column(name = "annual_income")
    private Double annualIncome;

    // Location
    @Column(name = "address")
    private String address;

    @Column(name = "city")
    private String city;

    @Column(name = "state")
    private String state;

    @Column(name = "country")
    private String country;

    @Column(name = "working_city")
    private String workingCity;

    @Column(name = "grew_up_in")
    private String grewUpIn;

    @Column(name = "zip_code")
    private String zipCode;

    @Column(name = "residency_status")
    private String residencyStatus;

    // Lifestyle
    @Column(name = "diet")
    private String diet;

    @Column(name = "blood_group")
    private String bloodGroup;

    @Column(name = "health_information")
    private String healthInformation;

    @Column(name = "disability")
    private String disability;

    // Family Details
    @Column(name = "father_status")
    private String fatherStatus;

    @Column(name = "mother_status")
    private String motherStatus;

    @Column(name = "brothers_count")
    private Integer brothersCount;

    @Column(name = "sisters_count")
    private Integer sistersCount;

    @Column(name = "family_financial_status")
    private String familyFinancialStatus;

    @Column(name = "family_location")
    private String familyLocation;

    // Lifestyle HABITS
    @Column(name = "drinking_habit")
    private String drinkingHabit;

    @Column(name = "smoking_habit")
    private String smokingHabit;

    // Profile Information
    @Column(name = "about_me", columnDefinition = "TEXT")
    private String aboutMe;

    @Column(name = "hobbies", columnDefinition = "TEXT")
    private String hobbies; // JSON array or comma-separated

    @Column(name = "profile_created_by")
    private String profileCreatedBy;

    @Column(name = "profile_visibility")
    private String profileVisibility;

    @Column(name = "profile_complete")
    private Boolean profileComplete;

    @Column(name = "contact_display_status")
    private String contactDisplayStatus; // Only Premium Members, Only Premium Members you like, Only visible to all
                                         // your Matches, No one

    @Enumerated(EnumType.STRING)
    @Column(name = "profile_photo_visibility")
    @Builder.Default
    private ProfilePhotoVisibility profilePhotoVisibility = ProfilePhotoVisibility.ALL_MEMBERS;

    @Enumerated(EnumType.STRING)
    @Column(name = "album_photo_visibility")
    @Builder.Default
    private AlbumPhotoVisibility albumPhotoVisibility = AlbumPhotoVisibility.LIKED_AND_PREMIUM;

    // Astro Details
    @Column(name = "time_of_birth")
    private java.time.LocalTime timeOfBirth;

    @Column(name = "place_of_birth")
    private String placeOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(name = "manglik_status")
    private com.punarmilan.backend.entity.enums.ManglikStatus manglikStatus;

    private String nakshatra;
    private String rashi;

    @Enumerated(EnumType.STRING)
    @Column(name = "astro_visibility", nullable = false)
    @Builder.Default
    private com.punarmilan.backend.entity.enums.AstroVisibility astroVisibility = com.punarmilan.backend.entity.enums.AstroVisibility.ALL_MEMBERS;

    // Photos
    @Column(name = "profile_photo_url")
    private String profilePhotoUrl;

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

    @Column(name = "photo_count")
    private Integer photoCount;

    // ID Proof & Verification
    @Column(name = "id_proof_type")
    private String idProofType;

    @Column(name = "id_proof_number")
    private String idProofNumber;

    @Column(name = "id_proof_url")
    private String idProofUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status")
    private VerificationStatus verificationStatus; // PENDING, VERIFIED, REJECTED

    @Column(name = "verification_notes", columnDefinition = "TEXT")
    private String verificationNotes;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "verified_by")
    private String verifiedBy;

    // Timestamps
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (verificationStatus == null) {
            verificationStatus = VerificationStatus.PENDING;
        }
        if (profileComplete == null) {
            profileComplete = calculateProfileCompletion();
        }
        updatePhotoCount();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        profileComplete = calculateProfileCompletion();
        updatePhotoCount();
    }

    private boolean calculateProfileCompletion() {
        int totalFields = 0;
        int filledFields = 0;

        if (fullName != null && !fullName.isEmpty())
            filledFields++;
        totalFields++;

        if (gender != null && !gender.isEmpty())
            filledFields++;
        totalFields++;

        if (dateOfBirth != null)
            filledFields++;
        totalFields++;

        if (height != null)
            filledFields++;
        totalFields++;

        if (religion != null && !religion.isEmpty())
            filledFields++;
        totalFields++;

        if (caste != null && !caste.isEmpty())
            filledFields++;
        totalFields++;

        if (maritalStatus != null && !maritalStatus.isEmpty())
            filledFields++;
        totalFields++;

        if (educationLevel != null && !educationLevel.isEmpty())
            filledFields++;
        totalFields++;

        if (occupation != null && !occupation.isEmpty())
            filledFields++;
        totalFields++;

        if (profilePhotoUrl != null && !profilePhotoUrl.isEmpty())
            filledFields++;
        totalFields++;

        return (filledFields * 100.0 / totalFields) >= 70.0;
    }

    private void updatePhotoCount() {
        int count = 0;
        if (profilePhotoUrl != null && !profilePhotoUrl.isEmpty())
            count++;
        if (photoUrl2 != null && !photoUrl2.isEmpty())
            count++;
        if (photoUrl3 != null && !photoUrl3.isEmpty())
            count++;
        if (photoUrl4 != null && !photoUrl4.isEmpty())
            count++;
        if (photoUrl5 != null && !photoUrl5.isEmpty())
            count++;
        if (photoUrl6 != null && !photoUrl6.isEmpty())
            count++;
        this.photoCount = count;
    }

    // Helper method to get all photos
    public List<String> getAllPhotos() {
        List<String> photos = new java.util.ArrayList<>();
        if (profilePhotoUrl != null && !profilePhotoUrl.isEmpty())
            photos.add(profilePhotoUrl);
        if (photoUrl2 != null && !photoUrl2.isEmpty())
            photos.add(photoUrl2);
        if (photoUrl3 != null && !photoUrl3.isEmpty())
            photos.add(photoUrl3);
        if (photoUrl4 != null && !photoUrl4.isEmpty())
            photos.add(photoUrl4);
        if (photoUrl5 != null && !photoUrl5.isEmpty())
            photos.add(photoUrl5);
        if (photoUrl6 != null && !photoUrl6.isEmpty())
            photos.add(photoUrl6);
        return photos;
    }

    public boolean isVerified() {
        return verificationStatus == VerificationStatus.VERIFIED;
    }

    public boolean isPremium() {
        return user != null && Boolean.TRUE.equals(user.getPremium());
    }

    public boolean isProfileComplete() {
        return Boolean.TRUE.equals(profileComplete);
    }
}