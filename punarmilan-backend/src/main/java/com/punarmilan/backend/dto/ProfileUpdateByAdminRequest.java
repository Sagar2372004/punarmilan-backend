package com.punarmilan.backend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ProfileUpdateByAdminRequest {
    @NotNull(message = "UserId is required")
    private Long userId;
    
    private String fullName;
    private String gender;
    private LocalDate dateOfBirth;
    private String maritalStatus;
    private String religion;
    private String caste;
    private String subCaste;
    private String occupation;
    private Double annualIncome;
    private String address;
    private String city;
    private String state;
    private String country;
    
    // ID Proof info
    private String idProofType;
    private String idProofNumber;
    
    private Boolean isActive;
    private Boolean isPremium;
}