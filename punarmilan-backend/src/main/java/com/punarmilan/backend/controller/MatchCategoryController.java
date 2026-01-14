package com.punarmilan.backend.controller;

import com.punarmilan.backend.dto.MatchCategoryDTO;
import com.punarmilan.backend.dto.MatchFilterDTO;
import com.punarmilan.backend.dto.MatchResponseDTO;
import com.punarmilan.backend.service.MatchCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/match-categories")
@RequiredArgsConstructor
@Tag(name = "Match Categories", description = "Browse matches by different categories")
@PreAuthorize("hasRole('USER')")
public class MatchCategoryController {

    private final MatchCategoryService matchCategoryService;

    @Operation(summary = "Get all match categories with counts")
    @GetMapping
    public ResponseEntity<List<MatchCategoryDTO>> getAllCategories() {
        List<MatchCategoryDTO> categories = matchCategoryService.getAllCategories();
        return ResponseEntity.ok(categories);
    }

    @Operation(summary = "Get category by slug")
    @GetMapping("/{slug}")
    public ResponseEntity<MatchCategoryDTO> getCategoryBySlug(@PathVariable String slug) {
        MatchCategoryDTO category = matchCategoryService.getCategoryBySlug(slug);
        return ResponseEntity.ok(category);
    }

    @Operation(summary = "Get matches by category")
    @GetMapping("/{category}/matches")
    public ResponseEntity<MatchResponseDTO.MatchListResponse> getMatchesByCategory(
            @PathVariable String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {
        
        Sort.Direction direction = sortDirection.equalsIgnoreCase("asc") ? 
                Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        
        MatchResponseDTO.MatchListResponse response = matchCategoryService.getMatchesByCategory(category, pageable);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get new matches")
    @GetMapping("/new/matches")
    public ResponseEntity<MatchResponseDTO.MatchListResponse> getNewMatches(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        MatchResponseDTO.MatchListResponse response = matchCategoryService.getNewMatches(pageable);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get today's matches")
    @GetMapping("/today/matches")
    public ResponseEntity<MatchResponseDTO.MatchListResponse> getTodaysMatches(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("lastActive").descending());
        MatchResponseDTO.MatchListResponse response = matchCategoryService.getTodaysMatches(pageable);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get my matches")
    @GetMapping("/my/matches")
    public ResponseEntity<MatchResponseDTO.MatchListResponse> getMyMatches(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("matchedAt").descending());
        MatchResponseDTO.MatchListResponse response = matchCategoryService.getMyMatches(pageable);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get matches near me")
    @GetMapping("/near/matches")
    public ResponseEntity<MatchResponseDTO.MatchListResponse> getNearMeMatches(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("distanceKm").ascending());
        MatchResponseDTO.MatchListResponse response = matchCategoryService.getNearMeMatches(pageable);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get more matches")
    @GetMapping("/more/matches")
    public ResponseEntity<MatchResponseDTO.MatchListResponse> getMoreMatches(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("compatibilityScore").descending());
        MatchResponseDTO.MatchListResponse response = matchCategoryService.getMoreMatches(pageable);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Search matches with filters")
    @PostMapping("/search")
    public ResponseEntity<MatchResponseDTO.MatchListResponse> searchMatches(
            @RequestBody MatchFilterDTO filterDTO,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        MatchResponseDTO.MatchListResponse response = matchCategoryService.searchMatches(filterDTO, pageable);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get match statistics")
    @GetMapping("/stats")
    public ResponseEntity<MatchResponseDTO.MatchStatsResponse> getMatchStats() {
        MatchResponseDTO.MatchStatsResponse stats = matchCategoryService.getMatchStats();
        return ResponseEntity.ok(stats);
    }

    @Operation(summary = "Get category counts")
    @GetMapping("/counts")
    public ResponseEntity<Map<String, Integer>> getCategoryCounts() {
        Map<String, Integer> counts = matchCategoryService.getCategoryCounts();
        return ResponseEntity.ok(counts);
    }

    @Operation(summary = "Mark user as viewed")
    @PostMapping("/view/{userId}")
    public ResponseEntity<Void> markAsViewed(@PathVariable Long userId) {
        matchCategoryService.markAsViewed(userId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Like a user")
    @PostMapping("/like/{userId}")
    public ResponseEntity<Void> markAsLiked(@PathVariable Long userId) {
        matchCategoryService.markAsLiked(userId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Skip a user")
    @PostMapping("/skip/{userId}")
    public ResponseEntity<Void> skipUser(@PathVariable Long userId) {
        matchCategoryService.skipUser(userId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Get match suggestions")
    @GetMapping("/suggestions")
    public ResponseEntity<Page<MatchResponseDTO>> getMatchSuggestions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<MatchResponseDTO> suggestions = matchCategoryService.getMatchSuggestions(pageable);
        return ResponseEntity.ok(suggestions);
    }

    @Operation(summary = "Get recently viewed profiles")
    @GetMapping("/recently-viewed")
    public ResponseEntity<Page<MatchResponseDTO>> getRecentlyViewed(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size); // ✅ NO SORT HERE

        Page<MatchResponseDTO> recentlyViewed =
                matchCategoryService.getRecentlyViewed(pageable);

        return ResponseEntity.ok(recentlyViewed);
    }

    @Operation(summary = "Get mutual likes")
    @GetMapping("/mutual-likes")
    public ResponseEntity<Page<MatchResponseDTO>> getMutualLikes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<MatchResponseDTO> mutualLikes = matchCategoryService.getMutualLikes(pageable);
        return ResponseEntity.ok(mutualLikes);
    }

    @Operation(summary = "Create new category (Admin only)")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MatchCategoryDTO> createCategory(
            @Valid @RequestBody MatchCategoryDTO.CategoryRequest request) {
        MatchCategoryDTO category = matchCategoryService.createCategory(request);
        return ResponseEntity.ok(category);
    }

    @Operation(summary = "Update category (Admin only)")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MatchCategoryDTO> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody MatchCategoryDTO.CategoryRequest request) {
        MatchCategoryDTO category = matchCategoryService.updateCategory(id, request);
        return ResponseEntity.ok(category);
    }

    @Operation(summary = "Delete category (Admin only)")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        matchCategoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }
}
