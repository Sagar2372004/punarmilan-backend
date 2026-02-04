package com.punarmilan.backend.service.impl;

import com.punarmilan.backend.dto.MatchResponseDTO;
import com.punarmilan.backend.dto.UserBasicDto;
import com.punarmilan.backend.entity.PartnerPreference;
import com.punarmilan.backend.entity.Profile;
import com.punarmilan.backend.entity.User;
import com.punarmilan.backend.repository.MatchingRepository;
import com.punarmilan.backend.repository.PartnerPreferenceRepository;
import com.punarmilan.backend.repository.ProfileRepository;
import com.punarmilan.backend.repository.UserRepository;
import com.punarmilan.backend.service.MatchService;
import com.punarmilan.backend.service.PhotoVisibilityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchServiceImpl implements MatchService {

    private final MatchingRepository matchingRepository;
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final PartnerPreferenceRepository partnerPreferenceRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final PhotoVisibilityService photoVisibilityService;

    private static final String REDIS_KEY_PREFIX = "user:matches:new:";

    @Override
    @Transactional(readOnly = true)
    public void computeAndCacheMatches(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null)
            return;

        Profile profile = profileRepository.findByUser(user).orElse(null);
        if (profile == null)
            return;

        PartnerPreference pref = partnerPreferenceRepository.findByProfile(profile).orElse(new PartnerPreference());

        // 1. Fetch top 100 candidates via optimized native SQL
        List<Map<String, Object>> candidates = matchingRepository.findTopCompatibleCandidates(
                userId,
                getPreferredGender(profile.getGender()),
                pref.getMinAge() != null ? pref.getMinAge() : 18,
                pref.getMaxAge() != null ? pref.getMaxAge() : 70,
                pref.getPreferredReligion(),
                pref.getMinEducationLevel(),
                pref.getMaritalStatus(),
                pref.getPreferredCity(),
                pref.getWorkingWith());

        if (candidates.isEmpty()) {
            log.info("No matches found for user: {}", userId);
            return;
        }

        // 2. Randomization: Shuffle top 100
        Collections.shuffle(candidates);

        // 3. Selection based on Premium status
        int countToPick = Boolean.TRUE.equals(user.getPremium()) ? 40 : 20;
        List<Map<String, Object>> selected = candidates.stream()
                .limit(countToPick)
                .collect(Collectors.toList());

        // 4. Clear and Push to Redis ZSet
        String redisKey = REDIS_KEY_PREFIX + userId;
        redisTemplate.delete(redisKey);

        for (Map<String, Object> entry : selected) {
            Long targetId = ((Number) entry.get("userId")).longValue();
            Double score = ((Number) entry.get("matchScore")).doubleValue();

            // Add Premium Boost if candidate is premium
            User targetUser = userRepository.findById(targetId).orElse(null);
            if (targetUser != null && Boolean.TRUE.equals(targetUser.getPremium())) {
                score += 20.0;
            }

            redisTemplate.opsForZSet().add(redisKey, targetId, score);
        }

        // Set TTL to 24 hours to prevent memory exhaustion
        redisTemplate.expire(redisKey, java.time.Duration.ofHours(24));

        log.info("Cached {} matches for user: {}", selected.size(), userId);
    }

    @Override
    public MatchResponseDTO.MatchListResponse getNewMatchesFromCache(Pageable pageable) {
        User currentUser = getCurrentUser();
        String redisKey = REDIS_KEY_PREFIX + currentUser.getId();

        // 1. Read IDs from Redis (Using Keyset-like pagination logic)
        // Note: For simplicity and since daily feed is small (20-40), we use revRange
        Set<Object> targetIdsObj = redisTemplate.opsForZSet().reverseRange(redisKey,
                pageable.getOffset(),
                pageable.getOffset() + pageable.getPageSize() - 1);

        if (targetIdsObj == null || targetIdsObj.isEmpty()) {
            return MatchResponseDTO.MatchListResponse.builder()
                    .matches(Collections.emptyList())
                    .totalCount(0)
                    .build();
        }

        List<Long> targetIds = targetIdsObj.stream()
                .map(obj -> Long.valueOf(obj.toString()))
                .collect(Collectors.toList());

        // 2. Fetch Profile DTOs using MySQL IN query
        List<Profile> profiles = profileRepository.findAllByUserIn(userRepository.findAllById(targetIds));

        // Mantain order as per Redis scores
        Map<Long, Profile> profileMap = profiles.stream()
                .collect(Collectors.toMap(p -> p.getUser().getId(), p -> p));

        List<MatchResponseDTO> matches = targetIds.stream()
                .filter(profileMap::containsKey)
                .map(id -> mapToDTO(profileMap.get(id), currentUser))
                .collect(Collectors.toList());

        Long totalCount = redisTemplate.opsForZSet().size(redisKey);

        return MatchResponseDTO.MatchListResponse.builder()
                .category("new")
                .title("New Matches")
                .matches(matches)
                .totalCount(totalCount != null ? totalCount.intValue() : 0)
                .page(pageable.getPageNumber())
                .size(pageable.getPageSize())
                .hasNext(pageable.getOffset() + pageable.getPageSize() < (totalCount != null ? totalCount : 0))
                .build();
    }

    private String getPreferredGender(String currentGender) {
        if ("Male".equalsIgnoreCase(currentGender))
            return "Female";
        if ("Female".equalsIgnoreCase(currentGender))
            return "Male";
        return null;
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
    }

    private MatchResponseDTO mapToDTO(Profile p, User viewer) {
        // Simplified mapping for production robustness
        return MatchResponseDTO.builder()
                .userId(p.getUser().getId())
                .user(UserBasicDto.builder()
                        .id(p.getUser().getId())
                        .fullName(p.getFullName())
                        .age(p.getAge())
                        .city(p.getCity())
                        .isPremium(Boolean.TRUE.equals(p.getUser().getPremium()))
                        .profilePhotoUrl(photoVisibilityService.getProfilePhoto(viewer, p.getUser()))
                        .build())
                .age(p.getAge())
                .city(p.getCity())
                .occupation(p.getOccupation())
                .isPremium(Boolean.TRUE.equals(p.getUser().getPremium()))
                .build();
    }
}
