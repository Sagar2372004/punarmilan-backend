package com.punarmilan.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProfileVerificationRequest {
    @NotNull(message = "UserId is required")
    private Long userId;
    
    @NotBlank(message = "Verification status is required")
    private String status; // VERIFIED, REJECTED
    
    private String notes;
    
    private String verifiedBy; // Admin email
}