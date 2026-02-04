package com.punarmilan.backend.service.impl;

import com.punarmilan.backend.entity.User;
import com.punarmilan.backend.repository.UserRepository;
import com.punarmilan.backend.service.MatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class MatchScheduler {

    private final MatchService matchService;
    private final UserRepository userRepository;
    private final Executor taskExecutor;

    /**
     * Runs daily at 2 AM to refresh New Matches for all active users.
     * Refactored to use Parallel Batching for high performance and safety.
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void refreshDailyMatches() {
        log.info("Starting Parallel Match Refresh Job at 2 AM...");

        int pageSize = 100;
        int pageNumber = 0;
        Page<User> userPage;
        long totalProcessed = 0;

        do {
            // Fetch users in batches of 100 using pagination
            userPage = userRepository.findAll(PageRequest.of(pageNumber, pageSize));
            List<User> users = userPage.getContent().stream()
                    .filter(User::isActive)
                    .collect(Collectors.toList());

            if (!users.isEmpty()) {
                // Initialize asynchronous tasks for the current batch
                List<CompletableFuture<Void>> futures = users.stream()
                        .map(user -> CompletableFuture.runAsync(() -> {
                            try {
                                matchService.computeAndCacheMatches(user.getId());
                            } catch (Exception e) {
                                log.error("Error calculating matches for user {}: {}", user.getId(), e.getMessage());
                            }
                        }, taskExecutor))
                        .collect(Collectors.toList());

                // BLOCKING SYNC: Wait for the entire batch of 100 to finish
                // This prevents the system (and DB) from being overwhelmed by too many parallel
                // threads
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

                totalProcessed += users.size();
                log.info("Batch {} complete. Total users processed so far: {}", pageNumber + 1, totalProcessed);
            }

            pageNumber++;
        } while (userPage.hasNext());

        log.info("Daily Parallel Match Refresh completed. Total users: {}", totalProcessed);
    }
}
