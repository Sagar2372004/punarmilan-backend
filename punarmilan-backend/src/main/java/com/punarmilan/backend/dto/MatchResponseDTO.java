package com.punarmilan.backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MatchResponseDTO {
    
    // Match information
    private Long matchId;
    private Long userId;
    private boolean isLiked;
    private boolean isMatched;
    private boolean isViewed;
    
    // User information
    private UserBasicDto user;  // ✅ Use UserBasicDto here
    private List<String> photos;
    private String primaryPhoto;
    
    // Stats (duplicate from user for easy access)
    private Integer age;
    private String city;
    private String occupation;
    private String education;
    private String height;
    private String religion;
    private String caste;
    
    // Compatibility
    private Integer compatibilityScore;
    private String compatibilityPercentage;
    private List<String> commonInterests;
    
    // Status indicators
    private boolean isOnline;
    private boolean isVerified;
    private boolean isPremium;
    private LocalDateTime lastActive;
    
    // Distance
    private Double distanceKm;
    private String distanceText;
    
    // Actions
    private boolean canLike;
    private boolean canChat;
    private boolean canViewProfile;
    private boolean canBlock;
    
    // Timestamps
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime matchedAt;
    
    // For "Near Me" category
    private GeoLocationDTO location;
    
    // For "Today's" category
    private boolean isNewToday;
    private boolean isRecentlyActive;
    
    // Inner classes
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeoLocationDTO {
        private Double latitude;
        private Double longitude;
        private String address;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MatchListResponse {
        private String category;
        private String title;
        private Integer totalCount;
        private List<MatchResponseDTO> matches;
        private Integer page;
        private Integer size;
        private Integer totalPages;
        private boolean hasNext;
        private boolean hasPrevious;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MatchStatsResponse {
        private Integer newMatches;
        private Integer todaysMatches;
        private Integer myMatches;
        private Integer nearMeMatches;
        private Integer moreMatches;
        private Integer totalMatches;
        private Integer unviewedMatches;
        private Integer mutualLikes;
        
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime lastUpdated;
    }
}