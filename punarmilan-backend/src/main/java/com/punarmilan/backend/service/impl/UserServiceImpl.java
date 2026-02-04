package com.punarmilan.backend.service.impl;

import com.punarmilan.backend.dto.AuthResponse;

import com.punarmilan.backend.dto.UserLoginRequest;
import com.punarmilan.backend.dto.UserRegisterRequest;
import com.punarmilan.backend.dto.UserResponse;
import com.punarmilan.backend.entity.User;
import com.punarmilan.backend.exception.ResourceNotFoundException;
import com.punarmilan.backend.exception.UnauthorizedException;
import com.punarmilan.backend.repository.AuditLogRepository;
import com.punarmilan.backend.repository.ConnectionRequestRepository;
import com.punarmilan.backend.repository.ConversationRepository;
import com.punarmilan.backend.repository.MatchRepository;
import com.punarmilan.backend.repository.MessageRepository;
import com.punarmilan.backend.repository.NotificationRepository;
import com.punarmilan.backend.repository.PartnerPreferenceRepository;
import com.punarmilan.backend.repository.PaymentTransactionRepository;
import com.punarmilan.backend.repository.PremiumSubscriptionRepository;
import com.punarmilan.backend.repository.UserViewHistoryRepository;
import com.punarmilan.backend.repository.ProfileRepository;
import com.punarmilan.backend.repository.UserRepository;
import com.punarmilan.backend.security.JwtUtil;
import com.punarmilan.backend.service.UserService;
import com.punarmilan.backend.service.VerificationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final ProfileRepository profileRepository;
    private final MatchRepository matchRepository;
    private final MessageRepository messageRepository;
    private final NotificationRepository notificationRepository;
    private final PartnerPreferenceRepository partnerPreferenceRepository;
    private final UserViewHistoryRepository userViewHistoryRepository;
    private final PremiumSubscriptionRepository premiumSubscriptionRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final AuditLogRepository auditLogRepository;
    private final ConnectionRequestRepository connectionRequestRepository;
    private final ConversationRepository conversationRepository;
    private final VerificationService verificationService;

    @Override
    public UserResponse registerUser(UserRegisterRequest request) {
        // This will now only initiate the registration and store in Redis
        verificationService.initiateRegistration(request);

        log.info("Registration initiated for: {}", request.getEmail());

        // Return a response indicating that verification is pending
        return UserResponse.builder()
                .email(request.getEmail())
                .mobileNumber(request.getMobileNumber())
                .isVerified(false)
                .isActive(true)
                .build();
    }

    @Override
    public AuthResponse loginUser(UserLoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail().toLowerCase())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        if (!user.isActive()) {
            throw new UnauthorizedException("Account is blocked by admin");
        }

        if (!user.isVerified()) {
            throw new UnauthorizedException("Please verify your email first");
        }

        // Update last login time
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        // ✅ FIX: Generate token with email AND role
        String token = jwtUtil.generateToken(user.getEmail(), user.getRole());

        return AuthResponse.builder()
                .token(token)
                .type("Bearer")
                .user(convertToUserResponse(user)) // Add user details
                .message("Login successful")
                .build();
    }

    @Override
    public List<UserResponse> getAllUsers() {
        List<User> users = userRepository.findAll();
        return users.stream()
                .map(this::convertToUserResponse)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        log.info("Starting deletion for user: {}", email);

        // 1. Delete Social/Interaction Data
        matchRepository.deleteByUser(user);
        messageRepository.deleteByUser(user);
        notificationRepository.deleteByUser(user);
        connectionRequestRepository.deleteByUser(user);
        conversationRepository.deleteByUser(user);
        userViewHistoryRepository.deleteByUser(user);

        // 2. Delete Profile and Preferences
        profileRepository.findByUser(user).ifPresent(profile -> {
            partnerPreferenceRepository.deleteByProfile(profile);
            profileRepository.delete(profile);
        });

        // 3. Delete Billing/Subscription Data
        premiumSubscriptionRepository.deleteByUser(user);
        paymentTransactionRepository.deleteByUser(user);

        // 4. Cleanup Logs by Email
        auditLogRepository.deleteByPerformedByEmail(email);

        // 5. Finally delete user
        userRepository.delete(user);
        log.info("Full cleanup completed for user: {}", email);
    }

    @Override
    public User getLoggedInUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("User not authenticated");
        }

        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        // Auto-unhide if logic applies
        if (user.isHidden() && user.getHiddenUntil() != null && user.getHiddenUntil().isBefore(LocalDateTime.now())) {
            user.setHidden(false);
            user.setHiddenUntil(null);
            userRepository.save(user);
            log.info("User {} profile auto-unhidden", user.getEmail());
        }

        return user;
    }

    private UserResponse convertToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .mobileNumber(user.getMobileNumber())
                .role(user.getRole())
                .isActive(user.isActive())
                .isVerified(user.isVerified())
                .isHidden(user.isHidden())
                .hiddenUntil(user.getHiddenUntil())
                .isPremium(user.getPremium() != null ? user.getPremium() : false)
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .lastLogin(user.getLastLogin())
                .premiumSince(user.getPremiumSince())
                .profileId(user.getProfileId()) // Add the masked ID to response
                .build();
    }

    @Override
    public String encodeProfileId(Long realId) {
        return User.generateProfileId(realId);
    }

    @Override
    public Long decodeProfileId(String profileId) {
        if (profileId == null || !profileId.startsWith(User.PROFILE_ID_PREFIX)) {
            throw new com.punarmilan.backend.exception.BadRequestException("Invalid Profile ID format");
        }
        try {
            return Long.parseLong(profileId.substring(User.PROFILE_ID_PREFIX.length())) - User.PROFILE_ID_OFFSET;
        } catch (NumberFormatException e) {
            throw new com.punarmilan.backend.exception.BadRequestException("Invalid numeric part in Profile ID");
        }
    }

    @Override
    public void updateEmailInitiate(String newEmail) {
        User user = getLoggedInUser();
        verificationService.initiateEmailUpdate(user, newEmail);
    }

    @Override
    public void updateEmailVerify(String token) {
        verificationService.verifyEmailUpdate(token);
    }

    @Override
    public void hideProfile(String duration) {
        User user = getLoggedInUser();
        LocalDateTime hiddenUntil;

        if ("UNHIDE".equalsIgnoreCase(duration)) {
            user.setHidden(false);
            user.setHiddenUntil(null);
            userRepository.save(user);
            log.info("User {} manually unhid their profile", user.getEmail());
            return;
        }

        switch (duration.toUpperCase()) {
            case "1_WEEK":
                hiddenUntil = LocalDateTime.now().plusWeeks(1);
                break;
            case "2_WEEKS":
                hiddenUntil = LocalDateTime.now().plusWeeks(2);
                break;
            case "1_MONTH":
                hiddenUntil = LocalDateTime.now().plusMonths(1);
                break;
            default:
                throw new com.punarmilan.backend.exception.BadRequestException(
                        "Invalid duration. Use 1_WEEK, 2_WEEKS, 1_MONTH or UNHIDE");
        }

        user.setHidden(true);
        user.setHiddenUntil(hiddenUntil);
        userRepository.save(user);
        log.info("User {} hid their profile until {}", user.getEmail(), hiddenUntil);
    }
}