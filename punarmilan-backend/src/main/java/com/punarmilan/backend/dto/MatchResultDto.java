package com.punarmilan.backend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MatchResultDto {
    private ProfileResponseDto profile;
    private Integer matchScore;
    private String matchPercentage;
    private String matchReason;
    private Boolean isPremiumMatch;
    
    @Data
    @Builder
    public static class MatchCriteria {
        private Boolean ageMatch;
        private Boolean heightMatch;
        private Boolean religionMatch;
        private Boolean locationMatch;
        private Boolean educationMatch;
        private Boolean lifestyleMatch;
        private Integer totalMatches;
        private Integer totalCriteria;
    }
 // Add userId to access user information
    private Long userId;
}