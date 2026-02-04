package com.punarmilan.backend.controller;

import com.punarmilan.backend.dto.MatchCategoryDTO;
import com.punarmilan.backend.dto.MatchFilterDTO;
import com.punarmilan.backend.dto.MatchResponseDTO;
import com.punarmilan.backend.service.MatchCategoryService;
import com.punarmilan.backend.service.MatchService;
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
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Match Categories", description = "Browse matches by different categories")
@PreAuthorize("hasRole('USER')")
public class MatchCategoryController {

    private final MatchCategoryService matchCategoryService;
    private final MatchService matchService;

    @Operation(summary = "Get all match categories with counts")
    @GetMapping("/match-categories")
    public ResponseEntity<List<MatchCategoryDTO>> getAllCategories() {
        List<MatchCategoryDTO> categories = matchCategoryService.getAllCategories();
        return ResponseEntity.ok(categories);
    }

    @Operation(summary = "Get category by slug")
    @GetMapping("/match-categories/{slug}")
    public ResponseEntity<MatchCategoryDTO> getCategoryBySlug(@PathVariable String slug) {
        MatchCategoryDTO category = matchCategoryService.getCategoryBySlug(slug);
        return ResponseEntity.ok(category);
    }

    @Operation(summary = "Get matches by category")
    @GetMapping("/match-categories/{category}/matches")
    public ResponseEntity<MatchResponseDTO.MatchListResponse> getMatchesByCategory(
            @PathVariable String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {

        Sort.Direction direction = sortDirection.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        MatchResponseDTO.MatchListResponse response = matchCategoryService.getMatchesByCategory(category, pageable);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get new matches")
    @GetMapping("/match-categories/new/matches")
    public ResponseEntity<MatchResponseDTO.MatchListResponse> getNewMatches(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        MatchResponseDTO.MatchListResponse response = matchCategoryService.getNewMatches(pageable);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get today's matches")
    @GetMapping("/match-categories/today/matches")
    public ResponseEntity<MatchResponseDTO.MatchListResponse> getTodaysMatches(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("lastActive").descending());
        MatchResponseDTO.MatchListResponse response = matchCategoryService.getTodaysMatches(pageable);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get my matches")
    @GetMapping("/match-categories/my/matches")
    public ResponseEntity<MatchResponseDTO.MatchListResponse> getMyMatches(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        MatchResponseDTO.MatchListResponse response = matchCategoryService.getMyMatches(pageable);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get matches near me")
    @GetMapping("/match-categories/near/matches")
    public ResponseEntity<MatchResponseDTO.MatchListResponse> getNearMeMatches(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("distanceKm").ascending());
        MatchResponseDTO.MatchListResponse response = matchCategoryService.getNearMeMatches(pageable);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get more matches")
    @GetMapping("/match-categories/more/matches")
    public ResponseEntity<MatchResponseDTO.MatchListResponse> getMoreMatches(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("compatibilityScore").descending());
        MatchResponseDTO.MatchListResponse response = matchCategoryService.getMoreMatches(pageable);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Search matches with filters")
    @PostMapping("/match-categories/search")
    public ResponseEntity<MatchResponseDTO.MatchListResponse> searchMatches(
            @RequestBody MatchFilterDTO filterDTO,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        MatchResponseDTO.MatchListResponse response = matchCategoryService.searchMatches(filterDTO, pageable);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get match statistics")
    @GetMapping("/match-categories/stats")
    public ResponseEntity<MatchResponseDTO.MatchStatsResponse> getMatchStats() {
        MatchResponseDTO.MatchStatsResponse stats = matchCategoryService.getMatchStats();
        return ResponseEntity.ok(stats);
    }

    @Operation(summary = "Get category counts")
    @GetMapping("/match-categories/counts")
    public ResponseEntity<Map<String, Integer>> getCategoryCounts() {
        Map<String, Integer> counts = matchCategoryService.getCategoryCounts();
        return ResponseEntity.ok(counts);
    }

    @Operation(summary = "Mark user as viewed")
    @PostMapping("/match-categories/view/{userId}")
    public ResponseEntity<Void> markAsViewed(@PathVariable Long userId) {
        matchCategoryService.markAsViewed(userId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Like a user")
    @PostMapping("/match-categories/like/{userId}")
    public ResponseEntity<Void> markAsLiked(@PathVariable Long userId) {
        matchCategoryService.markAsLiked(userId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Skip a user")
    @PostMapping("/match-categories/skip/{userId}")
    public ResponseEntity<Void> skipUser(@PathVariable Long userId) {
        matchCategoryService.skipUser(userId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Get match suggestions")
    @GetMapping("/match-categories/suggestions")
    public ResponseEntity<Page<MatchResponseDTO>> getMatchSuggestions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<MatchResponseDTO> suggestions = matchCategoryService.getMatchSuggestions(pageable);
        return ResponseEntity.ok(suggestions);
    }

    @Operation(summary = "Get recently viewed profiles")
    @GetMapping("/match-categories/recently-viewed")
    public ResponseEntity<Page<MatchResponseDTO>> getRecentlyViewed(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size); // ✅ NO SORT HERE

        Page<MatchResponseDTO> recentlyViewed = matchCategoryService.getRecentlyViewed(pageable);

        return ResponseEntity.ok(recentlyViewed);
    }

    @Operation(summary = "Get mutual likes")
    @GetMapping("/match-categories/mutual-likes")
    public ResponseEntity<Page<MatchResponseDTO>> getMutualLikes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<MatchResponseDTO> mutualLikes = matchCategoryService.getMutualLikes(pageable);
        return ResponseEntity.ok(mutualLikes);
    }

    @Operation(summary = "Create new category (Admin only)")
    @PostMapping("/match-categories")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MatchCategoryDTO> createCategory(
            @Valid @RequestBody MatchCategoryDTO.CategoryRequest request) {
        MatchCategoryDTO category = matchCategoryService.createCategory(request);
        return ResponseEntity.ok(category);
    }

    @Operation(summary = "Update category (Admin only)")
    @PutMapping("/match-categories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MatchCategoryDTO> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody MatchCategoryDTO.CategoryRequest request) {
        MatchCategoryDTO category = matchCategoryService.updateCategory(id, request);
        return ResponseEntity.ok(category);
    }

    @Operation(summary = "Delete category (Admin only)")
    @DeleteMapping("/match-categories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        matchCategoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /api/matches/new-registrations
     * Fetches real-time new registrations from MySQL (last 24h).
     * Live inside MatchCategoryController as requested.
     */
    @Operation(summary = "Get users registered in the last 24 hours (Real-time MySQL)")
    @GetMapping("/matches/new-registrations")
    public ResponseEntity<MatchResponseDTO.MatchListResponse> getNewRegistrations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(matchCategoryService.getNewRegistrations(pageable));
    }

    /**
     * GET /api/matches/new
     * Fetches pre-calculated matches from Redis.
     * Moved from NewMatchesController.
     */
    @Operation(summary = "Get Today's calculated matches (Redis-based)")
    @GetMapping("/matches/new")
    public ResponseEntity<MatchResponseDTO.MatchListResponse> getTodayMatchesRedis(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(matchService.getNewMatchesFromCache(pageable));
    }
}
