package com.punarmilan.backend.service;

import com.punarmilan.backend.dto.PhotoDto;
import com.punarmilan.backend.entity.User;

public interface PhotoVisibilityService {
    PhotoDto getProfilePhoto(User viewer, User owner);

    PhotoDto getAlbumPhoto(User viewer, User owner, String photoUrl);

    // Core logic methods requested by user
    boolean canViewProfilePhoto(User viewer, User owner);

    boolean canViewAlbumPhoto(User viewer, User owner);
}
