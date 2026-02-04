package com.punarmilan.backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ProfileResponseDto {

    // ================= IDENTIFICATION =================
    private Long id;
    private String profileId;

    // ================= PERSONAL =================
    private String fullName;
    private String gender;
    private Integer age;
    private String height;
    private Double weight;
    private String maritalStatus;
    private String motherTongue;

    // ================= RELIGION =================
    private String religion;
    private String caste;
    private String subCaste;
    private String gotra;

    // ================= PHOTOS =================
    private PhotoDto profilePhotoUrl;
    private PhotoDto photoUrl2;
    private PhotoDto photoUrl3;
    private PhotoDto photoUrl4;
    private PhotoDto photoUrl5;
    private PhotoDto photoUrl6;
    private Integer photoCount;

    // ================= VERIFICATION =================
    private String verificationStatus;
    private String idProofUrl;
    private String idProofType;
    private String idProofNumber;
    private LocalDateTime verifiedAt;
    private String verifiedBy;
    private String verificationNotes;
    private Boolean verified; // ✅ Changed to Boolean

    // ================= EDUCATION & CAREER =================
    private String educationLevel;
    private String educationField;
    private String college;
    private String occupation;
    private String company;
    private String workingWith;
    private Double annualIncome;
    private String workingCity;
    private String grewUpIn;
    private String zipCode;
    private String residencyStatus;

    // ================= LIFESTYLE =================
    private String diet;
    private String bloodGroup;
    private String healthInformation;
    private String disability;
    private String drinkingHabit;
    private String smokingHabit;

    // ================= LOCATION =================
    private String country;
    private String state;
    private String city;
    private String address;

    // ================= FAMILY =================
    private String fatherStatus;
    private String motherStatus;
    private Integer brothersCount;
    private Integer sistersCount;
    private String familyFinancialStatus;
    private String familyLocation;

    // ================= ABOUT =================
    private String aboutMe;
    private String hobbies;

    // ================= SETTINGS =================
    private String profileCreatedBy;
    private String profileVisibility;
    private Boolean profileComplete; // ✅ Changed to Boolean
    private Boolean isPremium; // ✅ Add this field
    private String profilePhotoVisibility;
    private String albumPhotoVisibility;
    private String contactDisplayStatus;
    private String mobileNumber;

    // ================= ASTRO =================
    @JsonFormat(pattern = "HH:mm")
    private java.time.LocalTime timeOfBirth;
    private String placeOfBirth;
    private String manglikStatus;
    private String nakshatra;
    private String rashi;
    private String astroVisibility;

    // ================= AUDIT =================
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Long userId;
    private String userEmail;
}