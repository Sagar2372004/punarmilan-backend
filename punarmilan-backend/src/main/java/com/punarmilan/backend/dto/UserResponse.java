package com.punarmilan.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserResponse {
    private Long id;
    private String email;
    private String mobileNumber;
    private String profileId;
    private String role;
    private Boolean isActive; // Changed from boolean to Boolean
    private Boolean isVerified; // Changed from boolean to Boolean
    private Boolean isPremium; // Added this field
    private Boolean isHidden;
    private LocalDateTime hiddenUntil;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastLogin;
    private LocalDateTime premiumSince; // Added this field
}