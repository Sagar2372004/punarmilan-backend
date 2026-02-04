package com.punarmilan.backend.controller;

import com.punarmilan.backend.dto.*;
import com.punarmilan.backend.entity.User;
import com.punarmilan.backend.service.UserService;
import com.punarmilan.backend.service.VerificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final VerificationService verificationService;

    // REGISTER
    @PostMapping("/register")
    public ResponseEntity<UserResponse> registerUser(
            @Valid @RequestBody UserRegisterRequest request) {

        UserResponse response = userService.registerUser(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    // LOGIN (JWT)
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> loginUser(
            @Valid @RequestBody UserLoginRequest request) {

        AuthResponse response = userService.loginUser(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/verify-email")
    public ResponseEntity<String> verifyEmail(@RequestParam String token) {
        verificationService.verifyEmail(token);
        return ResponseEntity
                .ok("Email verified successfully! Your account is now active.");
    }

    @DeleteMapping("/delete/{email}")
    public ResponseEntity<String> deleteUser(@PathVariable String email) {
        userService.deleteUserByEmail(email);
        return ResponseEntity.ok("User deleted successfully");
    }

    @DeleteMapping("/delete/me")
    public ResponseEntity<String> deleteMyAccount() {
        User user = userService.getLoggedInUser();
        userService.deleteUserByEmail(user.getEmail());
        return ResponseEntity.ok("Your account deleted successfully");
    }

    @PostMapping("/update-email")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<String> initiateEmailUpdate(
            @Valid @RequestBody UserRegisterRequest.EmailUpdateRequest request) {
        userService.updateEmailInitiate(request.getNewEmail());
        return ResponseEntity.ok("Verification email sent to your new address. Please verify to complete the update.");
    }

    @GetMapping("/verify-email-update")
    public ResponseEntity<String> verifyEmailUpdate(@RequestParam String token) {
        userService.updateEmailVerify(token);
        return ResponseEntity.ok("Email updated successfully!");
    }

    @PostMapping("/hide-profile")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<String> hideProfile(@RequestBody Map<String, String> request) {
        String duration = request.get("duration");
        userService.hideProfile(duration);
        return ResponseEntity.ok("Profile visibility updated successfully.");
    }

}
