package com.punarmilan.backend.service;

import com.punarmilan.backend.dto.*;
import com.punarmilan.backend.entity.PartnerPreference;
import com.punarmilan.backend.entity.Profile;

import java.util.List;
import java.util.Map;

public interface PartnerPreferenceService {
    
    /**
     * Save or update partner preferences for logged-in user
     */
    PartnerPreferenceResponseDto saveOrUpdatePreferences(PartnerPreferenceRequestDto requestDto);
    
    /**
     * Get partner preferences of logged-in user
     */
    PartnerPreferenceResponseDto getMyPreferences();
    
    /**
     * Find matching profiles based on user's preferences
     */
    List<MatchResultDto> findMatches();
    
    /**
     * Calculate match score between preference and profile
     */
    MatchResultDto calculateMatchScore(PartnerPreference preference, Profile profile);
    
    /**
     * Get match statistics and insights
     */
    Map<String, Object> getMatchStats();
    
    /**
     * Get suggested preferences based on user's profile
     * (Auto-fill preferences based on user's own profile)
     */
    PartnerPreferenceRequestDto getSuggestedPreferences();
    
    /**
     * Reset preferences to default
     */
    void resetPreferences();
    
    /**
     * Find matches with pagination
     */
    List<MatchResultDto> findMatchesWithPagination(int page, int size);
    
    /**
     * Get daily matches (auto-generated)
     */
    List<MatchResultDto> getDailyMatches();
    
    /**
     * Check compatibility between two specific profiles
     */
    MatchResultDto checkCompatibility(Long otherProfileId);
}