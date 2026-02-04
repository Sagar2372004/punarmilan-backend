package com.punarmilan.backend.service.impl;

import com.punarmilan.backend.entity.Profile;
import com.punarmilan.backend.entity.User;
import com.punarmilan.backend.entity.UserViewHistory;
import com.punarmilan.backend.repository.ProfileRepository;
import com.punarmilan.backend.repository.UserRepository;
import com.punarmilan.backend.repository.UserViewHistoryRepository;
import com.punarmilan.backend.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationScheduler {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final UserViewHistoryRepository viewHistoryRepository;
    private final EmailService emailService;
    private final MatchCategoryServiceImpl matchService;
    private final java.util.concurrent.Executor taskExecutor;

    /**
     * Daily at 9:00 AM - Send Match Mail & Recent Visitors
     */
    @Scheduled(cron = "0 0 9 * * *")
    public void sendDailyDigests() {
        log.info("Starting Daily Digest Job with Parallel Batching...");
        int pageSize = 100;
        int pageNumber = 0;
        org.springframework.data.domain.Page<User> userPage;

        do {
            userPage = userRepository.findAll(org.springframework.data.domain.PageRequest.of(pageNumber, pageSize));
            List<User> users = userPage.getContent().stream()
                    .filter(User::isActive)
                    .collect(Collectors.toList());

            List<java.util.concurrent.CompletableFuture<Void>> futures = users.stream()
                    .map(user -> java.util.concurrent.CompletableFuture.runAsync(() -> {
                        try {
                            sendRecentVisitorsDigest(user);
                            sendNewMatchMail(user);
                        } catch (Exception e) {
                            log.error("Error in daily digest for user: {}", user.getEmail(), e);
                        }
                    }, taskExecutor))
                    .collect(Collectors.toList());

            // Wait for this batch to finish before moving to next to avoid overwhelming the
            // DB
            java.util.concurrent.CompletableFuture.allOf(futures.toArray(new java.util.concurrent.CompletableFuture[0]))
                    .join();

            pageNumber++;
            log.info("Processed batch {} of daily digests", pageNumber);
        } while (userPage.hasNext());

        log.info("Daily Digest Job completed.");
    }

    /**
     * Weekly on Sunday at 10:00 AM - Send Premium Match Mail & Similar Profiles
     */
    @Scheduled(cron = "0 0 10 * * SUN")
    public void sendWeeklyDigests() {
        log.info("Starting Weekly Digest Job with Parallel Batching...");
        int pageSize = 100;
        int pageNumber = 0;
        org.springframework.data.domain.Page<User> userPage;

        do {
            userPage = userRepository.findAll(org.springframework.data.domain.PageRequest.of(pageNumber, pageSize));
            List<User> users = userPage.getContent().stream()
                    .filter(User::isActive)
                    .collect(Collectors.toList());

            List<java.util.concurrent.CompletableFuture<Void>> futures = users.stream()
                    .map(user -> java.util.concurrent.CompletableFuture.runAsync(() -> {
                        try {
                            if (user.getPremium()) {
                                sendPremiumMatchMail(user);
                            }
                            sendSimilarProfilesMail(user);
                        } catch (Exception e) {
                            log.error("Error in weekly digest for user: {}", user.getEmail(), e);
                        }
                    }, taskExecutor))
                    .collect(Collectors.toList());

            java.util.concurrent.CompletableFuture.allOf(futures.toArray(new java.util.concurrent.CompletableFuture[0]))
                    .join();

            pageNumber++;
            log.info("Processed batch {} of weekly digests", pageNumber);
        } while (userPage.hasNext());

        log.info("Weekly Digest Job completed.");
    }

    private void sendRecentVisitorsDigest(User user) {
        LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
        List<UserViewHistory> recentViews = viewHistoryRepository
                .findByViewedUser(user, org.springframework.data.domain.Pageable.unpaged())
                .getContent().stream()
                .filter(v -> v.getViewedAt().isAfter(yesterday))
                .collect(Collectors.toList());

        if (!recentViews.isEmpty()) {
            List<Profile> visitorProfiles = recentViews.stream()
                    .map(v -> profileRepository.findByUser(v.getViewer()).orElse(null))
                    .filter(java.util.Objects::nonNull)
                    .distinct()
                    .limit(5)
                    .collect(Collectors.toList());

            emailService.sendVisitorsDigestEmail(user, visitorProfiles);
        }
    }

    private void sendNewMatchMail(User user) {
        // Use existing match suggestions logic
        try {
            List<com.punarmilan.backend.dto.MatchResponseDTO> suggestions = matchService.getMatchSuggestions(
                    org.springframework.data.domain.PageRequest.of(0, 5)).getContent();

            if (!suggestions.isEmpty()) {
                List<Profile> matchProfiles = suggestions.stream()
                        .map(s -> profileRepository.findByUserId(s.getUserId()).orElse(null))
                        .filter(java.util.Objects::nonNull)
                        .collect(Collectors.toList());

                emailService.sendMatchDigestEmail(user, matchProfiles);
            }
        } catch (Exception e) {
            log.error("Error sending match mail for user: {}", user.getEmail(), e);
        }
    }

    private void sendPremiumMatchMail(User user) {
        // Specialized premium logic: High compatibility + Verified profiles
        try {
            List<com.punarmilan.backend.dto.MatchResponseDTO> suggestions = matchService.getMatchSuggestions(
                    org.springframework.data.domain.PageRequest.of(0, 10)).getContent();

            List<Profile> premiumMatches = suggestions.stream()
                    .filter(s -> s.getUser().isVerified() && s.getUser().getCompatibilityScore() > 70)
                    .map(s -> profileRepository.findByUserId(s.getUserId()).orElse(null))
                    .filter(java.util.Objects::nonNull)
                    .limit(5)
                    .collect(Collectors.toList());

            if (!premiumMatches.isEmpty()) {
                // Reuse digest or Create specialized premium mail
                emailService.sendMatchDigestEmail(user, premiumMatches);
            }
        } catch (Exception e) {
            log.error("Error sending premium match mail", e);
        }
    }

    private void sendSimilarProfilesMail(User user) {
        // Implementation for similar profiles based on common traits
        // For now, reuse suggestions with different title
    }
}
