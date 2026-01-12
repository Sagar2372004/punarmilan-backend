package com.punarmilan.backend.controller;

import com.punarmilan.backend.dto.ProfileResponseDto;
import com.punarmilan.backend.dto.UserResponse;
import com.punarmilan.backend.service.ProfileService;
import com.punarmilan.backend.service.impl.UserServiceImpl;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {
    
    private final ProfileService profileService;
    private final UserServiceImpl userService;
    
    // 🔹 GET profiles pending verification
    @GetMapping("/profiles/pending")
    public ResponseEntity<List<ProfileResponseDto>> getPendingVerification() {
        return ResponseEntity.ok(profileService.getPendingVerificationProfiles());
    }
    
    // 🔹 VERIFY/REJECT profile
    @PostMapping("/profiles/{profileId}/verify")
    public ResponseEntity<Map<String, String>> verifyProfile(
            @PathVariable Long profileId,
            @RequestParam boolean approve,
            @RequestParam(required = false) String notes) {
        
        String adminEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        
        profileService.verifyProfile(profileId, approve, notes, adminEmail);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Profile verification updated successfully");
        response.put("status", approve ? "VERIFIED" : "REJECTED");
        
        return ResponseEntity.ok(response);
    }
    
    // 🔹 GET verification statistics
    @GetMapping("/verification/stats")
    public ResponseEntity<Map<String, Object>> getVerificationStats() {
        return ResponseEntity.ok(profileService.getVerificationStatistics());
    }
    @DeleteMapping("/{email}")
    public ResponseEntity<Void> deleteUser(@PathVariable String email) {
        userService.deleteUserByEmail(email);
        return ResponseEntity.noContent().build();
    }
    @GetMapping("/users")  // Or just @GetMapping if base path includes /users
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<UserResponse> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }
}