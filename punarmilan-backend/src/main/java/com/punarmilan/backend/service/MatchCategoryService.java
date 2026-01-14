package com.punarmilan.backend.service;

import com.punarmilan.backend.dto.MatchCategoryDTO;
import com.punarmilan.backend.dto.MatchFilterDTO;
import com.punarmilan.backend.dto.MatchResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface MatchCategoryService {
    
    // Category Management
    List<MatchCategoryDTO> getAllCategories();
    MatchCategoryDTO getCategoryBySlug(String slug);
    MatchCategoryDTO createCategory(MatchCategoryDTO.CategoryRequest request);
    MatchCategoryDTO updateCategory(Long id, MatchCategoryDTO.CategoryRequest request);
    void deleteCategory(Long id);
    
    // Match fetching by category
    MatchResponseDTO.MatchListResponse getMatchesByCategory(String category, Pageable pageable);
    MatchResponseDTO.MatchListResponse getNewMatches(Pageable pageable);
    MatchResponseDTO.MatchListResponse getTodaysMatches(Pageable pageable);
    MatchResponseDTO.MatchListResponse getMyMatches(Pageable pageable);
    MatchResponseDTO.MatchListResponse getNearMeMatches(Pageable pageable);
    MatchResponseDTO.MatchListResponse getMoreMatches(Pageable pageable);
    
    // Filtered matches
    MatchResponseDTO.MatchListResponse searchMatches(MatchFilterDTO filterDTO, Pageable pageable);
    
    // Stats
    MatchResponseDTO.MatchStatsResponse getMatchStats();
    Map<String, Integer> getCategoryCounts();
    
    // Quick actions
    void markAsViewed(Long userId);
    void markAsLiked(Long userId);
    void skipUser(Long userId);
    
    // Get match suggestions
    Page<MatchResponseDTO> getMatchSuggestions(Pageable pageable);
    
    // Get recently viewed
    Page<MatchResponseDTO> getRecentlyViewed(Pageable pageable);
    
    // Get mutual likes
    Page<MatchResponseDTO> getMutualLikes(Pageable pageable);
}