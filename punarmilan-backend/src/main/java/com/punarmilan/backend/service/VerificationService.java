package com.punarmilan.backend.service;

import com.punarmilan.backend.dto.RegistrationSession;

import com.punarmilan.backend.dto.UserRegisterRequest;
import com.punarmilan.backend.entity.User;
import com.punarmilan.backend.exception.BadRequestException;
import com.punarmilan.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class VerificationService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final EmailService emailService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MatchService matchService;

    // Fallback in-memory storage for when Redis is unavailable
    private final Map<String, Object> inMemoryStore = new java.util.concurrent.ConcurrentHashMap<>();

    private static final String SESSION_KEY_PREFIX = "reg:session:";
    private static final String TOKEN_KEY_PREFIX = "reg:token:";
    private static final String EMAIL_UPDATE_PREFIX = "email:update:";
    private static final Duration EXPIRATION_TIME = Duration.ofMinutes(15);

    private void setValue(String key, Object value, Duration timeout) {
        try {
            redisTemplate.opsForValue().set(key, value, timeout);
        } catch (Exception e) {
            log.warn("Redis is unavailable, falling back to in-memory store: {}", e.getMessage());
            inMemoryStore.put(key, value);
            // In a real app, you'd want a scheduler to clean up inMemoryStore based on
            // timeout
        }
    }

    private Object getValue(String key) {
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.warn("Redis is unavailable, fetching from in-memory store: {}", e.getMessage());
            return inMemoryStore.get(key);
        }
    }

    private void deleteKey(String key) {
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.warn("Redis is unavailable, deleting from in-memory store: {}", e.getMessage());
            inMemoryStore.remove(key);
        }
    }

    public void initiateEmailUpdate(User user, String newEmail) {
        String email = newEmail.toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException("Email already taken");
        }

        String token = UUID.randomUUID().toString();

        // Store mappings
        // token -> {userId, newEmail}
        Map<String, String> updateData = new HashMap<>();
        updateData.put("userId", user.getId().toString());
        updateData.put("newEmail", email);

        setValue(EMAIL_UPDATE_PREFIX + token, updateData, EXPIRATION_TIME);

        // Send Email to NEW address
        emailService.sendEmailUpdateVerificationEmail(email, token);

        log.info("Email update initiated for userId: {}. New email: {}", user.getId(), email);
    }

    @SuppressWarnings("unchecked")
    public void verifyEmailUpdate(String token) {
        Map<Object, Object> updateData = (Map<Object, Object>) getValue(EMAIL_UPDATE_PREFIX + token);
        if (updateData == null) {
            throw new BadRequestException("Invalid or expired verification link");
        }

        Long userId = Long.parseLong((String) updateData.get("userId"));
        String newEmail = (String) updateData.get("newEmail");

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("User not found"));

        if (userRepository.existsByEmail(newEmail)) {
            throw new BadRequestException("New email is already taken by another user");
        }

        user.setEmail(newEmail);
        userRepository.save(user);

        // Clean up token mapping
        deleteKey(EMAIL_UPDATE_PREFIX + token);

        log.info("Email updated successfully for user ID: {} to {}", userId, newEmail);
    }

    public void initiateRegistration(UserRegisterRequest request) {
        String email = request.getEmail().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException("Email already registered");
        }

        String token = UUID.randomUUID().toString();

        RegistrationSession session = RegistrationSession.builder()
                .registerRequest(request)
                .emailToken(token)
                .emailVerified(false)
                .createdAt(System.currentTimeMillis())
                .build();

        // Store session and token mapping
        setValue(SESSION_KEY_PREFIX + email, session, EXPIRATION_TIME);
        setValue(TOKEN_KEY_PREFIX + token, email, EXPIRATION_TIME);

        // Send Email
        emailService.sendVerificationEmail(email, token);

        log.info("Registration initiated for {}. Token: {}", email, token);
    }

    public void verifyEmail(String token) {
        String email = (String) getValue(TOKEN_KEY_PREFIX + token);
        if (email == null) {
            throw new BadRequestException("Invalid or expired verification link");
        }

        RegistrationSession session = (RegistrationSession) getValue(SESSION_KEY_PREFIX + email);
        if (session == null) {
            throw new BadRequestException("Registration session expired");
        }

        session.setEmailVerified(true);
        // Clean up token mapping immediately
        deleteKey(TOKEN_KEY_PREFIX + token);

        log.info("Email verified for {}", email);

        // Complete registration immediately as additional verification is removed
        completeRegistration(email, session);
    }

    private void completeRegistration(String email, RegistrationSession session) {
        UserRegisterRequest request = session.getRegisterRequest();

        User user = User.builder()
                .email(request.getEmail().toLowerCase())
                .password(passwordEncoder.encode(request.getPassword()))
                .mobileNumber(request.getMobileNumber())
                .role("USER")
                .active(true)
                .verified(true)
                .premium(false)
                .build();

        userRepository.save(user);

        // Generate and set masked Profile ID after getting the database ID
        user.setProfileId(User.generateProfileId(user.getId()));
        userRepository.save(user);

        // Pre-calculate initial matches for the new user
        try {
            matchService.computeAndCacheMatches(user.getId());
        } catch (Exception e) {
            log.error("Failed to compute initial matches for new user {}: {}", user.getEmail(), e.getMessage());
        }

        // Clean up session
        deleteKey(SESSION_KEY_PREFIX + email);

        log.info("Registration completed and user saved: {}", email);
        emailService.sendWelcomeEmail(user);
    }
}
