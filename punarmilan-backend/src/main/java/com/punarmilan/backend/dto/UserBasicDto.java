package com.punarmilan.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserBasicDto {
    
    private Long id;
    private String email;
    private String fullName;
    private String gender;
    private Integer age;
    private String city;
    private String profilePhotoUrl;
    private boolean isVerified;
    private String occupation;
    private String education;
    private String religion;
    private String caste;
    
    // Additional fields for matching
    private boolean isOnline;
    private boolean isPremium;
    private String lastActive;
    
    // Distance info (for near me)
    private Double distanceKm;
    private String distanceText;
    
    // Compatibility
    private Integer compatibilityScore;
    private String compatibilityPercentage;
}