package com.punarmilan.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserProfileDto {
    // Basic User Info
    private Long id;
    private String email;
    private String mobileNumber;
    private String role;
    private Boolean isActive;
    private Boolean isVerified;
    private Boolean isPremium;
    private LocalDateTime lastLogin;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime premiumSince;

    // Profile Information
    private Long profileId;
    private Integer age;
    private LocalDate dateOfBirth;
    private Boolean profileComplete;
    private Double weight;

    // Personal Details
    private String fullName;
    private String gender;
    private String maritalStatus;
    private String motherTongue;
    private String address;
    private String aboutMe;
    private Double annualIncome;
    private String diet;
    private String drinkingHabit;
    private String smokingHabit;

    // Education & Career
    private String educationLevel;
    private String educationField;
    private String college;
    private String occupation;
    private String company;
    private String workingCity;

    // Religion & Caste
    private String religion;
    private String caste;
    private String subCaste;
    private String gotra;

    // Astro
    @com.fasterxml.jackson.annotation.JsonFormat(pattern = "HH:mm")
    private java.time.LocalTime timeOfBirth;
    private String placeOfBirth;
    private String manglikStatus;
    private String nakshatra;
    private String rashi;
    private String astroVisibility;

    // Physical Attributes
    private String height;
    private String hobbies; // JSON array or comma-separated

    // Location
    private String country;
    private String state;
    private String city;

    // Profile Settings
    private String profileCreatedBy;
    private String profileVisibility;
    private Integer photoCount;

    // Photos
    private String profilePhotoUrl;
    private String photoUrl2;
    private String photoUrl3;
    private String photoUrl4;
    private String photoUrl5;
    private String photoUrl6;

    // ID Proof & Verification
    private String idProofType;
    private String idProofNumber;
    private String idProofUrl;
    private String verificationStatus; // PENDING, VERIFIED, REJECTED
    private String verificationNotes;
    private LocalDateTime verifiedAt;
    private String verifiedBy;

    // Additional Info
    private List<String> allPhotos; // All uploaded photos URLs
}