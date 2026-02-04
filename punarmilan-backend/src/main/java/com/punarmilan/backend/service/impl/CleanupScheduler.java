package com.punarmilan.backend.service.impl;

import com.punarmilan.backend.repository.MatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class CleanupScheduler {

    private final MatchRepository matchRepository;

    /**
     * Mark matches as EXPIRED if they have passed their expiration date.
     * Runs every hour.
     */
    @Scheduled(cron = "0 0 * * * *")
    public void expireOldMatches() {
        log.info("Starting scheduled task: Expire old matches");
        int count = matchRepository.expireOldMatches(LocalDateTime.now());
        if (count > 0) {
            log.info("Successfully expired {} matches", count);
        }
    }
}
