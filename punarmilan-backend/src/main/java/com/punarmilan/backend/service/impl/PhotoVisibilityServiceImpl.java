 package com.punarmilan.backend.service.impl;

import com.punarmilan.backend.dto.PhotoDto;
import com.punarmilan.backend.entity.Profile;
import com.punarmilan.backend.entity.User;
import com.punarmilan.backend.entity.enums.AlbumPhotoVisibility;
import com.punarmilan.backend.entity.enums.ProfilePhotoVisibility;
import com.punarmilan.backend.repository.MatchRepository;
import com.punarmilan.backend.repository.ProfileRepository;
import com.punarmilan.backend.service.PhotoVisibilityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PhotoVisibilityServiceImpl implements PhotoVisibilityService {

    private final MatchRepository matchRepository;
    private final ProfileRepository profileRepository;

    private static final String BLURRED_PLACEHOLDER = "/assets/images/blurred-photo.jpg";

    @Override
    public PhotoDto getProfilePhoto(User viewer, User owner) {
        Profile ownerProfile = profileRepository.findByUser(owner)
                .orElseThrow(() -> new RuntimeException("Profile not found for owner"));

        String actualUrl = ownerProfile.getProfilePhotoUrl();
        if (actualUrl == null || actualUrl.isEmpty()) {
            return null;
        }

        if (canViewProfilePhoto(viewer, owner)) {
            return PhotoDto.builder()
                    .url(actualUrl)
                    .blurred(false)
                    .build();
        }

        // Determine reason
        String reason = "LIKE_REQUIRED";
        if (ownerProfile.getProfilePhotoVisibility() == ProfilePhotoVisibility.LIKED_AND_PREMIUM) {
            // LIKED_AND_PREMIUM means: Premium OR Liked.
            // If they can't view it, it means they are neither.
            // We usually prompt for PREMIUM first as it's the easiest way to unlock.
            reason = "PREMIUM_ONLY";
        }

        return PhotoDto.builder()
                .url(BLURRED_PLACEHOLDER)
                .blurred(true)
                .restrictionReason(reason)
                .build();
    }

    @Override
    public PhotoDto getAlbumPhoto(User viewer, User owner, String photoUrl) {
        if (photoUrl == null || photoUrl.isEmpty()) {
            return null;
        }

        if (canViewAlbumPhoto(viewer, owner)) {
            return PhotoDto.builder()
                    .url(photoUrl)
                    .blurred(false)
                    .build();
        }

        Profile ownerProfile = profileRepository.findByUser(owner)
                .orElseThrow(() -> new RuntimeException("Profile not found for owner"));

        String reason = "LIKE_REQUIRED";
        if (ownerProfile.getAlbumPhotoVisibility() == AlbumPhotoVisibility.LIKED_AND_PREMIUM) {
            reason = "PREMIUM_ONLY";
        }

        return PhotoDto.builder()
                .url(BLURRED_PLACEHOLDER)
                .blurred(true)
                .restrictionReason(reason)
                .build();
    }

    @Override
    public boolean canViewProfilePhoto(User viewer, User owner) {
        if (viewer.getId().equals(owner.getId()))
            return true; // Self view

        Profile ownerProfile = profileRepository.findByUser(owner)
                .orElseThrow(() -> new RuntimeException("Profile not found for owner"));

        ProfilePhotoVisibility visibility = ownerProfile.getProfilePhotoVisibility();

        if (visibility == ProfilePhotoVisibility.ALL_MEMBERS) {
            return true;
        }

        if (visibility == ProfilePhotoVisibility.LIKED_AND_PREMIUM) {
            return isPremium(viewer) || isLikedByOwner(owner, viewer);
        }

        return false;
    }

    @Override
    public boolean canViewAlbumPhoto(User viewer, User owner) {
        if (viewer.getId().equals(owner.getId()))
            return true; // Self view

        Profile ownerProfile = profileRepository.findByUser(owner)
                .orElseThrow(() -> new RuntimeException("Profile not found for owner"));

        AlbumPhotoVisibility visibility = ownerProfile.getAlbumPhotoVisibility();

        if (visibility == AlbumPhotoVisibility.LIKED_AND_PREMIUM) {
            return isPremium(viewer) || isLikedByOwner(owner, viewer);
        }

        if (visibility == AlbumPhotoVisibility.ONLY_LIKED) {
            return isLikedByOwner(owner, viewer);
        }

        return false;
    }

    private boolean isPremium(User user) {
        return Boolean.TRUE.equals(user.getPremium());
    }

    private boolean isLikedByOwner(User owner, User viewer) {
        return matchRepository.findMatchBetweenUsers(owner, viewer)
                .map(match -> match.hasUserLiked(owner.getId()))
                .orElse(false);
    }
}
