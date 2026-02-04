package com.punarmilan.backend.service;

import com.punarmilan.backend.dto.ProfileRequestDto;
import com.punarmilan.backend.dto.ProfileResponseDto;

import java.util.List;
import java.util.Map;

import org.springframework.web.multipart.MultipartFile;

public interface ProfileService {

    // Create or Update logged-in user's profile
    ProfileResponseDto saveOrUpdateProfile(ProfileRequestDto requestDto);

    // Get logged-in user's profile
    ProfileResponseDto getMyProfile();

    // Get a profile by its publicmasked ID
    ProfileResponseDto getProfileByPublicId(String profileId);

    // Search profiles (basic matchmaking)
    List<ProfileResponseDto> searchProfiles(String gender, String city, String religion);

    // Admin - get all profiles
    List<ProfileResponseDto> getAllProfiles();

    // Photo Methods
    String uploadProfilePhoto(MultipartFile file, int photoIndex);

    Map<String, Object> getProfilePhotos();

    void deleteProfilePhoto(int photoIndex);

    void setPrimaryPhoto(int photoIndex);

    // ID Proof Upload
    Map<String, String> uploadIdProof(MultipartFile file, String idProofType, String idProofNumber);

    // ================= ADMIN VERIFICATION METHODS =================
    List<ProfileResponseDto> getPendingVerificationProfiles();

    void verifyProfile(Long profileId, boolean approve, String notes, String verifiedBy);

    Map<String, Object> getVerificationStatistics();
}