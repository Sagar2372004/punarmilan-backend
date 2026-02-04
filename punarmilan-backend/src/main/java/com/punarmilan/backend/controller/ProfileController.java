package com.punarmilan.backend.controller;

import com.punarmilan.backend.dto.ProfileRequestDto;
import com.punarmilan.backend.dto.ProfileResponseDto;
import com.punarmilan.backend.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/profiles")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    /**
     * CREATE / UPDATE PROFILE
     * Role: USER
     * JWT required
     */
    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ProfileResponseDto> createOrUpdateProfile(
            @Valid @RequestBody ProfileRequestDto requestDto) {

        ProfileResponseDto response = profileService.saveOrUpdateProfile(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * PARTIAL UPDATE PROFILE (PATCH)
     * Role: USER
     */
    @PatchMapping("/me")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ProfileResponseDto> patchProfile(
            @RequestBody ProfileRequestDto requestDto) {

        ProfileResponseDto response = profileService.saveOrUpdateProfile(requestDto);
        return ResponseEntity.ok(response);
    }

    /**
     * GET LOGGED-IN USER PROFILE
     * Role: USER
     * JWT required
     */
    @GetMapping("/me")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ProfileResponseDto> getMyProfile() {

        return ResponseEntity.ok(profileService.getMyProfile());
    }

    /**
     * GET PROFILE BY PUBLIC ID
     * Role: USER
     */
    @GetMapping("/{profileId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ProfileResponseDto> getProfileByPublicId(@PathVariable String profileId) {
        return ResponseEntity.ok(profileService.getProfileByPublicId(profileId));
    }

    /**
     * SEARCH PROFILES (MATCHMAKING)
     * Role: USER
     * JWT required
     */
    @GetMapping("/search")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<ProfileResponseDto>> searchProfiles(
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String religion) {

        return ResponseEntity.ok(
                profileService.searchProfiles(gender, city, religion));
    }

    /**
     * ADMIN – GET ALL PROFILES
     * Role: ADMIN
     * JWT required
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ProfileResponseDto>> getAllProfiles() {

        return ResponseEntity.ok(profileService.getAllProfiles());
    }

    // ✅ NEW: Upload Profile Photo
    @PostMapping("/photo")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, String>> uploadProfilePhoto(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "photoIndex", defaultValue = "0") int photoIndex) {

        String photoUrl = profileService.uploadProfilePhoto(file, photoIndex);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Photo uploaded successfully");
        response.put("photoUrl", photoUrl);
        response.put("photoIndex", String.valueOf(photoIndex));

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ✅ NEW: Get All Photos
    @GetMapping("/photos")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> getProfilePhotos() {

        Map<String, Object> photos = profileService.getProfilePhotos();

        return ResponseEntity.ok(photos);
    }

    // ✅ NEW: Delete Photo
    @DeleteMapping("/photos/{photoIndex}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Void> deleteProfilePhoto(@PathVariable int photoIndex) {

        profileService.deleteProfilePhoto(photoIndex);
        return ResponseEntity.noContent().build();
    }

    // ✅ NEW: Set Primary Photo
    @PutMapping("/photos/{photoIndex}/primary")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Void> setPrimaryPhoto(@PathVariable int photoIndex) {

        profileService.setPrimaryPhoto(photoIndex);
        return ResponseEntity.ok().build();
    }

    // ✅ NEW: Upload ID Proof
    @PostMapping("/id-proof")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, String>> uploadIdProof(
            @RequestParam("file") MultipartFile file,
            @RequestParam("idProofType") String idProofType,
            @RequestParam("idProofNumber") String idProofNumber) {

        Map<String, String> response = profileService.uploadIdProof(file, idProofType, idProofNumber);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

}
