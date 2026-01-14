package com.punarmilan.backend.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ProfileResponseDto {

    // ================= IDENTIFICATION =================
    private Long id;

    // ================= PERSONAL =================
    private String fullName;
    private String gender;
    private Integer age;
    private Integer height;
    private Double weight;
    private String maritalStatus;
    private String motherTongue;

    // ================= RELIGION =================
    private String religion;
    private String caste;
    private String subCaste;
    private String gotra;
    
    // ================= PHOTOS =================
    private String profilePhotoUrl;
    private String photoUrl2;
    private String photoUrl3;
    private String photoUrl4;
    private String photoUrl5;
    private String photoUrl6;
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
    private String annualIncome;
    private String workingCity;

    // ================= LIFESTYLE =================
    private String diet;
    private String drinkingHabit;
    private String smokingHabit;

    // ================= LOCATION =================
    private String country;
    private String state;
    private String city;
    private String address;

    // ================= ABOUT =================
    private String aboutMe;

    // ================= SETTINGS =================
    private String profileCreatedBy;
    private String profileVisibility;
    private Boolean profileComplete; // ✅ Changed to Boolean
    private Boolean isPremium; // ✅ Add this field

    // ================= AUDIT =================
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}