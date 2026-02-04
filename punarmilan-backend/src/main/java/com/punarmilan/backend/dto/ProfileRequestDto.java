package com.punarmilan.backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ProfileRequestDto {

    // ================= PERSONAL =================
    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "Gender is required")
    private String gender;

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateOfBirth;

    private String height;
    private Double weight;
    private String maritalStatus;
    private String motherTongue;

    // ================= RELIGION =================
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

    // ================= PHOTOS =================
    private String profilePhotoUrl;
    private String photoUrl2;
    private String photoUrl3;
    private String photoUrl4;
    private String photoUrl5;
    private String photoUrl6;

    // ================= VERIFICATION =================
    private String idProofUrl;
    private String idProofType; // AADHAR, PAN, PASSPORT
    private String idProofNumber;

    // ================= SETTINGS =================
    private String profileCreatedBy; // Self / Parents
    private String profileVisibility; // Public / Private
    private String profilePhotoVisibility;
    private String albumPhotoVisibility;
    private String contactDisplayStatus;

    // ================= ASTRO =================
    @JsonFormat(pattern = "HH:mm")
    private java.time.LocalTime timeOfBirth;
    private String placeOfBirth;
    private String manglikStatus;
    private String nakshatra;
    private String rashi;
    private String astroVisibility;
}
