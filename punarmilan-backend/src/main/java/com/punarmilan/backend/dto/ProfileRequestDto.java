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
    private String profileCreatedBy;    // Self / Parents
    private String profileVisibility;   // Public / Private
}