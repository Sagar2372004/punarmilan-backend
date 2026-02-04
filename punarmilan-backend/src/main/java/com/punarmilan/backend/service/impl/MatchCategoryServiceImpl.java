package com.punarmilan.backend.service.impl;

import com.punarmilan.backend.dto.MatchCategoryDTO;

import com.punarmilan.backend.dto.MatchFilterDTO;
import com.punarmilan.backend.dto.MatchResponseDTO;
import com.punarmilan.backend.dto.UserBasicDto;
import com.punarmilan.backend.entity.*;
import com.punarmilan.backend.exception.BadRequestException;
import com.punarmilan.backend.exception.ResourceNotFoundException;
import com.punarmilan.backend.repository.*;
import com.punarmilan.backend.service.MatchCategoryService;
import com.punarmilan.backend.service.PhotoVisibilityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class MatchCategoryServiceImpl implements MatchCategoryService {

    private final MatchCategoryRepository matchCategoryRepository;
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final MatchRepository matchRepository;
    private final UserViewHistoryRepository viewHistoryRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final PhotoVisibilityService photoVisibilityService;
    private final ConnectionRequestRepository connectionRepository;

    // Pre-defined category slugs
    private static final String CATEGORY_NEW = "new";
    private static final String CATEGORY_TODAY = "today";
    private static final String CATEGORY_MY = "my";
    private static final String CATEGORY_NEAR = "near";
    private static final String CATEGORY_MORE = "more";

    @Override
    public List<MatchCategoryDTO> getAllCategories() {

        List<MatchCategory> categories = matchCategoryRepository.findByActiveTrueOrderBySortOrderAsc();
        Map<String, Integer> categoryCounts = getCategoryCounts();

        return categories.stream().map(category -> {
            Integer count = categoryCounts.getOrDefault(category.getSlug(), 0);
            return MatchCategoryDTO.builder()
                    .id(category.getId())
                    .name(category.getName())
                    .slug(category.getSlug())
                    .description(category.getDescription())
                    .icon(category.getIcon())
                    .color(category.getColor())
                    .count(count)
                    .isActive(category.isActive())
                    .showCount(category.isShowCount())
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    public MatchCategoryDTO getCategoryBySlug(String slug) {
        MatchCategory category = matchCategoryRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        Map<String, Integer> counts = getCategoryCounts();
        Integer count = counts.getOrDefault(slug, 0);

        return MatchCategoryDTO.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .description(category.getDescription())
                .icon(category.getIcon())
                .color(category.getColor())
                .count(count)
                .isActive(category.isActive())
                .showCount(category.isShowCount())
                .build();
    }

    @Override
    public MatchResponseDTO.MatchListResponse getMatchesByCategory(String category, Pageable pageable) {
        switch (category.toLowerCase()) {
            case CATEGORY_NEW:
                return getNewMatches(pageable);
            case CATEGORY_TODAY:
                return getTodaysMatches(pageable);
            case CATEGORY_MY:
                return getMyMatches(pageable);
            case CATEGORY_NEAR:
                return getNearMeMatches(pageable);
            case CATEGORY_MORE:
                return getMoreMatches(pageable);
            default:
                throw new BadRequestException("Invalid category: " + category);
        }
    }

    @Override
    public MatchResponseDTO.MatchListResponse getNewMatches(Pageable pageable) {
        User currentUser = getCurrentUser();
        Profile currentProfile = profileRepository.findByUser(currentUser)
                .orElseThrow(() -> new BadRequestException("Complete your profile first"));

        // Get users who joined recently (last 7 days) and match preferences
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        String preferredGender = getPreferredGender(currentProfile.getGender());

        // Get all users who joined recently - FIXED METHOD NAME
        List<User> allUsers = userRepository.findByCreatedAtAfterAndActiveTrue(weekAgo);

        // Filter based on gender preference
        List<User> filteredUsers = allUsers.stream()
                .filter(user -> !user.getId().equals(currentUser.getId()))
                .filter(user -> {
                    Profile userProfile = profileRepository.findByUser(user).orElse(null);
                    if (userProfile == null)
                        return false;

                    // Check gender compatibility
                    if (preferredGender != null && userProfile.getGender() != null) {
                        return preferredGender.equalsIgnoreCase(userProfile.getGender());
                    }
                    return true;
                })
                .filter(user -> !hasViewedUser(currentUser, user))
                .filter(user -> !hasLikedUser(currentUser, user))
                .collect(Collectors.toList());

        // Apply pagination
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filteredUsers.size());
        List<User> paginatedUsers = filteredUsers.subList(start, end);

        // Convert to DTOs
        List<MatchResponseDTO> matchDTOs = paginatedUsers.stream()
                .map(user -> mapToMatchResponseDTO(user, currentUser, "new"))
                .collect(Collectors.toList());

        // Get count (for display)
        int newMatchesCount = (int) filteredUsers.stream()
                .filter(user -> !hasViewedUser(currentUser, user))
                .count();

        return MatchResponseDTO.MatchListResponse.builder()
                .category(CATEGORY_NEW)
                .title("New Matches")
                .totalCount(newMatchesCount)
                .matches(matchDTOs)
                .page(pageable.getPageNumber())
                .size(pageable.getPageSize())
                .totalPages((int) Math.ceil((double) filteredUsers.size() / pageable.getPageSize()))
                .hasNext(end < filteredUsers.size())
                .hasPrevious(start > 0)
                .build();
    }

    @Override
    public MatchResponseDTO.MatchListResponse getTodaysMatches(Pageable pageable) {
        User currentUser = getCurrentUser();
        Profile currentProfile = profileRepository.findByUser(currentUser)
                .orElseThrow(() -> new BadRequestException("Complete your profile first"));

        LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        String preferredGender = getPreferredGender(currentProfile.getGender());

        // Get today's active users (active today) - FIXED METHOD NAME
        List<User> todaysUsers = userRepository.findByLastLoginAfterAndActiveTrue(todayStart);

        // Filter users
        List<User> filteredUsers = todaysUsers.stream()
                .filter(user -> !user.getId().equals(currentUser.getId()))
                .filter(user -> {
                    Profile userProfile = profileRepository.findByUser(user).orElse(null);
                    if (userProfile == null)
                        return false;

                    // Check gender compatibility
                    if (preferredGender != null && userProfile.getGender() != null) {
                        return preferredGender.equalsIgnoreCase(userProfile.getGender());
                    }
                    return true;
                })
                .filter(user -> !hasMatched(currentUser, user))
                .collect(Collectors.toList());

        // Apply pagination
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filteredUsers.size());
        List<User> paginatedUsers = filteredUsers.subList(start, end);

        // Convert to DTOs
        List<MatchResponseDTO> matchDTOs = paginatedUsers.stream()
                .map(user -> mapToMatchResponseDTO(user, currentUser, "today"))
                .collect(Collectors.toList());

        // Get today's match count
        long todaysMatchCount = matchRepository.findAll().stream()
                .filter(match -> {
                    // FIXED: Check if match is matched
                    return match.getStatus() == Match.MatchStatus.MATCHED &&
                            match.getMatchedAt() != null &&
                            match.getMatchedAt().isAfter(todayStart) &&
                            match.isParticipant(currentUser.getId());
                })
                .count();

        return MatchResponseDTO.MatchListResponse.builder()
                .category(CATEGORY_TODAY)
                .title("Today's Matches")
                .totalCount((int) todaysMatchCount)
                .matches(matchDTOs)
                .page(pageable.getPageNumber())
                .size(pageable.getPageSize())
                .totalPages((int) Math.ceil((double) filteredUsers.size() / pageable.getPageSize()))
                .hasNext(end < filteredUsers.size())
                .hasPrevious(start > 0)
                .build();
    }

    @Override
    public MatchResponseDTO.MatchListResponse getMyMatches(Pageable pageable) {
        User currentUser = getCurrentUser();
        Profile currentProfile = profileRepository.findByUser(currentUser)
                .orElseThrow(() -> new BadRequestException("Complete your profile first"));

        // Calculate target gender (Opposite Gender)
        String targetGender = getPreferredGender(currentProfile.getGender());

        // Fetches profiles of opposite gender from the database
        Page<Profile> targetProfiles = profileRepository.findByGenderIgnoreCase(targetGender, pageable);

        List<MatchResponseDTO> matches = targetProfiles.getContent().stream()
                .map(p -> mapToMatchResponseDTO(p.getUser(), currentUser, "my"))
                .collect(Collectors.toList());

        return MatchResponseDTO.MatchListResponse.builder()
                .category(CATEGORY_MY)
                .title("Opposite Gender Profiles")
                .matches(matches)
                .totalCount((int) targetProfiles.getTotalElements())
                .page(pageable.getPageNumber())
                .size(pageable.getPageSize())
                .totalPages(targetProfiles.getTotalPages())
                .hasNext(targetProfiles.hasNext())
                .hasPrevious(targetProfiles.hasPrevious())
                .build();
    }

    @Override
    public MatchResponseDTO.MatchListResponse getNearMeMatches(Pageable pageable) {
        User currentUser = getCurrentUser();
        Profile currentProfile = profileRepository.findByUser(currentUser)
                .orElseThrow(() -> new BadRequestException("Complete your profile first"));

        // Get current user's city
        String currentCity = currentProfile.getCity();
        if (currentCity == null || currentCity.trim().isEmpty()) {
            throw new BadRequestException("Please add your city to find matches near you");
        }

        // Find users in same city - FIXED METHOD NAME
        List<Profile> profilesInCity = profileRepository.findByCityIgnoreCaseAndUserActiveTrue(currentCity);

        // Filter users
        List<User> nearUsers = profilesInCity.stream()
                .map(Profile::getUser)
                .filter(user -> !user.getId().equals(currentUser.getId()))
                .filter(user -> {
                    Profile userProfile = profileRepository.findByUser(user).orElse(null);
                    return userProfile != null && isGenderCompatible(currentProfile, userProfile);
                })
                .filter(user -> !hasMatched(currentUser, user))
                .collect(Collectors.toList());

        // Apply pagination
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), nearUsers.size());
        List<User> paginatedUsers = nearUsers.subList(start, end);

        // Convert to DTOs
        List<MatchResponseDTO> matchDTOs = paginatedUsers.stream()
                .map(user -> mapToMatchResponseDTO(user, currentUser, "near"))
                .collect(Collectors.toList());

        return MatchResponseDTO.MatchListResponse.builder()
                .category(CATEGORY_NEAR)
                .title("Near Me")
                .totalCount(nearUsers.size())
                .matches(matchDTOs)
                .page(pageable.getPageNumber())
                .size(pageable.getPageSize())
                .totalPages((int) Math.ceil((double) nearUsers.size() / pageable.getPageSize()))
                .hasNext(end < nearUsers.size())
                .hasPrevious(start > 0)
                .build();
    }

    @Override
    public MatchResponseDTO.MatchListResponse getMoreMatches(Pageable pageable) {
        User currentUser = getCurrentUser();
        Profile currentProfile = profileRepository.findByUser(currentUser)
                .orElseThrow(() -> new BadRequestException("Complete your profile first"));

        // Get all active users excluding already interacted ones - FIXED METHOD NAME
        List<User> allUsers = userRepository.findByActiveTrue();

        String preferredGender = getPreferredGender(currentProfile.getGender());

        List<User> moreUsers = allUsers.stream()
                .filter(user -> !user.getId().equals(currentUser.getId()))
                .filter(user -> {
                    Profile userProfile = profileRepository.findByUser(user).orElse(null);
                    if (userProfile == null)
                        return false;

                    // Check gender compatibility
                    if (preferredGender != null && userProfile.getGender() != null) {
                        return preferredGender.equalsIgnoreCase(userProfile.getGender());
                    }
                    return true;
                })
                .filter(user -> !hasViewedUser(currentUser, user))
                .filter(user -> !hasLikedUser(currentUser, user))
                .filter(user -> !hasMatched(currentUser, user))
                .collect(Collectors.toList());

        // Apply pagination
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), moreUsers.size());
        List<User> paginatedUsers = moreUsers.subList(start, end);

        // Convert to DTOs
        List<MatchResponseDTO> matchDTOs = paginatedUsers.stream()
                .map(user -> mapToMatchResponseDTO(user, currentUser, "more"))
                .collect(Collectors.toList());

        return MatchResponseDTO.MatchListResponse.builder()
                .category(CATEGORY_MORE)
                .title("More Matches")
                .totalCount(moreUsers.size())
                .matches(matchDTOs)
                .page(pageable.getPageNumber())
                .size(pageable.getPageSize())
                .totalPages((int) Math.ceil((double) moreUsers.size() / pageable.getPageSize()))
                .hasNext(end < moreUsers.size())
                .hasPrevious(start > 0)
                .build();
    }

    @Override
    public MatchResponseDTO.MatchListResponse searchMatches(MatchFilterDTO filterDTO, Pageable pageable) {
        User currentUser = getCurrentUser();
        Profile currentProfile = profileRepository.findByUser(currentUser)
                .orElseThrow(() -> new BadRequestException("Complete your profile first"));

        // Apply filters to get users
        List<User> filteredUsers = applyFilters(currentUser, currentProfile, filterDTO);

        // Apply sorting
        filteredUsers = applySorting(filteredUsers, filterDTO.getSortBy(), filterDTO.getSortOrder());

        // Apply pagination
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filteredUsers.size());
        List<User> paginatedUsers = filteredUsers.subList(start, end);

        // Convert to DTOs
        List<MatchResponseDTO> matchDTOs = paginatedUsers.stream()
                .map(user -> mapToMatchResponseDTO(user, currentUser, "search"))
                .collect(Collectors.toList());

        return MatchResponseDTO.MatchListResponse.builder()
                .category("search")
                .title("Search Results")
                .totalCount(filteredUsers.size())
                .matches(matchDTOs)
                .page(pageable.getPageNumber())
                .size(pageable.getPageSize())
                .totalPages((int) Math.ceil((double) filteredUsers.size() / pageable.getPageSize()))
                .hasNext(end < filteredUsers.size())
                .hasPrevious(start > 0)
                .build();
    }

    @Override
    public MatchResponseDTO.MatchStatsResponse getMatchStats() {
        User currentUser = getCurrentUser();

        Map<String, Integer> categoryCounts = getCategoryCounts();

        // Calculate mutual likes
        long mutualLikes = matchRepository.findAll().stream()
                .filter(match -> match.isParticipant(currentUser.getId()))
                .filter(match -> {
                    // FIXED: Use hasUserLiked method
                    return match.hasUserLiked(currentUser.getId());
                })
                .filter(match -> {
                    User otherUser = match.getOtherUser(currentUser.getId());
                    return match.hasUserLiked(otherUser.getId());
                })
                .count();

        // Calculate unviewed matches
        long unviewedMatches = matchRepository.findConfirmedMatchesByUser(currentUser, Pageable.unpaged())
                .getContent().stream()
                .filter(match -> {
                    User otherUser = match.getOtherUser(currentUser.getId());
                    return !hasViewedUser(currentUser, otherUser);
                })
                .count();

        return MatchResponseDTO.MatchStatsResponse.builder()
                .newMatches(categoryCounts.getOrDefault(CATEGORY_NEW, 0))
                .todaysMatches(categoryCounts.getOrDefault(CATEGORY_TODAY, 0))
                .myMatches(categoryCounts.getOrDefault(CATEGORY_MY, 0))
                .nearMeMatches(categoryCounts.getOrDefault(CATEGORY_NEAR, 0))
                .moreMatches(categoryCounts.getOrDefault(CATEGORY_MORE, 0))
                .totalMatches(categoryCounts.values().stream().mapToInt(Integer::intValue).sum())
                .unviewedMatches((int) unviewedMatches)
                .mutualLikes((int) mutualLikes)
                .lastUpdated(LocalDateTime.now())
                .build();
    }

    @Override
    public Map<String, Integer> getCategoryCounts() {
        User currentUser = getCurrentUser();
        Profile currentProfile = profileRepository.findByUser(currentUser).orElse(null);

        if (currentProfile == null) {
            return Map.of(
                    CATEGORY_NEW, 0,
                    CATEGORY_TODAY, 0,
                    CATEGORY_MY, 0,
                    CATEGORY_NEAR, 0,
                    CATEGORY_MORE, 0);
        }

        Map<String, Integer> counts = new HashMap<>();

        // New Matches count (users joined in last 7 days, not viewed)
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        String preferredGender = getPreferredGender(currentProfile.getGender());

        long newCount = userRepository.findByCreatedAtAfterAndActiveTrue(weekAgo).stream()
                .filter(user -> !user.getId().equals(currentUser.getId()))
                .filter(user -> {
                    Profile userProfile = profileRepository.findByUser(user).orElse(null);
                    if (userProfile == null)
                        return false;

                    if (preferredGender != null && userProfile.getGender() != null) {
                        return preferredGender.equalsIgnoreCase(userProfile.getGender());
                    }
                    return true;
                })
                .filter(user -> !hasViewedUser(currentUser, user))
                .filter(user -> !hasLikedUser(currentUser, user))
                .count();
        counts.put(CATEGORY_NEW, (int) newCount);

        // Today's Matches count
        LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        long todayCount = matchRepository.findAll().stream()
                .filter(match -> {
                    // FIXED: Check match status
                    return match.getStatus() == Match.MatchStatus.MATCHED &&
                            match.getMatchedAt() != null &&
                            match.getMatchedAt().isAfter(todayStart) &&
                            match.isParticipant(currentUser.getId());
                })
                .count();
        counts.put(CATEGORY_TODAY, (int) todayCount);

        // My Matches count - FIXED METHOD
        long myCount = matchRepository.countConfirmedMatches(currentUser);
        counts.put(CATEGORY_MY, (int) Math.min(myCount, 9999)); // Cap at 9999+

        // Near Me count
        String currentCity = currentProfile.getCity();
        long nearCount = 0;
        if (currentCity != null && !currentCity.trim().isEmpty()) {
            nearCount = profileRepository.findByCityIgnoreCaseAndUserActiveTrue(currentCity).stream()
                    .map(Profile::getUser)
                    .filter(user -> !user.getId().equals(currentUser.getId()))
                    .filter(user -> {
                        Profile userProfile = profileRepository.findByUser(user).orElse(null);
                        return userProfile != null && isGenderCompatible(currentProfile, userProfile);
                    })
                    .filter(user -> !hasMatched(currentUser, user))
                    .count();
        }
        counts.put(CATEGORY_NEAR, (int) nearCount);

        // More Matches count
        long moreCount = userRepository.findByActiveTrue().stream()
                .filter(user -> !user.getId().equals(currentUser.getId()))
                .filter(user -> {
                    Profile userProfile = profileRepository.findByUser(user).orElse(null);
                    if (userProfile == null)
                        return false;

                    if (preferredGender != null && userProfile.getGender() != null) {
                        return preferredGender.equalsIgnoreCase(userProfile.getGender());
                    }
                    return true;
                })
                .filter(user -> !hasViewedUser(currentUser, user))
                .filter(user -> !hasLikedUser(currentUser, user))
                .filter(user -> !hasMatched(currentUser, user))
                .count();
        counts.put(CATEGORY_MORE, (int) moreCount);

        return counts;
    }

    @Override
    public void markAsViewed(Long userId) {
        User currentUser = getCurrentUser();
        User viewedUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Create or update view history
        UserViewHistory viewHistory = viewHistoryRepository.findByViewerAndViewedUser(currentUser, viewedUser)
                .orElse(UserViewHistory.builder()
                        .viewer(currentUser)
                        .viewedUser(viewedUser)
                        .viewedAt(LocalDateTime.now())
                        .profileViewed(true)
                        .build());

        viewHistory.setViewedAt(LocalDateTime.now());
        viewHistory.setProfileViewed(true);
        viewHistoryRepository.save(viewHistory);

        log.debug("User {} viewed profile of user {}", currentUser.getEmail(), viewedUser.getEmail());
    }

    @Override
    public void markAsLiked(Long userId) {
        User currentUser = getCurrentUser();
        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Find or create match - FIXED METHOD
        Match match = matchRepository.findMatchBetweenUsers(currentUser, targetUser)
                .orElseGet(() -> createNewMatch(currentUser, targetUser));

        // Like the user - FIXED METHOD
        match.likeUser(currentUser.getId(), false); // false = not super like

        // Save the match
        Match savedMatch = matchRepository.save(match);

        // Send notification via Event
        eventPublisher.publishEvent(new com.punarmilan.backend.event.NotificationEvent(
                this,
                targetUser,
                currentUser,
                "SHORTLISTED",
                getUserDisplayName(currentUser) + " liked your profile",
                savedMatch));

        log.info("User {} liked user {}", currentUser.getEmail(), targetUser.getEmail());
    }

    @Override
    public void skipUser(Long userId) {
        User currentUser = getCurrentUser();
        User skippedUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Mark as viewed to avoid showing again
        markAsViewed(userId);

        log.debug("User {} skipped user {}", currentUser.getEmail(), skippedUser.getEmail());
    }

    @Override
    public Page<MatchResponseDTO> getMatchSuggestions(Pageable pageable) {
        User currentUser = getCurrentUser();
        Profile currentProfile = profileRepository.findByUser(currentUser)
                .orElseThrow(() -> new BadRequestException("Complete your profile first"));

        // Get compatibility-based suggestions
        List<User> allUsers = userRepository.findByActiveTrue();
        String preferredGender = getPreferredGender(currentProfile.getGender());

        List<MatchResponseDTO> suggestions = allUsers.stream()
                .filter(user -> !user.getId().equals(currentUser.getId()))
                .filter(user -> {
                    Profile userProfile = profileRepository.findByUser(user).orElse(null);
                    if (userProfile == null)
                        return false;

                    if (preferredGender != null && userProfile.getGender() != null) {
                        return preferredGender.equalsIgnoreCase(userProfile.getGender());
                    }
                    return true;
                })
                .filter(user -> !hasViewedUser(currentUser, user))
                .filter(user -> !hasLikedUser(currentUser, user))
                .filter(user -> !hasMatched(currentUser, user))
                .sorted((u1, u2) -> {
                    Profile p1 = profileRepository.findByUser(u1).orElse(new Profile());
                    Profile p2 = profileRepository.findByUser(u2).orElse(new Profile());
                    int score1 = calculateCompatibilityScore(currentProfile, p1);
                    int score2 = calculateCompatibilityScore(currentProfile, p2);
                    return Integer.compare(score2, score1); // Descending
                })
                .map(user -> mapToMatchResponseDTO(user, currentUser, "suggestions"))
                .collect(Collectors.toList());

        // Apply pagination
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), suggestions.size());
        List<MatchResponseDTO> paginatedList = suggestions.subList(start, end);

        return new PageImpl<>(paginatedList, pageable, suggestions.size());
    }

    @Override
    public Page<MatchResponseDTO> getRecentlyViewed(Pageable pageable) {
        User currentUser = getCurrentUser();

        // FIXED: Use the correct method
        Page<User> viewedUsers = viewHistoryRepository.findRecentlyViewedUsers(currentUser, pageable);

        return viewedUsers.map(user -> mapToMatchResponseDTO(user, currentUser, "viewed"));
    }

    @Override
    public Page<MatchResponseDTO> getMutualLikes(Pageable pageable) {
        User currentUser = getCurrentUser();

        // Get matches where both users liked each other
        List<Match> mutualMatches = matchRepository.findAll().stream()
                .filter(match -> match.isParticipant(currentUser.getId()))
                .filter(match -> {
                    User otherUser = match.getOtherUser(currentUser.getId());
                    // FIXED: Use hasUserLiked method
                    return match.hasUserLiked(currentUser.getId()) &&
                            match.hasUserLiked(otherUser.getId());
                })
                .filter(match -> !match.isMatched()) // Not yet matched (pending mutual like)
                .collect(Collectors.toList());

        List<MatchResponseDTO> mutualLikes = mutualMatches.stream()
                .map(match -> {
                    User otherUser = match.getOtherUser(currentUser.getId());
                    return mapToMatchResponseDTO(otherUser, currentUser, "mutual");
                })
                .collect(Collectors.toList());

        // Apply pagination
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), mutualLikes.size());
        List<MatchResponseDTO> paginatedList = mutualLikes.subList(start, end);

        return new PageImpl<>(paginatedList, pageable, mutualLikes.size());
    }

    @Override
    @Transactional(readOnly = true)
    public MatchResponseDTO.MatchListResponse getNewRegistrations(Pageable pageable) {
        User currentUser = getCurrentUser();
        LocalDateTime twentyFourHoursAgo = LocalDateTime.now().minusHours(24);

        Page<Profile> newProfiles = profileRepository.findNewRegistrations(
                currentUser.getId(),
                twentyFourHoursAgo,
                pageable);

        List<MatchResponseDTO> matches = newProfiles.getContent().stream()
                .map(p -> mapToMatchResponseDTO(p.getUser(), currentUser, "new-registrations"))
                .collect(Collectors.toList());

        return MatchResponseDTO.MatchListResponse.builder()
                .category("new-registrations")
                .title("Just Joined (Last 24 Hours)")
                .matches(matches)
                .totalCount((int) newProfiles.getTotalElements())
                .page(pageable.getPageNumber())
                .size(pageable.getPageSize())
                .totalPages(newProfiles.getTotalPages())
                .hasNext(newProfiles.hasNext())
                .hasPrevious(newProfiles.hasPrevious())
                .build();
    }

    @Override
    public MatchCategoryDTO createCategory(MatchCategoryDTO.CategoryRequest request) {
        if (matchCategoryRepository.existsBySlug(request.getSlug())) {
            throw new BadRequestException("Category with this slug already exists");
        }

        MatchCategory category = MatchCategory.builder()
                .name(request.getName())
                .slug(request.getSlug())
                .description(request.getDescription())
                .icon(request.getIcon())
                .color(request.getColor())
                .sortOrder(request.getSortOrder())
                .showCount(request.isShowCount())
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        MatchCategory savedCategory = matchCategoryRepository.save(category);

        return MatchCategoryDTO.builder()
                .id(savedCategory.getId())
                .name(savedCategory.getName())
                .slug(savedCategory.getSlug())
                .description(savedCategory.getDescription())
                .icon(savedCategory.getIcon())
                .color(savedCategory.getColor())
                .isActive(savedCategory.isActive())
                .showCount(savedCategory.isShowCount())
                .build();
    }

    @Override
    public MatchCategoryDTO updateCategory(Long id, MatchCategoryDTO.CategoryRequest request) {
        MatchCategory category = matchCategoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        // Check if slug is being changed and if it's already taken
        if (!category.getSlug().equals(request.getSlug()) &&
                matchCategoryRepository.existsBySlug(request.getSlug())) {
            throw new BadRequestException("Category with this slug already exists");
        }

        category.setName(request.getName());
        category.setSlug(request.getSlug());
        category.setDescription(request.getDescription());
        category.setIcon(request.getIcon());
        category.setColor(request.getColor());
        category.setSortOrder(request.getSortOrder());
        category.setShowCount(request.isShowCount());
        category.setUpdatedAt(LocalDateTime.now());

        MatchCategory updatedCategory = matchCategoryRepository.save(category);

        return MatchCategoryDTO.builder()
                .id(updatedCategory.getId())
                .name(updatedCategory.getName())
                .slug(updatedCategory.getSlug())
                .description(updatedCategory.getDescription())
                .icon(updatedCategory.getIcon())
                .color(updatedCategory.getColor())
                .isActive(updatedCategory.isActive())
                .showCount(updatedCategory.isShowCount())
                .build();
    }

    @Override
    public void deleteCategory(Long id) {
        MatchCategory category = matchCategoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        matchCategoryRepository.delete(category);
    }

    // ==================== PRIVATE HELPER METHODS ====================

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private String getPreferredGender(String currentGender) {
        if ("Male".equalsIgnoreCase(currentGender)) {
            return "Female";
        } else if ("Female".equalsIgnoreCase(currentGender)) {
            return "Male";
        }
        return null;
    }

    private boolean isGenderCompatible(Profile profile1, Profile profile2) {
        String gender1 = profile1.getGender();
        String gender2 = profile2.getGender();

        if (gender1 == null || gender2 == null) {
            return true;
        }

        return ("Male".equalsIgnoreCase(gender1) && "Female".equalsIgnoreCase(gender2)) ||
                ("Female".equalsIgnoreCase(gender1) && "Male".equalsIgnoreCase(gender2));
    }

    private boolean hasViewedUser(User viewer, User viewedUser) {
        return viewHistoryRepository.findByViewerAndViewedUser(viewer, viewedUser).isPresent();
    }

    private boolean hasLikedUser(User user1, User user2) {
        Optional<Match> match = matchRepository.findMatchBetweenUsers(user1, user2);
        return match.map(m -> m.hasUserLiked(user1.getId())).orElse(false);
    }

    private boolean hasMatched(User user1, User user2) {
        return matchRepository.areUsersMatched(user1, user2);
    }

    private Match createNewMatch(User user1, User user2) {
        User firstUser = user1.getId() < user2.getId() ? user1 : user2;
        User secondUser = user1.getId() < user2.getId() ? user2 : user1;

        return Match.builder()
                .user1(firstUser)
                .user2(secondUser)
                .status(Match.MatchStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .active(true)
                .build();
    }

    private String getUserDisplayName(User user) {
        return profileRepository.findByUser(user)
                .map(Profile::getFullName)
                .orElse(user.getEmail());
    }

    private List<User> applyFilters(User currentUser, Profile currentProfile, MatchFilterDTO filterDTO) {
        List<User> allUsers = userRepository.findByActiveTrue();
        String preferredGender = filterDTO.getPreferredGender() != null ? filterDTO.getPreferredGender()
                : getPreferredGender(currentProfile.getGender());

        return allUsers.stream()
                .filter(user -> !user.getId().equals(currentUser.getId()))
                .filter(user -> {
                    Profile userProfile = profileRepository.findByUser(user).orElse(null);
                    if (userProfile == null)
                        return false;

                    // Gender filter
                    if (preferredGender != null && userProfile.getGender() != null) {
                        if (!preferredGender.equalsIgnoreCase(userProfile.getGender())) {
                            return false;
                        }
                    }

                    // Age filter
                    if (filterDTO.getMinAge() != null && userProfile.getAge() != null) {
                        if (userProfile.getAge() < filterDTO.getMinAge()) {
                            return false;
                        }
                    }
                    if (filterDTO.getMaxAge() != null && userProfile.getAge() != null) {
                        if (userProfile.getAge() > filterDTO.getMaxAge()) {
                            return false;
                        }
                    }

                    // City filter
                    if (filterDTO.getCity() != null && userProfile.getCity() != null) {
                        if (!filterDTO.getCity().equalsIgnoreCase(userProfile.getCity())) {
                            return false;
                        }
                    }

                    // Education filter
                    if (filterDTO.getEducationLevel() != null && userProfile.getEducationLevel() != null) {
                        if (!filterDTO.getEducationLevel().equalsIgnoreCase(userProfile.getEducationLevel())) {
                            return false;
                        }
                    }

                    // Occupation filter
                    if (filterDTO.getOccupation() != null && userProfile.getOccupation() != null) {
                        if (!filterDTO.getOccupation().equalsIgnoreCase(userProfile.getOccupation())) {
                            return false;
                        }
                    }

                    // workingWith filter - NEW
                    if (filterDTO.getWorkingWith() != null && userProfile.getWorkingWith() != null) {
                        if (!filterDTO.getWorkingWith().equalsIgnoreCase(userProfile.getWorkingWith())) {
                            return false;
                        }
                    }

                    // Verified filter
                    if (filterDTO.isOnlyVerified() && !userProfile.isVerified()) {
                        return false;
                    }

                    // Photos filter
                    if (filterDTO.isOnlyWithPhotos() &&
                            (userProfile.getProfilePhotoUrl() == null
                                    || userProfile.getProfilePhotoUrl().trim().isEmpty())) {
                        return false;
                    }

                    // Already liked filter
                    if (filterDTO.isExcludeAlreadyLiked() && hasLikedUser(currentUser, user)) {
                        return false;
                    }

                    // Viewed filter
                    if (filterDTO.isExcludeViewed() && hasViewedUser(currentUser, user)) {
                        return false;
                    }

                    // Matched filter
                    if (filterDTO.isExcludeMatched() && hasMatched(currentUser, user)) {
                        return false;
                    }

                    return true;
                })
                .collect(Collectors.toList());
    }

    private List<User> applySorting(List<User> users, String sortBy, String sortOrder) {
        if (sortBy == null) {
            return users;
        }

        Comparator<User> comparator = null;

        switch (sortBy.toLowerCase()) {
            case "compatibility":
                comparator = Comparator.comparing(user -> {
                    Profile userProfile = profileRepository.findByUser(user).orElse(new Profile());
                    Profile currentProfile = profileRepository.findByUser(getCurrentUser()).orElse(new Profile());
                    return calculateCompatibilityScore(currentProfile, userProfile);
                });
                break;

            case "recent":
                comparator = Comparator.comparing(user -> {
                    Profile userProfile = profileRepository.findByUser(user).orElse(new Profile());
                    return userProfile.getCreatedAt() != null ? userProfile.getCreatedAt() : LocalDateTime.MIN;
                });
                break;

            case "age":
                comparator = Comparator.comparing(user -> {
                    Profile userProfile = profileRepository.findByUser(user).orElse(new Profile());
                    return userProfile.getAge() != null ? userProfile.getAge() : 0;
                });
                break;

            case "distance":
                // Assuming we have location data
                comparator = Comparator.comparing(user -> {
                    // Implement distance calculation if location data is available
                    return 0.0;
                });
                break;

            default:
                return users;
        }

        if ("desc".equalsIgnoreCase(sortOrder)) {
            comparator = comparator.reversed();
        }

        return users.stream()
                .sorted(comparator)
                .collect(Collectors.toList());
    }

    private MatchResponseDTO mapToMatchResponseDTO(User user, User currentUser, String category) {
        Profile userProfile = profileRepository.findByUser(user).orElse(new Profile());
        Profile currentProfile = profileRepository.findByUser(currentUser).orElse(new Profile());

        boolean isViewed = hasViewedUser(currentUser, user);
        boolean isLiked = hasLikedUser(currentUser, user);
        boolean isMatched = hasMatched(currentUser, user);

        // Get match record if exists
        Optional<Match> matchRecord = matchRepository.findMatchBetweenUsers(currentUser, user);
        LocalDateTime matchedAt = matchRecord.map(Match::getMatchedAt).orElse(null);

        // Calculate online status (simplified)
        boolean isOnline = user.getLastLogin() != null &&
                user.getLastLogin().isAfter(LocalDateTime.now().minusMinutes(5));

        // Calculate distance (simplified - same city = 0km)
        Double distanceKm = null;
        String distanceText = null;
        if (userProfile.getCity() != null && currentProfile.getCity() != null) {
            if (userProfile.getCity().equalsIgnoreCase(currentProfile.getCity())) {
                distanceText = "Same City";
                distanceKm = 0.0;
            } else {
                distanceText = userProfile.getCity();
                distanceKm = 10.0; // Placeholder
            }
        }

        // ✅ Use UserBasicDto builder
        MatchResponseDTO response = MatchResponseDTO.builder()
                .userId(user.getId())
                .isLiked(isLiked)
                .isMatched(isMatched)
                .isViewed(isViewed)

                .user(UserBasicDto.builder() // ✅ Correct builder call
                        .id(user.getId())
                        .email(user.getEmail())
                        .fullName(userProfile.getFullName())
                        .gender(userProfile.getGender())
                        .age(userProfile.getAge())
                        .city(userProfile.getCity())
                        .profilePhotoUrl(photoVisibilityService.getProfilePhoto(currentUser, user))
                        .isVerified(userProfile.isVerified())
                        .occupation(userProfile.getOccupation())
                        .education(userProfile.getEducationLevel())
                        .religion(userProfile.getReligion())
                        .caste(userProfile.getCaste())
                        .isOnline(isOnline)
                        .isPremium(userProfile.isPremium())
                        .distanceKm(distanceKm)
                        .distanceText(distanceText)
                        .compatibilityScore(calculateCompatibilityScore(currentProfile, userProfile))
                        .compatibilityPercentage(calculateCompatibilityPercentage(currentProfile, userProfile))
                        .build())

                .primaryPhoto(photoVisibilityService.getProfilePhoto(currentUser, user))
                .photos(userProfile.getAllPhotos() != null ? userProfile.getAllPhotos().stream()
                        .map(url -> photoVisibilityService.getAlbumPhoto(currentUser, user, url))
                        .collect(Collectors.toList()) : Collections.emptyList())

                .age(userProfile.getAge())
                .city(userProfile.getCity())
                .occupation(userProfile.getOccupation())
                .education(userProfile.getEducationLevel())
                .height(userProfile.getHeight())
                .religion(userProfile.getReligion())
                .caste(userProfile.getCaste())

                .compatibilityScore(calculateCompatibilityScore(currentProfile, userProfile))
                .compatibilityPercentage(calculateCompatibilityPercentage(currentProfile, userProfile))
                .commonInterests(calculateCommonInterests(currentProfile, userProfile))

                .isOnline(isOnline)
                .isVerified(userProfile.isVerified())
                .isPremium(userProfile.isPremium())
                .lastActive(user.getLastLogin())

                .distanceKm(distanceKm)
                .distanceText(distanceText)

                .canLike(!isLiked && !isMatched)
                .canChat(isMatched)
                .canViewProfile(true)
                .canBlock(!isMatched)

                .createdAt(user.getCreatedAt())
                .matchedAt(matchedAt)

                .isNewToday(category.equals("today"))
                .isRecentlyActive(isOnline)
                .build();

        // ✅ Apply Astro Visibility Filtering
        applyAstroPrivacy(currentUser, userProfile, response);

        return response;
    }

    private void applyAstroPrivacy(User viewer, Profile ownerProfile, MatchResponseDTO response) {
        if (viewer.getId().equals(ownerProfile.getUser().getId()))
            return;

        // Admin override
        if (viewer.getRole() != null && viewer.getRole().equalsIgnoreCase("ROLE_ADMIN"))
            return;

        com.punarmilan.backend.entity.enums.AstroVisibility visibility = ownerProfile.getAstroVisibility();
        if (visibility == null)
            visibility = com.punarmilan.backend.entity.enums.AstroVisibility.ALL_MEMBERS;

        boolean canSee = false;
        if (visibility == com.punarmilan.backend.entity.enums.AstroVisibility.ALL_MEMBERS) {
            canSee = true;
        } else if (visibility == com.punarmilan.backend.entity.enums.AstroVisibility.CONTACTED_AND_ACCEPTED) {
            User owner = ownerProfile.getUser();
            boolean matched = matchRepository.areUsersMatched(viewer, owner);
            boolean connected = connectionRepository.areUsersConnected(viewer, owner);
            canSee = matched || connected;
        }

        if (!canSee) {
            if (response.getUser() != null) {
                response.getUser().setRashi(null);
                response.getUser().setManglikStatus(null);
            }
        } else {
            if (response.getUser() != null) {
                response.getUser().setRashi(ownerProfile.getRashi());
                response.getUser().setManglikStatus(
                        ownerProfile.getManglikStatus() != null ? ownerProfile.getManglikStatus().name() : null);
            }
        }
    }

    private Integer calculateCompatibilityScore(Profile profile1, Profile profile2) {
        int score = 0;

        // Age compatibility (±5 years = 30 points, ±10 years = 15 points)
        if (profile1.getAge() != null && profile2.getAge() != null) {
            int ageDiff = Math.abs(profile1.getAge() - profile2.getAge());
            if (ageDiff <= 5)
                score += 30;
            else if (ageDiff <= 10)
                score += 15;
        }

        // City compatibility (same city = 25 points)
        if (profile1.getCity() != null && profile2.getCity() != null &&
                profile1.getCity().equalsIgnoreCase(profile2.getCity())) {
            score += 25;
        }

        // Education compatibility (same level = 20 points)
        if (profile1.getEducationLevel() != null && profile2.getEducationLevel() != null &&
                profile1.getEducationLevel().equals(profile2.getEducationLevel())) {
            score += 20;
        }

        // Occupation compatibility (same field = 15 points)
        if (profile1.getOccupation() != null && profile2.getOccupation() != null &&
                profile1.getOccupation().equalsIgnoreCase(profile2.getOccupation())) {
            score += 15;
        }

        // workingWith compatibility (same career sector = 15 points) - NEW
        if (profile1.getWorkingWith() != null && profile2.getWorkingWith() != null &&
                profile1.getWorkingWith().equalsIgnoreCase(profile2.getWorkingWith())) {
            score += 15;
        }

        // Verified status (both verified = 10 points)
        if (profile1.isVerified() && profile2.isVerified()) {
            score += 10;
        }

        return Math.min(score, 100);
    }

    private String calculateCompatibilityPercentage(Profile profile1, Profile profile2) {
        int score = calculateCompatibilityScore(profile1, profile2);
        return score + "%";
    }

    private List<String> calculateCommonInterests(Profile profile1, Profile profile2) {
        List<String> commonInterests = new ArrayList<>();

        // Add common interests based on profile data
        if (profile1.getHobbies() != null && profile2.getHobbies() != null) {
            // Simple intersection check
            if (profile1.getHobbies().equalsIgnoreCase(profile2.getHobbies())) {
                commonInterests.add(profile1.getHobbies());
            }
        }

        if (profile1.getReligion() != null && profile2.getReligion() != null &&
                profile1.getReligion().equalsIgnoreCase(profile2.getReligion())) {
            commonInterests.add(profile1.getReligion());
        }

        return commonInterests;
    }
}