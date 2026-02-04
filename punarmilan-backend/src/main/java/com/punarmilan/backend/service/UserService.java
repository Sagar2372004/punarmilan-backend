package com.punarmilan.backend.service;

import com.punarmilan.backend.dto.AuthResponse;
import com.punarmilan.backend.dto.UserLoginRequest;
import com.punarmilan.backend.dto.UserRegisterRequest;
import com.punarmilan.backend.dto.UserResponse;
import com.punarmilan.backend.entity.User;

import java.util.List;

public interface UserService {
    UserResponse registerUser(UserRegisterRequest request);

    AuthResponse loginUser(UserLoginRequest request);

    List<UserResponse> getAllUsers();

    void deleteUserByEmail(String email);

    User getLoggedInUser();

    void updateEmailInitiate(String newEmail);

    void updateEmailVerify(String token);

    void hideProfile(String duration);

    String encodeProfileId(Long realId);

    Long decodeProfileId(String profileId);
}
