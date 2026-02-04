package com.punarmilan.backend.service.impl;

import com.punarmilan.backend.dto.ProfileRequestDto;
import com.punarmilan.backend.dto.ProfileResponseDto;
import com.punarmilan.backend.entity.Profile;
import com.punarmilan.backend.entity.User;
import com.punarmilan.backend.exception.BadRequestException;
import com.punarmilan.backend.exception.ResourceNotFoundException;
import com.punarmilan.backend.repository.ProfileRepository;
import com.punarmilan.backend.repository.UserRepository;
import com.punarmilan.backend.service.FileStorageService;
import com.punarmilan.backend.service.NotificationService;
import com.punarmilan.backend.service.PhotoVisibilityService;
import com.punarmilan.backend.service.ProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import com.punarmilan.backend.entity.enums.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ProfileServiceImpl implements ProfileService {

    private final ProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final NotificationService notificationService;
    private final PhotoVisibilityService photoVisibilityService;
    private final com.punarmilan.backend.repository.MatchRepository matchRepository;
    private final com.punarmilan.backend.repository.ConnectionRequestRepository connectionRepository;

    // 🔐 Fetch logged-in user from JWT
    public User getLoggedInUser() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    // 🔹 CREATE / UPDATE PROFILE
    @Override
    public ProfileResponseDto saveOrUpdateProfile(ProfileRequestDto dto) {
        User user = getLoggedInUser();

        // 1. Fetch existing profile or initialize a new one (preserving save+update
        // logic)
        Profile profile = profileRepository.findByUser(user)
                .orElseGet(() -> {
                    log.info("Creating new profile for user: {}", user.getEmail());
                    return Profile.builder()
                            .user(user)
                            .createdAt(LocalDateTime.now())
                            .verificationStatus(Profile.VerificationStatus.UNVERIFIED) // Default for new
                            .build();
                });

        // 2. Mandatory Validations (only if it's a new profile or field is provided)
        validateProfileData(dto, profile);

        // 3. Partial Update Strategy: Only update if DTO field is NOT NULL

        // Personal Details
        updateIfPresent(dto.getFullName(), profile::setFullName);
        updateIfPresent(dto.getGender(), profile::setGender);
        updateIfPresent(dto.getDateOfBirth(), profile::setDateOfBirth);
        updateIfPresent(dto.getHeight(), profile::setHeight);
        updateIfPresent(dto.getWeight(), profile::setWeight);
        updateIfPresent(dto.getMaritalStatus(), profile::setMaritalStatus);
        updateIfPresent(dto.getMotherTongue(), profile::setMotherTongue);

        // Religion & Caste
        updateIfPresent(dto.getReligion(), profile::setReligion);
        updateIfPresent(dto.getCaste(), profile::setCaste);
        updateIfPresent(dto.getSubCaste(), profile::setSubCaste);
        updateIfPresent(dto.getGotra(), profile::setGotra);

        // Education & Career
        updateIfPresent(dto.getEducationLevel(), profile::setEducationLevel);
        updateIfPresent(dto.getEducationField(), profile::setEducationField);
        updateIfPresent(dto.getCollege(), profile::setCollege);
        updateIfPresent(dto.getOccupation(), profile::setOccupation);
        updateIfPresent(dto.getCompany(), profile::setCompany);
        updateIfPresent(dto.getWorkingWith(), profile::setWorkingWith);
        updateIfPresent(dto.getAnnualIncome(), profile::setAnnualIncome);
        updateIfPresent(dto.getWorkingCity(), profile::setWorkingCity);
        updateIfPresent(dto.getGrewUpIn(), profile::setGrewUpIn);
        updateIfPresent(dto.getZipCode(), profile::setZipCode);
        updateIfPresent(dto.getResidencyStatus(), profile::setResidencyStatus);

        // Lifestyle & Location
        updateIfPresent(dto.getDiet(), profile::setDiet);
        updateIfPresent(dto.getBloodGroup(), profile::setBloodGroup);
        updateIfPresent(dto.getHealthInformation(), profile::setHealthInformation);
        updateIfPresent(dto.getDisability(), profile::setDisability);
        updateIfPresent(dto.getDrinkingHabit(), profile::setDrinkingHabit);
        updateIfPresent(dto.getSmokingHabit(), profile::setSmokingHabit);
        updateIfPresent(dto.getCountry(), profile::setCountry);
        updateIfPresent(dto.getState(), profile::setState);
        updateIfPresent(dto.getCity(), profile::setCity);
        updateIfPresent(dto.getAddress(), profile::setAddress);

        // Family Details
        updateIfPresent(dto.getFatherStatus(), profile::setFatherStatus);
        updateIfPresent(dto.getMotherStatus(), profile::setMotherStatus);
        updateIfPresent(dto.getBrothersCount(), profile::setBrothersCount);
        updateIfPresent(dto.getSistersCount(), profile::setSistersCount);
        updateIfPresent(dto.getFamilyFinancialStatus(), profile::setFamilyFinancialStatus);
        updateIfPresent(dto.getFamilyLocation(), profile::setFamilyLocation);

        // About & Settings
        updateIfPresent(dto.getAboutMe(), profile::setAboutMe);
        updateIfPresent(dto.getHobbies(), profile::setHobbies);
        updateIfPresent(dto.getProfileCreatedBy(), profile::setProfileCreatedBy);
        updateIfPresent(dto.getProfileVisibility(), profile::setProfileVisibility);
        updateIfPresent(dto.getContactDisplayStatus(), profile::setContactDisplayStatus);

        // Safe Enum Parsing (PATCH style)
        parseEnum(dto.getProfilePhotoVisibility(), ProfilePhotoVisibility.class, profile::setProfilePhotoVisibility);
        parseEnum(dto.getAlbumPhotoVisibility(), AlbumPhotoVisibility.class, profile::setAlbumPhotoVisibility);
        parseEnum(dto.getManglikStatus(), ManglikStatus.class, profile::setManglikStatus);
        parseEnum(dto.getAstroVisibility(), AstroVisibility.class, profile::setAstroVisibility);

        // Photos
        updateIfPresent(dto.getProfilePhotoUrl(), profile::setProfilePhotoUrl);
        updateIfPresent(dto.getPhotoUrl2(), profile::setPhotoUrl2);
        updateIfPresent(dto.getPhotoUrl3(), profile::setPhotoUrl3);
        updateIfPresent(dto.getPhotoUrl4(), profile::setPhotoUrl4);
        updateIfPresent(dto.getPhotoUrl5(), profile::setPhotoUrl5);
        updateIfPresent(dto.getPhotoUrl6(), profile::setPhotoUrl6);

        // Astro Details
        updateIfPresent(dto.getTimeOfBirth(), profile::setTimeOfBirth);
        updateIfPresent(dto.getPlaceOfBirth(), profile::setPlaceOfBirth);
        updateIfPresent(dto.getNakshatra(), profile::setNakshatra);
        updateIfPresent(dto.getRashi(), profile::setRashi);

        // 4. Verification Fields
        if (dto.getIdProofUrl() != null && !dto.getIdProofUrl().isEmpty()) {
            profile.setIdProofUrl(dto.getIdProofUrl());
            updateIfPresent(dto.getIdProofType(), profile::setIdProofType);
            updateIfPresent(dto.getIdProofNumber(), profile::setIdProofNumber);
            profile.setVerificationStatus(Profile.VerificationStatus.PENDING);
            log.info("Verification requested by user: {}", user.getEmail());
        }

        // 5. Post-Update Housekeeping
        updatePhotoCount(profile);

        boolean wasComplete = profile.getProfileComplete() != null && profile.getProfileComplete();
        boolean isNowComplete = checkIfProfileComplete(profile);
        profile.setProfileComplete(isNowComplete);

        profile.setUpdatedAt(LocalDateTime.now());

        Profile savedProfile = profileRepository.save(profile);
        log.info("Profile saved/updated for user: {}, Complete: {}", user.getEmail(), isNowComplete);

        if (!wasComplete && isNowComplete) {
            notificationService.sendProfileCompletionNotification(user);
        }
        return mapToResponse(savedProfile);
    }

    /**
     * Utility: Update field only if the new value is present.
     * Prevents overwriting valid data with null.
     */
    private <T> void updateIfPresent(T newValue, Consumer<T> setter) {
        Optional.ofNullable(newValue).ifPresent(setter);
    }

    /**
     * Utility: Safe Enum Parsing for PATCH updates.
     */
    private <E extends Enum<E>> void parseEnum(String value, Class<E> enumClass, Consumer<E> setter) {
        if (value != null && !value.isEmpty()) {
            try {
                setter.accept(Enum.valueOf(enumClass, value.toUpperCase()));
            } catch (IllegalArgumentException e) {
                log.warn("Invalid enum value '{}' for {}", value, enumClass.getSimpleName());
            }
        }
    }

    /**
     * Centralized validation logic.
     */
    private void validateProfileData(ProfileRequestDto dto, Profile existing) {
        // Gender validation (only if provided)
        if (dto.getGender() != null) {
            List<String> validGenders = Arrays.asList("Male", "Female", "Other", "Prefer not to say");
            boolean isValidGender = validGenders.stream()
                    .anyMatch(valid -> valid.equalsIgnoreCase(dto.getGender()));

            if (!isValidGender) {
                throw new BadRequestException("Gender must be one of: " + String.join(", ", validGenders));
            }
        }

        // Mandatory fields for NEW profiles
        if (existing.getId() == null) {
            if (dto.getGender() == null || dto.getGender().trim().isEmpty()) {
                throw new BadRequestException("Gender is required for new profiles");
            }
            if (dto.getFullName() == null || dto.getFullName().trim().isEmpty()) {
                throw new BadRequestException("Full Name is required for new profiles");
            }
        }
    }

    private boolean checkIfProfileComplete(Profile p) {
        return p.getFullName() != null && !p.getFullName().trim().isEmpty() &&
                p.getGender() != null && !p.getGender().trim().isEmpty() &&
                p.getDateOfBirth() != null &&
                p.getReligion() != null && !p.getReligion().trim().isEmpty() &&
                p.getCity() != null && !p.getCity().trim().isEmpty() &&
                p.getProfilePhotoUrl() != null && !p.getProfilePhotoUrl().trim().isEmpty();
    }

    // 🔹 GET MY PROFILE
    @Override
    public ProfileResponseDto getMyProfile() {
        User user = getLoggedInUser();
        Profile profile = profileRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found"));

        return mapToResponse(profile);
    }

    @Override
    public ProfileResponseDto getProfileByPublicId(String profileId) {
        User user = userRepository.findByProfileId(profileId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found with ID: " + profileId));

        Profile profile = profileRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Profile data not found for user ID: " + profileId));

        return mapToResponse(profile);
    }

    @Override
    public List<ProfileResponseDto> searchProfiles(String gender, String city, String religion) {
        // Get current user's profile and gender
        User currentUser = getLoggedInUser();
        Profile currentUserProfile = profileRepository.findByUser(currentUser)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found"));

        String currentUserGender = currentUserProfile.getGender();

        return profileRepository.findByProfileCompleteTrue()
                .stream()
                .filter(p -> p.getVerificationStatus() == Profile.VerificationStatus.VERIFIED)

                // 🔴 IMPORTANT: Add gender filtering based on current user
                .filter(p -> {
                    if ("Male".equalsIgnoreCase(currentUserGender)) {
                        return "Female".equalsIgnoreCase(p.getGender());
                    } else if ("Female".equalsIgnoreCase(currentUserGender)) {
                        return "Male".equalsIgnoreCase(p.getGender());
                    }
                    return true; // Other genders (if any)
                })

                // Exclude current user
                .filter(p -> !p.getId().equals(currentUserProfile.getId()))

                // Other filters from parameters
                .filter(p -> gender == null || p.getGender().equalsIgnoreCase(gender))
                .filter(p -> city == null || p.getCity().equalsIgnoreCase(city))
                .filter(p -> religion == null || p.getReligion().equalsIgnoreCase(religion))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProfileResponseDto> getAllProfiles() {
        return profileRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    // ================= PHOTO METHODS =================

    @Override
    public String uploadProfilePhoto(MultipartFile file, int photoIndex) {
        // Validate photo index
        if (photoIndex < 0 || photoIndex > 5) {
            throw new BadRequestException("Photo index must be between 0 and 5");
        }

        // Validate file
        validatePhotoFile(file);

        // Get user's profile
        User user = getLoggedInUser();
        Profile profile = profileRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found. Please create a profile first."));

        // Store file and get URL
        String photoUrl = fileStorageService.storeFile(file);

        // Update profile with photo URL based on index
        updatePhotoUrlByIndex(profile, photoIndex, photoUrl);

        // Update photo count and save
        updatePhotoCount(profile);
        profile.setUpdatedAt(LocalDateTime.now());
        profileRepository.save(profile);

        log.info("Photo uploaded for user: {}, index: {}, url: {}", user.getEmail(), photoIndex, photoUrl);
        return photoUrl;
    }

    @Override
    public Map<String, Object> getProfilePhotos() {
        User user = getLoggedInUser();
        Profile profile = profileRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found"));

        Map<String, Object> photos = new LinkedHashMap<>();
        photos.put("profilePhotoUrl", profile.getProfilePhotoUrl());
        photos.put("photoUrl2", profile.getPhotoUrl2());
        photos.put("photoUrl3", profile.getPhotoUrl3());
        photos.put("photoUrl4", profile.getPhotoUrl4());
        photos.put("photoUrl5", profile.getPhotoUrl5());
        photos.put("photoUrl6", profile.getPhotoUrl6());
        photos.put("photoCount", profile.getPhotoCount());
        photos.put("hasPhotos", profile.getPhotoCount() > 0);

        // Add photo URLs as array for easy frontend consumption
        List<String> allPhotos = Arrays.asList(
                profile.getProfilePhotoUrl(),
                profile.getPhotoUrl2(),
                profile.getPhotoUrl3(),
                profile.getPhotoUrl4(),
                profile.getPhotoUrl5(),
                profile.getPhotoUrl6()).stream()
                .filter(Objects::nonNull)
                .filter(url -> !url.isEmpty())
                .collect(Collectors.toList());

        photos.put("allPhotos", allPhotos);

        return photos;
    }

    @Override
    public void deleteProfilePhoto(int photoIndex) {
        if (photoIndex < 0 || photoIndex > 5) {
            throw new BadRequestException("Photo index must be between 0 and 5");
        }

        User user = getLoggedInUser();
        Profile profile = profileRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found"));

        // Clear photo URL based on index
        updatePhotoUrlByIndex(profile, photoIndex, null);

        // Update photo count and save
        updatePhotoCount(profile);
        profile.setUpdatedAt(LocalDateTime.now());
        profileRepository.save(profile);

        log.info("Photo deleted for user: {}, index: {}", user.getEmail(), photoIndex);
    }

    @Override
    public void setPrimaryPhoto(int photoIndex) {
        if (photoIndex < 0 || photoIndex > 5) {
            throw new BadRequestException("Photo index must be between 0 and 5");
        }

        User user = getLoggedInUser();
        Profile profile = profileRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found"));

        // Get current photo at the specified index
        String photoUrlToSetAsPrimary = getPhotoUrlByIndex(profile, photoIndex);
        if (photoUrlToSetAsPrimary == null || photoUrlToSetAsPrimary.isEmpty()) {
            throw new BadRequestException("No photo at index: " + photoIndex);
        }

        // If trying to set primary photo as already primary, do nothing
        if (photoIndex == 0 && photoUrlToSetAsPrimary.equals(profile.getProfilePhotoUrl())) {
            log.info("Photo is already primary for user: {}", user.getEmail());
            return;
        }

        // Store current primary photo URL
        String oldPrimaryUrl = profile.getProfilePhotoUrl();

        // Set new primary photo
        profile.setProfilePhotoUrl(photoUrlToSetAsPrimary);

        // If there was an old primary photo and it's different from the new one
        if (oldPrimaryUrl != null && !oldPrimaryUrl.isEmpty() && !oldPrimaryUrl.equals(photoUrlToSetAsPrimary)) {
            // Find an empty slot for the old primary photo
            boolean moved = false;
            for (int i = 1; i <= 5; i++) {
                if (getPhotoUrlByIndex(profile, i) == null || getPhotoUrlByIndex(profile, i).isEmpty()) {
                    updatePhotoUrlByIndex(profile, i, oldPrimaryUrl);
                    moved = true;
                    break;
                }
            }

            // If no empty slot and photoIndex > 0, swap it into the slot where the new
            // primary came from
            if (!moved && photoIndex > 0) {
                updatePhotoUrlByIndex(profile, photoIndex, oldPrimaryUrl);
            }
        } else if (photoIndex > 0) {
            // If setting a non-primary photo as primary and no old primary existed, clear
            // its original slot
            updatePhotoUrlByIndex(profile, photoIndex, null);
        }

        // Update photo count and save
        updatePhotoCount(profile);
        profile.setUpdatedAt(LocalDateTime.now());
        profileRepository.save(profile);

        log.info("Primary photo set for user: {}, index: {}", user.getEmail(), photoIndex);
    }

    @Override
    public Map<String, String> uploadIdProof(MultipartFile file, String idProofType, String idProofNumber) {
        // Validation
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is empty");
        }

        // 1. File Type Validation
        String contentType = file.getContentType();
        if (contentType == null ||
                (!contentType.equals("image/jpeg") &&
                        !contentType.equals("image/png") &&
                        !contentType.equals("application/pdf"))) {
            throw new BadRequestException("Only JPEG, PNG and PDF files are allowed");
        }

        // 2. File Size Validation (10MB)
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new BadRequestException("File size must be less than 10MB");
        }

        // Get user's profile
        User user = getLoggedInUser();
        Profile profile = profileRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found. Please create a profile first."));

        // Store file and get URL
        String idProofUrl = fileStorageService.storeFile(file);

        // Update profile
        profile.setIdProofUrl(idProofUrl);
        profile.setIdProofType(idProofType);
        profile.setIdProofNumber(idProofNumber);
        profile.setVerificationStatus(Profile.VerificationStatus.PENDING);
        profile.setUpdatedAt(LocalDateTime.now());

        profileRepository.save(profile);

        log.info("ID Proof uploaded for user: {}, type: {}, url: {}", user.getEmail(), idProofType, idProofUrl);

        Map<String, String> response = new HashMap<>();
        response.put("message", "ID Proof uploaded successfully. Verification is pending.");
        response.put("idProofUrl", idProofUrl);
        response.put("verificationStatus", "PENDING");

        return response;
    }

    // ================= ADMIN VERIFICATION METHODS =================

    @Override
    public List<ProfileResponseDto> getPendingVerificationProfiles() {
        return profileRepository.findAll()
                .stream()
                .filter(p -> p.getVerificationStatus() == Profile.VerificationStatus.PENDING)
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public void verifyProfile(Long profileId, boolean approve, String notes, String verifiedBy) {
        Profile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found with id: " + profileId));

        if (approve) {
            profile.setVerificationStatus(Profile.VerificationStatus.VERIFIED);
            profile.setVerifiedAt(LocalDateTime.now());
            profile.setVerifiedBy(verifiedBy);
            profile.setVerificationNotes(notes != null ? notes : "Profile verified successfully");
            log.info("Profile {} verified by admin: {}", profileId, verifiedBy);
        } else {
            profile.setVerificationStatus(Profile.VerificationStatus.REJECTED);
            profile.setVerificationNotes(notes != null ? notes : "Profile verification rejected");
            log.info("Profile {} rejected by admin: {}", profileId, verifiedBy);
        }

        profile.setUpdatedAt(LocalDateTime.now());
        profileRepository.save(profile);
    }

    @Override
    public Map<String, Object> getVerificationStatistics() {
        long total = profileRepository.count();
        long verified = profileRepository.countByVerificationStatus(Profile.VerificationStatus.VERIFIED);
        long pending = profileRepository.countByVerificationStatus(Profile.VerificationStatus.PENDING);
        long rejected = profileRepository.countByVerificationStatus(Profile.VerificationStatus.REJECTED);
        long unverified = profileRepository.countByVerificationStatus(Profile.VerificationStatus.UNVERIFIED);

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalProfiles", total);
        stats.put("verified", verified);
        stats.put("pending", pending);
        stats.put("rejected", rejected);
        stats.put("unverified", unverified);
        stats.put("verificationRate", total > 0 ? (verified * 100.0 / total) : 0);

        return stats;
    }

    // ================= HELPER METHODS =================

    private void updatePhotoUrlByIndex(Profile profile, int index, String url) {
        switch (index) {
            case 0 -> profile.setProfilePhotoUrl(url);
            case 1 -> profile.setPhotoUrl2(url);
            case 2 -> profile.setPhotoUrl3(url);
            case 3 -> profile.setPhotoUrl4(url);
            case 4 -> profile.setPhotoUrl5(url);
            case 5 -> profile.setPhotoUrl6(url);
        }
    }

    private String getPhotoUrlByIndex(Profile profile, int index) {
        return switch (index) {
            case 0 -> profile.getProfilePhotoUrl();
            case 1 -> profile.getPhotoUrl2();
            case 2 -> profile.getPhotoUrl3();
            case 3 -> profile.getPhotoUrl4();
            case 4 -> profile.getPhotoUrl5();
            case 5 -> profile.getPhotoUrl6();
            default -> null;
        };
    }

    private void updatePhotoCount(Profile profile) {
        int count = 0;
        if (profile.getProfilePhotoUrl() != null && !profile.getProfilePhotoUrl().isEmpty())
            count++;
        if (profile.getPhotoUrl2() != null && !profile.getPhotoUrl2().isEmpty())
            count++;
        if (profile.getPhotoUrl3() != null && !profile.getPhotoUrl3().isEmpty())
            count++;
        if (profile.getPhotoUrl4() != null && !profile.getPhotoUrl4().isEmpty())
            count++;
        if (profile.getPhotoUrl5() != null && !profile.getPhotoUrl5().isEmpty())
            count++;
        if (profile.getPhotoUrl6() != null && !profile.getPhotoUrl6().isEmpty())
            count++;
        profile.setPhotoCount(count);
    }

    private void validatePhotoFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is empty");
        }

        // Check file type
        String contentType = file.getContentType();
        if (contentType == null ||
                (!contentType.equals("image/jpeg") &&
                        !contentType.equals("image/png") &&
                        !contentType.equals("image/jpg"))) {
            throw new BadRequestException("Only JPEG, JPG and PNG images are allowed");
        }

        // Check file size (max 5MB)
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new BadRequestException("File size must be less than 5MB");
        }

        // Check filename
        String fileName = file.getOriginalFilename();
        if (fileName != null && fileName.contains("..")) {
            throw new BadRequestException("Invalid file name");
        }
    }

    // In ProfileServiceImpl.java, update the mapToResponse method:
    private ProfileResponseDto mapToResponse(Profile profile) {
        User viewer = getLoggedInUser();
        User owner = profile.getUser();

        ProfileResponseDto response = ProfileResponseDto.builder()
                .id(profile.getId())
                .profileId(owner.getProfileId()) // Fetch from User entity
                .fullName(profile.getFullName())
                .gender(profile.getGender())
                .age(profile.getAge())
                .height(profile.getHeight())
                .weight(profile.getWeight())
                .maritalStatus(profile.getMaritalStatus())
                .motherTongue(profile.getMotherTongue())
                .religion(profile.getReligion())
                .caste(profile.getCaste())
                .subCaste(profile.getSubCaste())
                .gotra(profile.getGotra())
                .educationLevel(profile.getEducationLevel())
                .educationField(profile.getEducationField())
                .college(profile.getCollege())
                .occupation(profile.getOccupation())
                .company(profile.getCompany())
                .workingWith(profile.getWorkingWith())
                .annualIncome(profile.getAnnualIncome())
                .workingCity(profile.getWorkingCity())
                .diet(profile.getDiet())
                .drinkingHabit(profile.getDrinkingHabit())
                .smokingHabit(profile.getSmokingHabit())
                .country(profile.getCountry())
                .state(profile.getState())
                .city(profile.getCity())
                .address(profile.getAddress())
                .grewUpIn(profile.getGrewUpIn())
                .zipCode(profile.getZipCode())
                .residencyStatus(profile.getResidencyStatus())
                .bloodGroup(profile.getBloodGroup())
                .healthInformation(profile.getHealthInformation())
                .disability(profile.getDisability())
                .fatherStatus(profile.getFatherStatus())
                .motherStatus(profile.getMotherStatus())
                .brothersCount(profile.getBrothersCount())
                .sistersCount(profile.getSistersCount())
                .familyFinancialStatus(profile.getFamilyFinancialStatus())
                .familyLocation(profile.getFamilyLocation())
                .aboutMe(profile.getAboutMe())
                .hobbies(profile.getHobbies())

                .profileCreatedBy(profile.getProfileCreatedBy())
                .profileVisibility(profile.getProfileVisibility())
                .profileComplete(profile.getProfileComplete())
                .isPremium(profile.isPremium())
                .profilePhotoVisibility(
                        profile.getProfilePhotoVisibility() != null ? profile.getProfilePhotoVisibility().name() : null)
                .albumPhotoVisibility(
                        profile.getAlbumPhotoVisibility() != null ? profile.getAlbumPhotoVisibility().name() : null)

                // Astro
                .timeOfBirth(profile.getTimeOfBirth())
                .placeOfBirth(profile.getPlaceOfBirth())
                .manglikStatus(profile.getManglikStatus() != null ? profile.getManglikStatus().name() : null)
                .nakshatra(profile.getNakshatra())
                .rashi(profile.getRashi())
                .astroVisibility(
                        profile.getAstroVisibility() != null ? profile.getAstroVisibility().name() : "ALL_MEMBERS")

                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                // Photos with visibility logic
                .profilePhotoUrl(photoVisibilityService.getProfilePhoto(viewer, owner))
                .photoUrl2(photoVisibilityService.getAlbumPhoto(viewer, owner, profile.getPhotoUrl2()))
                .photoUrl3(photoVisibilityService.getAlbumPhoto(viewer, owner, profile.getPhotoUrl3()))
                .photoUrl4(photoVisibilityService.getAlbumPhoto(viewer, owner, profile.getPhotoUrl4()))
                .photoUrl5(photoVisibilityService.getAlbumPhoto(viewer, owner, profile.getPhotoUrl5()))
                .photoUrl6(photoVisibilityService.getAlbumPhoto(viewer, owner, profile.getPhotoUrl6()))
                .photoCount(profile.getPhotoCount())
                // Verification
                .verificationStatus(
                        profile.getVerificationStatus() != null ? profile.getVerificationStatus().name() : "UNVERIFIED")
                .idProofUrl(profile.getIdProofUrl())
                .idProofType(profile.getIdProofType())
                .idProofNumber(profile.getIdProofNumber())
                .verifiedAt(profile.getVerifiedAt())
                .verifiedBy(profile.getVerifiedBy())
                .verificationNotes(profile.getVerificationNotes())
                .verified(profile.isVerified())
                .contactDisplayStatus(profile.getContactDisplayStatus())
                .build();

        // ✅ CONTACT VISIBILITY FILTERING
        applyContactPrivacy(viewer, profile, response);

        // ✅ ASTRO VISIBILITY FILTERING
        applyAstroPrivacy(viewer, profile, response);

        return response;
    }

    private void applyAstroPrivacy(User viewer, Profile ownerProfile, ProfileResponseDto response) {
        User owner = ownerProfile.getUser();

        // 1. Self view
        if (viewer.getId().equals(owner.getId())) {
            return;
        }

        // 2. Admin override
        if (viewer.getRole() != null && viewer.getRole().equalsIgnoreCase("ROLE_ADMIN")) {
            return;
        }

        com.punarmilan.backend.entity.enums.AstroVisibility visibility = ownerProfile.getAstroVisibility();
        if (visibility == null)
            visibility = com.punarmilan.backend.entity.enums.AstroVisibility.ALL_MEMBERS;

        boolean canSee = false;

        if (visibility == com.punarmilan.backend.entity.enums.AstroVisibility.ALL_MEMBERS) {
            canSee = true;
        } else if (visibility == com.punarmilan.backend.entity.enums.AstroVisibility.CONTACTED_AND_ACCEPTED) {
            // Check if matched OR connected
            boolean matched = matchRepository.areUsersMatched(viewer, owner);
            boolean connected = connectionRepository.areUsersConnected(viewer, owner);
            canSee = matched || connected;
        }

        // Apply filters if not allowed to see
        if (!canSee) {
            response.setTimeOfBirth(null);
            response.setPlaceOfBirth(null);
            response.setManglikStatus(null);
            response.setNakshatra(null);
            response.setRashi(null);
        }
    }

    private void applyContactPrivacy(User viewer, Profile ownerProfile, ProfileResponseDto response) {
        User owner = ownerProfile.getUser();

        // 1. Self view
        if (viewer.getId().equals(owner.getId())) {
            response.setMobileNumber(owner.getMobileNumber());
            return;
        }

        // 2. Admin override
        if (viewer.getRole() != null && viewer.getRole().equalsIgnoreCase("ROLE_ADMIN")) {
            response.setMobileNumber(owner.getMobileNumber());
            return;
        }

        String status = ownerProfile.getContactDisplayStatus();
        if (status == null || status.isEmpty()) {
            // Default to "Only Premium Members"
            status = "Only Premium Members";
        }

        boolean canSee = false;

        switch (status) {
            case "Only Premium Members":
                canSee = viewer.getPremium() != null && viewer.getPremium();
                break;

            case "Only Premium Members you like":
                boolean viewerIsPremium = viewer.getPremium() != null && viewer.getPremium();
                if (viewerIsPremium) {
                    // Check if owner has liked the viewer
                    Optional<com.punarmilan.backend.entity.Match> match = matchRepository.findActiveMatchBetweenUsers(
                            owner,
                            viewer);
                    if (match.isPresent()) {
                        com.punarmilan.backend.entity.Match m = match.get();
                        if (m.getUser1().getId().equals(owner.getId())) {
                            canSee = m.isUser1Liked();
                        } else {
                            canSee = m.isUser2Liked();
                        }
                    }
                }
                break;

            case "Only visible to all your Matches":
                canSee = matchRepository.areUsersMatched(viewer, owner);
                break;

            case "No one":
            default:
                canSee = false;
                break;
        }

        if (canSee) {
            response.setMobileNumber(owner.getMobileNumber());
        } else {
            response.setMobileNumber(null);
        }
    }
}