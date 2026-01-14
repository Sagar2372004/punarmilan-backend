package com.punarmilan.backend.service.impl;

import com.punarmilan.backend.dto.*;
import com.punarmilan.backend.entity.PartnerPreference;
import com.punarmilan.backend.entity.Profile;
import com.punarmilan.backend.entity.User;
import com.punarmilan.backend.exception.ResourceNotFoundException;
import com.punarmilan.backend.repository.PartnerPreferenceRepository;
import com.punarmilan.backend.repository.ProfileRepository;
import com.punarmilan.backend.service.PartnerPreferenceService;
import com.punarmilan.backend.service.ProfileService;
import com.punarmilan.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PartnerPreferenceServiceImpl implements PartnerPreferenceService {

    private final PartnerPreferenceRepository preferenceRepository;
    private final ProfileRepository profileRepository;
    private final ProfileService profileService;
    private final UserService userService;  // Add UserService dependency

    @Override
    public PartnerPreferenceResponseDto saveOrUpdatePreferences(PartnerPreferenceRequestDto requestDto) {
        User user = userService.getLoggedInUser();  // Use userService
        Profile profile = profileRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found"));

        PartnerPreference preference = preferenceRepository.findByProfile(profile)
                .orElse(PartnerPreference.builder()
                        .profile(profile)
                        .createdAt(LocalDateTime.now())
                        .build());

        updatePreferenceFromDto(preference, requestDto);
        preference.setUpdatedAt(LocalDateTime.now());
        
        PartnerPreference saved = preferenceRepository.save(preference);
        log.info("Partner preferences saved for user: {}", user.getEmail());
        
        return mapToResponse(saved);
    }

    @Override
    public PartnerPreferenceResponseDto getMyPreferences() {
        User user = userService.getLoggedInUser();  // Use userService
        Profile profile = profileRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found"));

        PartnerPreference preference = preferenceRepository.findByProfile(profile)
                .orElseThrow(() -> new ResourceNotFoundException("Partner preferences not found"));

        return mapToResponse(preference);
    }

    @Override
    public List<MatchResultDto> findMatches() {
        User user = userService.getLoggedInUser();  // Use userService
        Profile myProfile = profileRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found"));

        PartnerPreference myPreference = preferenceRepository.findByProfile(myProfile)
                .orElseThrow(() -> new ResourceNotFoundException("Partner preferences not found"));

        // Get all eligible profiles
        List<Profile> eligibleProfiles = getEligibleProfiles(myProfile, myPreference);
        
        // Calculate match scores and filter
        List<MatchResultDto> matches = eligibleProfiles.stream()
                .map(profile -> calculateMatchScore(myPreference, profile))
                .filter(result -> result.getMatchScore() >= getThreshold(myPreference))
                .sorted(Comparator.comparing(MatchResultDto::getMatchScore).reversed())
                .limit(50) // Limit to top 50 matches
                .collect(Collectors.toList());

        log.info("Found {} matches for user: {}", matches.size(), user.getEmail());
        return matches;
    }

    @Override
    public MatchResultDto calculateMatchScore(PartnerPreference preference, Profile profile) {
        Map<String, Boolean> criteriaMatches = new HashMap<>();
        List<String> matchReasons = new ArrayList<>();
        int totalScore = 0;
        int maxPossibleScore = 0;

        // Age match (15 points)
        if (preference.getMinAge() != null && preference.getMaxAge() != null) {
            maxPossibleScore += 15;
            if (profile.getAge() != null && 
                profile.getAge() >= preference.getMinAge() && 
                profile.getAge() <= preference.getMaxAge()) {
                totalScore += 15;
                criteriaMatches.put("age", true);
                matchReasons.add("Age matches your preference");
            }
        }

        // Height match (10 points)
        if (preference.getMinHeight() != null && preference.getMaxHeight() != null) {
            maxPossibleScore += 10;
            if (profile.getHeight() != null && 
                isHeightInRange(profile.getHeight(), preference.getMinHeight(), preference.getMaxHeight())) {
                totalScore += 10;
                criteriaMatches.put("height", true);
                matchReasons.add("Height matches your preference");
            }
        }

        // Religion match (20 points)
        if (preference.getPreferredReligion() != null) {
            maxPossibleScore += 20;
            if (profile.getReligion() != null && 
                preference.getPreferredReligion().equalsIgnoreCase(profile.getReligion())) {
                totalScore += 20;
                criteriaMatches.put("religion", true);
                matchReasons.add("Religion matches");
            }
        }

        // Location match (15 points)
        if (preference.getPreferredCity() != null) {
            maxPossibleScore += 15;
            if (profile.getCity() != null && 
                preference.getPreferredCity().equalsIgnoreCase(profile.getCity())) {
                totalScore += 15;
                criteriaMatches.put("location", true);
                matchReasons.add("Same city preference");
            }
        }

        // Education match (15 points)
        if (preference.getMinEducationLevel() != null) {
            maxPossibleScore += 15;
            if (profile.getEducationLevel() != null && 
                isEducationLevelSufficient(profile.getEducationLevel(), preference.getMinEducationLevel())) {
                totalScore += 15;
                criteriaMatches.put("education", true);
                matchReasons.add("Education level meets criteria");
            }
        }

        // Lifestyle match (15 points)
        if (preference.getPreferredDiet() != null && 
            !preference.getPreferredDiet().equals("No Preference")) {
            maxPossibleScore += 15;
            if (profile.getDiet() != null && 
                preference.getPreferredDiet().equalsIgnoreCase(profile.getDiet())) {
                totalScore += 15;
                criteriaMatches.put("lifestyle", true);
                matchReasons.add("Diet preference matches");
            }
        }

        // Marital status match (10 points)
        if (preference.getMaritalStatus() != null && 
            !preference.getMaritalStatus().equals("No Preference")) {
            maxPossibleScore += 10;
            if (profile.getMaritalStatus() != null && 
                preference.getMaritalStatus().equalsIgnoreCase(profile.getMaritalStatus())) {
                totalScore += 10;
                criteriaMatches.put("marital", true);
                matchReasons.add("Marital status matches");
            }
        }

        // Calculate percentage
        int matchPercentage = maxPossibleScore > 0 ? (totalScore * 100) / maxPossibleScore : 0;
        
        String reason = matchReasons.isEmpty() ? "Basic compatibility found" : 
                       String.join(", ", matchReasons.subList(0, Math.min(3, matchReasons.size())));

        return MatchResultDto.builder()
                .profile(convertToProfileResponse(profile))
                .matchScore(matchPercentage)
                .matchPercentage(matchPercentage + "%")
                .matchReason(reason)
                .isPremiumMatch(matchPercentage >= 80)
                .build();
    }

    @Override
    public Map<String, Object> getMatchStats() {
        User user = userService.getLoggedInUser();  // Use userService
        Profile profile = profileRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found"));

        PartnerPreference preference = preferenceRepository.findByProfile(profile).orElse(null);
        List<MatchResultDto> matches = preference != null ? findMatches() : new ArrayList<>();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalPotentialMatches", matches.size());
        stats.put("premiumMatches", matches.stream().filter(m -> m.getMatchScore() >= 80).count());
        stats.put("goodMatches", matches.stream().filter(m -> m.getMatchScore() >= 60 && m.getMatchScore() < 80).count());
        stats.put("averageMatches", matches.stream().filter(m -> m.getMatchScore() < 60).count());
        stats.put("preferencesSet", preference != null);
        stats.put("lastUpdated", preference != null ? preference.getUpdatedAt() : null);
        stats.put("matchSettings", preference != null ? mapToResponse(preference) : "No preferences set");

        return stats;
    }

    @Override
    public PartnerPreferenceRequestDto getSuggestedPreferences() {
        User user = userService.getLoggedInUser();  // Use userService
        Profile profile = profileRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found"));

        // Auto-suggest preferences based on user's own profile
        return PartnerPreferenceRequestDto.builder()
                .minAge(profile.getAge() != null ? profile.getAge() - 5 : 25)
                .maxAge(profile.getAge() != null ? profile.getAge() + 5 : 35)
                .preferredReligion(profile.getReligion())
                .preferredCaste(profile.getCaste())
                .preferredCity(profile.getCity())
                .preferredState(profile.getState())
                .preferredDiet(profile.getDiet())
                .maritalStatus("Single")
                .showVerifiedOnly(true)
                .enableAutoMatch(true)
                .matchScoreThreshold(70)
                .build();
    }

    @Override
    public void resetPreferences() {
        User user = userService.getLoggedInUser();  // Use userService
        Profile profile = profileRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found"));

        preferenceRepository.deleteByProfile(profile);
        log.info("Partner preferences reset for user: {}", user.getEmail());
    }

    @Override
    public List<MatchResultDto> findMatchesWithPagination(int page, int size) {
        List<MatchResultDto> allMatches = findMatches();
        
        int start = Math.min(page * size, allMatches.size());
        int end = Math.min((page + 1) * size, allMatches.size());
        
        return allMatches.subList(start, end);
    }

    @Override
    public List<MatchResultDto> getDailyMatches() {
        // Return top 10 matches for daily viewing
        List<MatchResultDto> allMatches = findMatches();
        return allMatches.stream()
                .limit(10)
                .collect(Collectors.toList());
    }

    @Override
    public MatchResultDto checkCompatibility(Long otherProfileId) {
        User user = userService.getLoggedInUser();  // Use userService
        Profile myProfile = profileRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Your profile not found"));

        Profile otherProfile = profileRepository.findById(otherProfileId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found with id: " + otherProfileId));

        PartnerPreference myPreference = preferenceRepository.findByProfile(myProfile)
                .orElseThrow(() -> new ResourceNotFoundException("Please set your partner preferences first"));

        return calculateMatchScore(myPreference, otherProfile);
    }

    // ================= PRIVATE HELPER METHODS =================

    private List<Profile> getEligibleProfiles(Profile myProfile, PartnerPreference preference) {
        String currentUserGender = myProfile.getGender();
        
        return profileRepository.findByProfileCompleteTrue()
                .stream()
                .filter(p -> !p.getId().equals(myProfile.getId())) // Exclude self
                
                // 🔴 IMPORTANT: Add opposite gender filtering
                .filter(p -> {
                    if ("Male".equalsIgnoreCase(currentUserGender)) {
                        return "Female".equalsIgnoreCase(p.getGender());
                    } else if ("Female".equalsIgnoreCase(currentUserGender)) {
                        return "Male".equalsIgnoreCase(p.getGender());
                    }
                    return true;
                })
                
                .filter(p -> p.getVerificationStatus() == Profile.VerificationStatus.VERIFIED)
                .filter(p -> preference.getShowVerifiedOnly() == null || 
                           !preference.getShowVerifiedOnly() || 
                           p.getVerificationStatus() == Profile.VerificationStatus.VERIFIED)
                .filter(p -> preference.getPreferredReligion() == null || 
                           preference.getPreferredReligion().equals(p.getReligion()))
                .collect(Collectors.toList());
    }
    
    private int getThreshold(PartnerPreference preference) {
        return preference.getMatchScoreThreshold() != null ? 
               preference.getMatchScoreThreshold() : 60;
    }

    private void updatePreferenceFromDto(PartnerPreference preference, PartnerPreferenceRequestDto dto) {
        preference.setMinAge(dto.getMinAge());
        preference.setMaxAge(dto.getMaxAge());
        preference.setMinHeight(dto.getMinHeight());
        preference.setMaxHeight(dto.getMaxHeight());
        preference.setPreferredReligion(dto.getPreferredReligion());
        preference.setPreferredCaste(dto.getPreferredCaste());
        preference.setPreferredSubCaste(dto.getPreferredSubCaste());
        preference.setMinEducationLevel(dto.getMinEducationLevel());
        preference.setPreferredCity(dto.getPreferredCity());
        preference.setPreferredState(dto.getPreferredState());
        preference.setPreferredDiet(dto.getPreferredDiet());
        preference.setDrinkingHabit(dto.getDrinkingHabit());
        preference.setSmokingHabit(dto.getSmokingHabit());
        preference.setMaritalStatus(dto.getMaritalStatus());
        preference.setOccupation(dto.getOccupation());
        preference.setMinAnnualIncome(dto.getMinAnnualIncome());
        preference.setPreferWorkingProfessional(dto.getPreferWorkingProfessional());
        preference.setPreferNri(dto.getPreferNri());
        preference.setShowVerifiedOnly(dto.getShowVerifiedOnly());
        preference.setEnableAutoMatch(dto.getEnableAutoMatch());
        preference.setMatchScoreThreshold(dto.getMatchScoreThreshold());
    }

    private PartnerPreferenceResponseDto mapToResponse(PartnerPreference preference) {
        return PartnerPreferenceResponseDto.builder()
                .id(preference.getId())
                .profileId(preference.getProfile().getId())
                .minAge(preference.getMinAge())
                .maxAge(preference.getMaxAge())
                .minHeight(preference.getMinHeight())
                .maxHeight(preference.getMaxHeight())
                .preferredReligion(preference.getPreferredReligion())
                .preferredCaste(preference.getPreferredCaste())
                .preferredSubCaste(preference.getPreferredSubCaste())
                .minEducationLevel(preference.getMinEducationLevel())
                .preferredCity(preference.getPreferredCity())
                .preferredState(preference.getPreferredState())
                .preferredDiet(preference.getPreferredDiet())
                .drinkingHabit(preference.getDrinkingHabit())
                .smokingHabit(preference.getSmokingHabit())
                .maritalStatus(preference.getMaritalStatus())
                .occupation(preference.getOccupation())
                .minAnnualIncome(preference.getMinAnnualIncome())
                .preferWorkingProfessional(preference.getPreferWorkingProfessional())
                .preferNri(preference.getPreferNri())
                .showVerifiedOnly(preference.getShowVerifiedOnly())
                .enableAutoMatch(preference.getEnableAutoMatch())
                .matchScoreThreshold(preference.getMatchScoreThreshold())
                .build();
    }

    private ProfileResponseDto convertToProfileResponse(Profile profile) {
        // Use ProfileService's mapToResponse method if available
        // If not, create a basic response
        return ProfileResponseDto.builder()
                .id(profile.getId())
                .fullName(profile.getFullName())
                .age(profile.getAge())
                .gender(profile.getGender())
                .height(profile.getHeight())
                .city(profile.getCity())
                .state(profile.getState())
                .religion(profile.getReligion())
                .occupation(profile.getOccupation())
                .profilePhotoUrl(profile.getProfilePhotoUrl())
                .build();
    }

    /**
     * Fixed method to handle height comparison when profile height is Integer (inches)
     * and preference heights are String (format like "5'8\"")
     */
    private boolean isHeightInRange(Integer heightInches, String minHeightStr, String maxHeightStr) {
        // Handle null cases
        if (heightInches == null || minHeightStr == null || maxHeightStr == null) {
            return true; // If any value is null, consider it a match
        }
        
        try {
            // Convert preference height strings to inches
            double minHeightInches = convertHeightToInches(minHeightStr);
            double maxHeightInches = convertHeightToInches(maxHeightStr);
            
            // Compare integer height with converted values
            return heightInches >= minHeightInches && heightInches <= maxHeightInches;
        } catch (Exception e) {
            log.warn("Error comparing heights: profileHeight={}, min={}, max={}", 
                    heightInches, minHeightStr, maxHeightStr, e);
            return false;
        }
    }
    
    private double convertHeightToInches(String height) {
        // Convert "5'8\"" to inches: 5*12 + 8 = 68
        if (height == null || height.trim().isEmpty()) {
            return 0;
        }
        
        try {
            // Remove quotes and split by feet symbol
            String cleanHeight = height.replace("\"", "").trim();
            String[] parts = cleanHeight.split("'");
            
            if (parts.length == 2) {
                int feet = Integer.parseInt(parts[0].trim());
                int inches = parts[1].trim().isEmpty() ? 0 : Integer.parseInt(parts[1].trim());
                return feet * 12 + inches;
            } else if (parts.length == 1) {
                // Handle case where only feet are provided (e.g., "5'")
                int feet = Integer.parseInt(parts[0].trim());
                return feet * 12;
            }
            return 0;
        } catch (NumberFormatException e) {
            log.warn("Invalid height format: {}", height);
            return 0;
        } catch (Exception e) {
            log.warn("Error converting height: {}", height, e);
            return 0;
        }
    }

    private boolean isEducationLevelSufficient(String candidateLevel, String requiredLevel) {
        // Simple implementation - you can enhance this
        if (candidateLevel == null || requiredLevel == null) return true;
        
        // Define education hierarchy
        Map<String, Integer> educationRank = Map.of(
            "Post Graduate", 5,
            "Graduate", 4,
            "Diploma", 3,
            "12th", 2,
            "10th", 1
        );
        
        Integer candidateRank = educationRank.getOrDefault(candidateLevel, 0);
        Integer requiredRank = educationRank.getOrDefault(requiredLevel, 0);
        
        return candidateRank >= requiredRank;
    }
}