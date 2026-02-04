package com.punarmilan.backend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PartnerPreferenceRequestDto {
    private Integer minAge;
    private Integer maxAge;
    private String minHeight;
    private String maxHeight;
    private String preferredReligion;
    private String preferredCaste;
    private String preferredSubCaste;
    private String minEducationLevel;
    private String preferredCity;
    private String preferredState;
    private String preferredDiet;
    private String drinkingHabit;
    private String smokingHabit;
    private String maritalStatus;
    private String occupation;
    private String workingWith;
    private String minAnnualIncome;
    private Boolean preferWorkingProfessional;
    private Boolean preferNri;
    private Boolean showVerifiedOnly;
    private Boolean enableAutoMatch;
    private Integer matchScoreThreshold;
}
