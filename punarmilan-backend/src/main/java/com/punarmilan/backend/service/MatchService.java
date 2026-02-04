package com.punarmilan.backend.service;

import com.punarmilan.backend.dto.MatchResponseDTO;
import org.springframework.data.domain.Pageable;

public interface MatchService {

    /**
     * Pre-calculates matches for a user and stores them in Redis.
     * Triggered by daily scheduler or registration.
     */
    void computeAndCacheMatches(Long userId);

    /**
     * Fetches New Matches from Redis ZSet for the logged-in user.
     */
    MatchResponseDTO.MatchListResponse getNewMatchesFromCache(Pageable pageable);
}
