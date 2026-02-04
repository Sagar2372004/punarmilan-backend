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
public class MatchFilterDTO {

    // Age range
    private Integer minAge;
    private Integer maxAge;

    // Location
    private String city;
    private Integer maxDistanceKm;

    // Education & Profession
    private String educationLevel;
    private String occupation;
    private String workingWith;

    // Height
    private Integer minHeightCm;
    private Integer maxHeightCm;

    // Marital Status
    private String maritalStatus;

    // Preferences
    private String preferredGender;
    private String preferredReligion;
    private String preferredCaste;
    private String preferredMotherTongue;
    private Integer preferredIncomeMin;
    private Integer preferredIncomeMax;

    // Filter options
    private boolean onlyVerified;
    private boolean onlyWithPhotos;
    private boolean onlyOnline;
    private boolean excludeAlreadyLiked;
    private boolean excludeViewed;
    private boolean excludeMatched;

    // Sorting
    private String sortBy; // "compatibility", "recent", "distance", "age"
    private String sortOrder; // "ASC", "DESC"

    // Pagination
    private Integer page;
    private Integer size;
    private String category; // "new", "today", "my", "near", "more"

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SaveFilterRequest {
        private String filterName;
        private MatchFilterDTO filterData;
    }
}