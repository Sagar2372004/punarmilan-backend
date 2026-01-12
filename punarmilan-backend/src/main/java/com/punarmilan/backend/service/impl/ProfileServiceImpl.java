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
import com.punarmilan.backend.service.ProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ProfileServiceImpl implements ProfileService {

    private final ProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

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
        
        // Gender validation with more options
        String gender = dto.getGender();
        if (gender == null || gender.trim().isEmpty()) {
            throw new BadRequestException("Gender is required");
        }
        
        // Acceptable gender values
        List<String> validGenders = Arrays.asList("Male", "Female", "Other", "Prefer not to say");
        boolean isValidGender = validGenders.stream()
                .anyMatch(valid -> valid.equalsIgnoreCase(gender));
        
        if (!isValidGender) {
            throw new BadRequestException("Gender must be one of: " + String.join(", ", validGenders));
        }
        
        Profile profile = profileRepository.findByUser(user)
                .orElse(Profile.builder()
                        .user(user)
                        .createdAt(LocalDateTime.now())
                        .build());
        
        // Personal Details
        profile.setFullName(dto.getFullName());
        profile.setGender(dto.getGender());
        profile.setDateOfBirth(dto.getDateOfBirth());
        profile.setAge(Period.between(dto.getDateOfBirth(), LocalDate.now()).getYears());
        profile.setHeight(dto.getHeight());
        profile.setWeight(dto.getWeight());
        profile.setMaritalStatus(dto.getMaritalStatus());
        profile.setMotherTongue(dto.getMotherTongue());

        // Religion Details
        profile.setReligion(dto.getReligion());
        profile.setCaste(dto.getCaste());
        profile.setSubCaste(dto.getSubCaste());
        profile.setGotra(dto.getGotra());

        // Education & Career
        profile.setEducationLevel(dto.getEducationLevel());
        profile.setEducationField(dto.getEducationField());
        profile.setCollege(dto.getCollege());
        profile.setOccupation(dto.getOccupation());
        profile.setCompany(dto.getCompany());
        profile.setAnnualIncome(dto.getAnnualIncome());
        profile.setWorkingCity(dto.getWorkingCity());

        // Lifestyle
        profile.setDiet(dto.getDiet());
        profile.setDrinkingHabit(dto.getDrinkingHabit());
        profile.setSmokingHabit(dto.getSmokingHabit());

        // Location
        profile.setCountry(dto.getCountry());
        profile.setState(dto.getState());
        profile.setCity(dto.getCity());
        profile.setAddress(dto.getAddress());

        // About & Settings
        profile.setAboutMe(dto.getAboutMe());
        profile.setProfileCreatedBy(dto.getProfileCreatedBy());
        profile.setProfileVisibility(dto.getProfileVisibility());

        // Photos
        profile.setProfilePhotoUrl(dto.getProfilePhotoUrl());
        profile.setPhotoUrl2(dto.getPhotoUrl2());
        profile.setPhotoUrl3(dto.getPhotoUrl3());
        profile.setPhotoUrl4(dto.getPhotoUrl4());
        profile.setPhotoUrl5(dto.getPhotoUrl5());
        profile.setPhotoUrl6(dto.getPhotoUrl6());

        // Verification Fields
        profile.setIdProofUrl(dto.getIdProofUrl());
        profile.setIdProofType(dto.getIdProofType());
        profile.setIdProofNumber(dto.getIdProofNumber());
        
        // If ID proof is provided, set status to PENDING
        if (dto.getIdProofUrl() != null && !dto.getIdProofUrl().isEmpty()) {
            profile.setVerificationStatus(Profile.VerificationStatus.PENDING);
            log.info("Verification requested by user: {}", user.getEmail());
        }

        // Calculate photo count
        updatePhotoCount(profile);

        profile.setProfileComplete(true);
        profile.setUpdatedAt(LocalDateTime.now());

        Profile savedProfile = profileRepository.save(profile);
        log.info("Profile saved/updated for user: {}", user.getEmail());

        return mapToResponse(savedProfile);
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
                        return "Female".equalsIgnoreCase(p.getGender()); // Male ला फक्त Female
                    } else if ("Female".equalsIgnoreCase(currentUserGender)) {
                        return "Male".equalsIgnoreCase(p.getGender());   // Female ला फक्त Male
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
            profile.getPhotoUrl6()
        ).stream()
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

        // If there was an old primary photo and it's different from new one
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
            
            // If no empty slot and photoIndex > 0, clear the slot where the new primary came from
            if (!moved && photoIndex > 0) {
                updatePhotoUrlByIndex(profile, photoIndex, null);
            }
        } else if (photoIndex > 0) {
            // If setting a non-primary photo as primary, clear its original slot
            updatePhotoUrlByIndex(profile, photoIndex, null);
        }

        // Update photo count and save
        updatePhotoCount(profile);
        profile.setUpdatedAt(LocalDateTime.now());
        profileRepository.save(profile);

        log.info("Primary photo set for user: {}, index: {}", user.getEmail(), photoIndex);
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
        List<Profile> allProfiles = profileRepository.findAll();
        
        long total = allProfiles.size();
        long verified = allProfiles.stream()
                .filter(p -> p.getVerificationStatus() == Profile.VerificationStatus.VERIFIED)
                .count();
        long pending = allProfiles.stream()
                .filter(p -> p.getVerificationStatus() == Profile.VerificationStatus.PENDING)
                .count();
        long rejected = allProfiles.stream()
                .filter(p -> p.getVerificationStatus() == Profile.VerificationStatus.REJECTED)
                .count();
        long unverified = allProfiles.stream()
                .filter(p -> p.getVerificationStatus() == Profile.VerificationStatus.UNVERIFIED)
                .count();
        
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
        if (profile.getProfilePhotoUrl() != null && !profile.getProfilePhotoUrl().isEmpty()) count++;
        if (profile.getPhotoUrl2() != null && !profile.getPhotoUrl2().isEmpty()) count++;
        if (profile.getPhotoUrl3() != null && !profile.getPhotoUrl3().isEmpty()) count++;
        if (profile.getPhotoUrl4() != null && !profile.getPhotoUrl4().isEmpty()) count++;
        if (profile.getPhotoUrl5() != null && !profile.getPhotoUrl5().isEmpty()) count++;
        if (profile.getPhotoUrl6() != null && !profile.getPhotoUrl6().isEmpty()) count++;
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

    // 🔁 ENTITY → RESPONSE DTO
    private ProfileResponseDto mapToResponse(Profile profile) {
        return ProfileResponseDto.builder()
                .id(profile.getId())
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
                .annualIncome(profile.getAnnualIncome())
                .workingCity(profile.getWorkingCity())
                .diet(profile.getDiet())
                .drinkingHabit(profile.getDrinkingHabit())
                .smokingHabit(profile.getSmokingHabit())
                .country(profile.getCountry())
                .state(profile.getState())
                .city(profile.getCity())
                .address(profile.getAddress())
                .aboutMe(profile.getAboutMe())
                .profileCreatedBy(profile.getProfileCreatedBy())
                .profileVisibility(profile.getProfileVisibility())
                .profileComplete(profile.isProfileComplete())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                // Photos
                .profilePhotoUrl(profile.getProfilePhotoUrl())
                .photoUrl2(profile.getPhotoUrl2())
                .photoUrl3(profile.getPhotoUrl3())
                .photoUrl4(profile.getPhotoUrl4())
                .photoUrl5(profile.getPhotoUrl5())
                .photoUrl6(profile.getPhotoUrl6())
                .photoCount(profile.getPhotoCount())
                // Verification
                .verificationStatus(profile.getVerificationStatus() != null ? 
                        profile.getVerificationStatus().name() : "UNVERIFIED")
                .idProofUrl(profile.getIdProofUrl())
                .idProofType(profile.getIdProofType())
                .idProofNumber(profile.getIdProofNumber())
                .verifiedAt(profile.getVerifiedAt())
                .verifiedBy(profile.getVerifiedBy())
                .verificationNotes(profile.getVerificationNotes())
                .verified(profile.isVerified())
                .build();
    }
}