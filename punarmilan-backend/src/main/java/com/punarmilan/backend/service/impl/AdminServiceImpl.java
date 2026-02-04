package com.punarmilan.backend.service.impl;

import com.punarmilan.backend.dto.*;
import com.punarmilan.backend.entity.*;
import com.punarmilan.backend.exception.ResourceNotFoundException;
import com.punarmilan.backend.repository.*;
import com.punarmilan.backend.service.AdminService;
import com.punarmilan.backend.service.EmailService;
import com.punarmilan.backend.service.NotificationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final PremiumSubscriptionRepository subscriptionRepository;
    private final PaymentTransactionRepository transactionRepository;
    private final NotificationRepository notificationRepository;
    private final MessageRepository messageRepository;
    private final AuditLogRepository auditLogRepository;
    private final NotificationService notificationService;
    private final EmailService emailService;
    private final HttpServletRequest httpServletRequest;

    @Override
    public List<AdminUserDto> getAllUsers() {
        return userRepository.findByRole("USER").stream()
                .map(this::mapToAdminUserDto)
                .collect(Collectors.toList());
    }

    @Override
    public Page<AdminUserDto> getUsersPaginated(Pageable pageable) {
        return userRepository.findByRole("USER", pageable)
                .map(this::mapToAdminUserDto);
    }

    @Override
    public AdminUserDto getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        return mapToAdminUserDto(user);
    }

    @Override
    public Map<String, Object> performUserAction(UserActionRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Map<String, Object> oldValues = new HashMap<>();
        Map<String, Object> newValues = new HashMap<>();

        switch (request.getAction().toUpperCase()) {
            case "BLOCK":
                oldValues.put("active", user.isActive());
                user.setActive(false);
                newValues.put("active", false);
                logAdminAction("USER_BLOCKED", "USER", user.getId(),
                        "User blocked. Reason: " + request.getReason());
                break;

            case "UNBLOCK":
                oldValues.put("active", user.isActive());
                user.setActive(true);
                newValues.put("active", true);
                logAdminAction("USER_UNBLOCKED", "USER", user.getId(),
                        "User unblocked.");
                break;

            case "DELETE":
                oldValues.put("email", user.getEmail());
                oldValues.put("active", user.isActive());
                user.setActive(false);
                userRepository.save(user);
                // Soft delete - mark as inactive
                logAdminAction("USER_DELETED", "USER", user.getId(),
                        "User marked as deleted. Reason: " + request.getReason());
                break;

            case "MAKE_PREMIUM":
                oldValues.put("premium", user.getPremium());
                user.setPremium(true);
                user.setPremiumSince(LocalDateTime.now());
                newValues.put("premium", true);
                newValues.put("premiumSince", LocalDateTime.now());

                // Create manual subscription
                PremiumSubscription subscription = PremiumSubscription.builder()
                        .user(user)
                        .plan(PremiumSubscription.SubscriptionPlan.PREMIUM)
                        .status(PremiumSubscription.SubscriptionStatus.ACTIVE)
                        .paymentProvider("MANUAL_ADMIN")
                        .amount(0.0)
                        .currency("INR")
                        .startDate(LocalDateTime.now())
                        .endDate(LocalDateTime.now().plusMonths(1))
                        .autoRenew(false)
                        .build();
                subscriptionRepository.save(subscription);

                logAdminAction("USER_MADE_PREMIUM", "USER", user.getId(),
                        "User upgraded to premium by admin.");
                break;

            case "REMOVE_PREMIUM":
                oldValues.put("premium", user.getPremium());
                user.setPremium(false);
                user.setPremiumSince(null);
                newValues.put("premium", false);
                newValues.put("premiumSince", null);

                // Cancel all active subscriptions
                subscriptionRepository.findByUserAndStatus(user, PremiumSubscription.SubscriptionStatus.ACTIVE)
                        .forEach(sub -> {
                            sub.setStatus(PremiumSubscription.SubscriptionStatus.CANCELLED);
                            subscriptionRepository.save(sub);
                        });

                logAdminAction("PREMIUM_REMOVED", "USER", user.getId(),
                        "Premium removed from user by admin.");
                break;

            default:
                throw new IllegalArgumentException("Invalid action: " + request.getAction());
        }

        userRepository.save(user);

        // Send notification to user
        notificationService.createNotification(
                user.getId(),
                "SYSTEM_ALERT",
                "Account Updated",
                "Your account has been updated by admin. Action: " + request.getAction(),
                null,
                "ACCOUNT");

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Action performed successfully");
        response.put("userId", user.getId());
        response.put("action", request.getAction());

        return response;
    }

    @Override
    public List<SubscriptionDto> getAllSubscriptions() {
        return subscriptionRepository.findAll().stream()
                .map(this::mapToSubscriptionDto)
                .collect(Collectors.toList());
    }

    @Override
    public SubscriptionDto getSubscriptionById(Long subscriptionId) {
        PremiumSubscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found"));
        return mapToSubscriptionDto(subscription);
    }

    @Override
    public boolean cancelSubscription(Long subscriptionId) {
        PremiumSubscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found"));

        subscription.setStatus(PremiumSubscription.SubscriptionStatus.CANCELLED);
        subscription.setAutoRenew(false);
        subscriptionRepository.save(subscription);

        // Update user premium status
        User user = subscription.getUser();
        long activeSubscriptions = subscriptionRepository.findByUser(user).stream()
                .filter(sub -> sub.getStatus() == PremiumSubscription.SubscriptionStatus.ACTIVE)
                .count();

        if (activeSubscriptions == 0) {
            user.setPremium(false);
            userRepository.save(user);
        }

        logAdminAction("SUBSCRIPTION_CANCELLED", "SUBSCRIPTION", subscriptionId,
                "Subscription cancelled by admin");

        return true;
    }

    @Override
    public boolean updateSubscriptionPlan(Long subscriptionId, String plan) {
        PremiumSubscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found"));

        PremiumSubscription.SubscriptionPlan newPlan;
        try {
            newPlan = PremiumSubscription.SubscriptionPlan.valueOf(plan.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid plan: " + plan);
        }

        subscription.setPlan(newPlan);
        subscriptionRepository.save(subscription);

        logAdminAction("SUBSCRIPTION_UPDATED", "SUBSCRIPTION", subscriptionId,
                "Subscription plan updated to: " + plan);

        return true;
    }

    @Override
    public List<PaymentTransactionDto> getAllTransactions() {
        return transactionRepository.findAll().stream()
                .map(this::mapToPaymentTransactionDto)
                .collect(Collectors.toList());
    }

    @Override
    public Page<PaymentTransactionDto> getTransactionsPaginated(Pageable pageable) {
        return transactionRepository.findAll(pageable)
                .map(this::mapToPaymentTransactionDto);
    }

    @Override
    public boolean refundPayment(Long transactionId) {
        PaymentTransaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));

        transaction.setStatus(PaymentTransaction.PaymentStatus.REFUNDED);
        transactionRepository.save(transaction);

        logAdminAction("PAYMENT_REFUNDED", "TRANSACTION", transactionId,
                "Payment refunded by admin");

        return true;
    }

    @Override
    public AdminStatsDto getDashboardStats() {
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime weekStart = LocalDateTime.of(LocalDate.now().minusDays(7), LocalTime.MIN);
        LocalDateTime monthStart = LocalDateTime.of(LocalDate.now().minusDays(30), LocalTime.MIN);

        long totalUsers = userRepository.count();
        long activeUsers = userRepository.findByActiveTrue().size();
        long premiumUsers = userRepository.findAll().stream()
                .filter(u -> Boolean.TRUE.equals(u.getPremium()))
                .count();

        long newUsersToday = userRepository.findByCreatedAtAfter(todayStart).size();
        long newUsersThisWeek = userRepository.findByCreatedAtAfter(weekStart).size();
        long newUsersThisMonth = userRepository.findByCreatedAtAfter(monthStart).size();

        List<PremiumSubscription> allSubscriptions = subscriptionRepository.findAll();
        long totalSubscriptions = allSubscriptions.size();
        long activeSubscriptions = allSubscriptions.stream()
                .filter(s -> s.getStatus() == PremiumSubscription.SubscriptionStatus.ACTIVE)
                .count();
        long expiredSubscriptions = allSubscriptions.stream()
                .filter(s -> s.getStatus() == PremiumSubscription.SubscriptionStatus.EXPIRED)
                .count();
        double totalRevenue = allSubscriptions.stream()
                .mapToDouble(PremiumSubscription::getAmount)
                .sum();

        List<PaymentTransaction> allTransactions = transactionRepository.findAll();
        long totalTransactions = allTransactions.size();
        long successfulTransactions = allTransactions.stream()
                .filter(t -> t.getStatus() == PaymentTransaction.PaymentStatus.PAID)
                .count();
        long failedTransactions = allTransactions.stream()
                .filter(t -> t.getStatus() == PaymentTransaction.PaymentStatus.FAILED)
                .count();
        double totalTransactionAmount = allTransactions.stream()
                .mapToDouble(PaymentTransaction::getAmount)
                .sum();

        long totalNotifications = notificationRepository.count();
        long unreadNotifications = notificationRepository.countByUserAndReadFalse(null);

        long totalMessages = messageRepository.count();
        long unreadMessages = 0; // You need to implement this based on your message structure

        return AdminStatsDto.builder()
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .premiumUsers(premiumUsers)
                .newUsersToday(newUsersToday)
                .newUsersThisWeek(newUsersThisWeek)
                .newUsersThisMonth(newUsersThisMonth)
                .totalSubscriptions(totalSubscriptions)
                .activeSubscriptions(activeSubscriptions)
                .expiredSubscriptions(expiredSubscriptions)
                .totalRevenue(totalRevenue)
                .totalTransactions(totalTransactions)
                .successfulTransactions(successfulTransactions)
                .failedTransactions(failedTransactions)
                .totalTransactionAmount(totalTransactionAmount)
                .totalNotifications(totalNotifications)
                .unreadNotifications(unreadNotifications)
                .totalMessages(totalMessages)
                .unreadMessages(unreadMessages)
                .build();
    }

    @Override
    public Map<String, Long> getUserGrowthStats(int days) {
        Map<String, Long> stats = new LinkedHashMap<>();
        LocalDate endDate = LocalDate.now();

        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = endDate.minusDays(i);
            LocalDateTime startOfDay = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endOfDay = LocalDateTime.of(date, LocalTime.MAX);

            long count = userRepository.findAll().stream()
                    .filter(user -> {
                        LocalDateTime createdAt = user.getCreatedAt();
                        return createdAt != null &&
                                createdAt.isAfter(startOfDay) &&
                                createdAt.isBefore(endOfDay);
                    })
                    .count();

            stats.put(date.toString(), count);
        }

        return stats;
    }

    @Override
    public Map<String, Double> getRevenueStats(int days) {
        Map<String, Double> stats = new LinkedHashMap<>();
        LocalDate endDate = LocalDate.now();

        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = endDate.minusDays(i);
            LocalDateTime startOfDay = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endOfDay = LocalDateTime.of(date, LocalTime.MAX);

            double revenue = subscriptionRepository.findAll().stream()
                    .filter(sub -> {
                        LocalDateTime createdAt = sub.getCreatedAt();
                        return createdAt != null &&
                                createdAt.isAfter(startOfDay) &&
                                createdAt.isBefore(endOfDay);
                    })
                    .mapToDouble(PremiumSubscription::getAmount)
                    .sum();

            stats.put(date.toString(), revenue);
        }

        return stats;
    }

    @Override
    public Page<AuditLogDto> getAuditLogs(Pageable pageable) {
        return auditLogRepository.findByOrderByCreatedAtDesc(pageable)
                .map(this::mapToAuditLogDto);
    }

    @Override
    public List<AuditLogDto> getAuditLogsByEntity(String entityType, Long entityId) {
        return auditLogRepository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc(entityType, entityId)
                .stream()
                .map(this::mapToAuditLogDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<AuditLogDto> getAuditLogsByUser(String email) {
        return auditLogRepository.findByPerformedByEmailOrderByCreatedAtDesc(email)
                .stream()
                .map(this::mapToAuditLogDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<AuditLogDto> getAuditLogsBetweenDates(LocalDateTime start, LocalDateTime end) {
        return auditLogRepository.findBetweenDates(start, end)
                .stream()
                .map(this::mapToAuditLogDto)
                .collect(Collectors.toList());
    }

    @Override
    @Scheduled(cron = "0 0 3 * * *") // Run daily at 3 AM
    public void cleanupOldData() {
        cleanupOldData(30); // Default to 30 days
    }

    @Override
    public void cleanupOldData(int days) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(days);

        // Cleanup old audit logs
        List<AuditLog> oldLogs = auditLogRepository.findAll().stream()
                .filter(log -> log.getCreatedAt().isBefore(cutoffDate))
                .collect(Collectors.toList());

        auditLogRepository.deleteAll(oldLogs);
        log.info("Cleaned up {} old audit logs", oldLogs.size());
    }

    @Override
    public void sendBulkNotification(List<Long> userIds, String title, String message) {
        for (Long userId : userIds) {
            try {
                notificationService.createNotification(
                        userId,
                        "SYSTEM_ALERT",
                        title,
                        message,
                        null,
                        "ADMIN_NOTIFICATION");
            } catch (Exception e) {
                log.error("Failed to send notification to user {}: {}", userId, e.getMessage());
            }
        }

        logAdminAction("BULK_NOTIFICATION_SENT", "SYSTEM", null,
                "Bulk notification sent to " + userIds.size() + " users");
    }

    @Override
    public void exportData(String dataType, LocalDateTime start, LocalDateTime end) {
        // Implementation for data export (CSV, Excel, etc.)
        // This would typically generate and return a file
        log.info("Exporting {} data from {} to {}", dataType, start, end);
        logAdminAction("DATA_EXPORTED", "SYSTEM", null,
                "Exported " + dataType + " data");
    }

    // Helper Methods
    private void logAdminAction(String action, String entityType, Long entityId, String description) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        String ipAddress = httpServletRequest.getRemoteAddr();
        String userAgent = httpServletRequest.getHeader("User-Agent");

        AuditLog auditLog = AuditLog.builder()
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .performedBy(email)
                .performedByEmail(email)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .createdAt(LocalDateTime.now())
                .build();

        auditLogRepository.save(auditLog);
        log.info("Admin action logged: {} by {}", action, email);
    }

    private AdminUserDto mapToAdminUserDto(User user) {
        return AdminUserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .mobileNumber(user.getMobileNumber())
                .role(user.getRole())
                .isActive(user.isActive())
                .isVerified(user.isVerified())
                .isPremium(user.getPremium())
                .lastLogin(user.getLastLogin())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .premiumSince(user.getPremiumSince())
                .build();
    }

    private SubscriptionDto mapToSubscriptionDto(PremiumSubscription subscription) {
        return SubscriptionDto.builder()
                .id(subscription.getId())
                .userId(subscription.getUser().getId())
                .plan(subscription.getPlan().name())
                .status(subscription.getStatus().name())
                .amount(subscription.getAmount())
                .currency(subscription.getCurrency())
                .startDate(subscription.getStartDate())
                .endDate(subscription.getEndDate())
                .autoRenew(subscription.isAutoRenew())
                .createdAt(subscription.getCreatedAt())
                .build();
    }

    private PaymentTransactionDto mapToPaymentTransactionDto(PaymentTransaction transaction) {
        return PaymentTransactionDto.builder()
                .id(transaction.getId())
                .userId(transaction.getUser().getId())
                .transactionId(transaction.getTransactionId())
                .orderId(transaction.getOrderId())
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .status(transaction.getStatus().name())
                .paymentMethod(transaction.getPaymentMethod() != null ? transaction.getPaymentMethod().name() : null)
                .paymentProvider(transaction.getPaymentProvider())
                .description(transaction.getDescription())
                .createdAt(transaction.getCreatedAt())
                .updatedAt(transaction.getUpdatedAt())
                .build();
    }

    private AuditLogDto mapToAuditLogDto(AuditLog auditLog) {
        return AuditLogDto.builder()
                .id(auditLog.getId())
                .action(auditLog.getAction())
                .entityType(auditLog.getEntityType())
                .entityId(auditLog.getEntityId())
                .performedBy(auditLog.getPerformedBy())
                .performedByEmail(auditLog.getPerformedByEmail())
                .ipAddress(auditLog.getIpAddress())
                .userAgent(auditLog.getUserAgent())
                .oldValues(auditLog.getOldValues())
                .newValues(auditLog.getNewValues())
                .createdAt(auditLog.getCreatedAt())
                .build();
    }

    // =============== USER PROFILE MANAGEMENT ===============

    @Override
    public AdminUserProfileDto getUserProfileById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Profile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User profile not found"));

        return mapToAdminUserProfileDto(user, profile);
    }

    @Override
    public Page<AdminUserProfileDto> getAllUserProfiles(Pageable pageable) {
        return profileRepository.findAll(pageable)
                .map(profile -> mapToAdminUserProfileDto(profile.getUser(), profile));
    }

    @Override
    public Page<AdminUserProfileDto> getProfilesByVerificationStatus(String status, Pageable pageable) {
        return profileRepository
                .findByVerificationStatus(Enum.valueOf(com.punarmilan.backend.entity.Profile.VerificationStatus.class,
                        status.toUpperCase()), pageable)
                .map(profile -> mapToAdminUserProfileDto(profile.getUser(), profile));
    }

    @Override
    public List<AdminUserProfileDto> searchProfiles(String keyword) {
        List<Profile> profiles = profileRepository.findByFullNameContaining(keyword);
        return profiles.stream()
                .map(profile -> mapToAdminUserProfileDto(profile.getUser(), profile))
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> verifyUserProfile(ProfileVerificationRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Profile profile = profileRepository.findByUserId(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User profile not found"));

        String adminEmail = SecurityContextHolder.getContext().getAuthentication().getName();

        Map<String, Object> oldValues = new HashMap<>();
        oldValues.put("verificationStatus",
                profile.getVerificationStatus() != null ? profile.getVerificationStatus().name() : null);
        oldValues.put("verificationNotes", profile.getVerificationNotes());

        profile.setVerificationStatus(Enum.valueOf(com.punarmilan.backend.entity.Profile.VerificationStatus.class,
                request.getStatus().toUpperCase()));
        profile.setVerificationNotes(request.getNotes());
        profile.setVerifiedAt(LocalDateTime.now());
        profile.setVerifiedBy(adminEmail);

        if ("VERIFIED".equalsIgnoreCase(request.getStatus())) {
            user.setVerified(true);
            userRepository.save(user);

            notificationService.createNotification(
                    user.getId(),
                    "VERIFICATION_APPROVED",
                    "Profile Verification Successful!",
                    "Congratulations! Your profile has been verified. You can now enjoy full access to all features.",
                    null,
                    "ACCOUNT");

            // Send email
            emailService.sendVerificationStatusEmail(user, true, null);

        } else if ("REJECTED".equalsIgnoreCase(request.getStatus())) {
            user.setVerified(false);
            userRepository.save(user);

            notificationService.createNotification(
                    user.getId(),
                    "VERIFICATION_REJECTED",
                    "Profile Verification Update",
                    "Your profile verification request has been reviewed. Please check the notes for details and resubmit if needed.",
                    null,
                    "ACCOUNT");

            // Send email
            emailService.sendVerificationStatusEmail(user, false, request.getNotes());
        }

        profileRepository.save(profile);

        Map<String, Object> newValues = new HashMap<>();
        newValues.put("verificationStatus",
                profile.getVerificationStatus() != null ? profile.getVerificationStatus().name() : null);
        newValues.put("verificationNotes", profile.getVerificationNotes());
        newValues.put("verifiedAt", profile.getVerifiedAt());
        newValues.put("verifiedBy", profile.getVerifiedBy());

        // Log the verification action
        logAdminAction("PROFILE_VERIFICATION_" + request.getStatus().toUpperCase(),
                "USER_PROFILE",
                profile.getId(),
                "Profile verification status changed to: " + request.getStatus() +
                        (request.getNotes() != null ? ". Notes: " + request.getNotes() : ""));

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Profile verification updated successfully");
        response.put("userId", user.getId());
        response.put("verificationStatus", request.getStatus());
        response.put("verifiedBy", adminEmail);
        response.put("verifiedAt", LocalDateTime.now());

        return response;
    }

    @Override
    public Map<String, Object> updateProfileByAdmin(ProfileUpdateByAdminRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Profile profile = profileRepository.findByUserId(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User profile not found"));

        Map<String, Object> oldValues = new HashMap<>();
        Map<String, Object> newValues = new HashMap<>();

        // Update profile fields if provided
        if (request.getFullName() != null) {
            oldValues.put("fullName", profile.getFullName());
            profile.setFullName(request.getFullName());
            newValues.put("fullName", request.getFullName());
        }

        if (request.getGender() != null) {
            oldValues.put("gender", profile.getGender());
            profile.setGender(request.getGender());
            newValues.put("gender", request.getGender());
        }

        if (request.getDateOfBirth() != null) {
            oldValues.put("dateOfBirth", profile.getDateOfBirth());
            profile.setDateOfBirth(request.getDateOfBirth());
            newValues.put("dateOfBirth", request.getDateOfBirth());
        }

        if (request.getMaritalStatus() != null) {
            oldValues.put("maritalStatus", profile.getMaritalStatus());
            profile.setMaritalStatus(request.getMaritalStatus());
            newValues.put("maritalStatus", request.getMaritalStatus());
        }

        if (request.getReligion() != null) {
            oldValues.put("religion", profile.getReligion());
            profile.setReligion(request.getReligion());
            newValues.put("religion", request.getReligion());
        }

        if (request.getCaste() != null) {
            oldValues.put("caste", profile.getCaste());
            profile.setCaste(request.getCaste());
            newValues.put("caste", request.getCaste());
        }

        if (request.getSubCaste() != null) {
            oldValues.put("subCaste", profile.getSubCaste());
            profile.setSubCaste(request.getSubCaste());
            newValues.put("subCaste", request.getSubCaste());
        }

        if (request.getOccupation() != null) {
            oldValues.put("occupation", profile.getOccupation());
            profile.setOccupation(request.getOccupation());
            newValues.put("occupation", request.getOccupation());
        }

        if (request.getAnnualIncome() != null) {
            oldValues.put("annualIncome", profile.getAnnualIncome());
            profile.setAnnualIncome(request.getAnnualIncome());
            newValues.put("annualIncome", request.getAnnualIncome());
        }

        if (request.getAddress() != null) {
            oldValues.put("address", profile.getAddress());
            profile.setAddress(request.getAddress());
            newValues.put("address", request.getAddress());
        }

        if (request.getCity() != null) {
            oldValues.put("city", profile.getCity());
            profile.setCity(request.getCity());
            newValues.put("city", request.getCity());
        }

        if (request.getState() != null) {
            oldValues.put("state", profile.getState());
            profile.setState(request.getState());
            newValues.put("state", request.getState());
        }

        if (request.getCountry() != null) {
            oldValues.put("country", profile.getCountry());
            profile.setCountry(request.getCountry());
            newValues.put("country", request.getCountry());
        }

        if (request.getIdProofType() != null) {
            oldValues.put("idProofType", profile.getIdProofType());
            profile.setIdProofType(request.getIdProofType());
            newValues.put("idProofType", request.getIdProofType());
        }

        if (request.getIdProofNumber() != null) {
            oldValues.put("idProofNumber", profile.getIdProofNumber());
            profile.setIdProofNumber(request.getIdProofNumber());
            newValues.put("idProofNumber", request.getIdProofNumber());
        }

        // Update user fields
        if (request.getIsActive() != null) {
            oldValues.put("user.active", user.isActive());
            user.setActive(request.getIsActive());
            newValues.put("user.active", request.getIsActive());
        }

        if (request.getIsPremium() != null) {
            oldValues.put("user.premium", user.getPremium());
            user.setPremium(request.getIsPremium());
            newValues.put("user.premium", request.getIsPremium());

            if (request.getIsPremium()) {
                user.setPremiumSince(LocalDateTime.now());
            } else {
                user.setPremiumSince(null);
            }
        }

        userRepository.save(user);
        profileRepository.save(profile);

        // Log the update
        logAdminAction("PROFILE_UPDATED_BY_ADMIN",
                "USER_PROFILE",
                profile.getId(),
                "Profile updated by admin. Fields updated: " + String.join(", ", newValues.keySet()));

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Profile updated successfully");
        response.put("userId", user.getId());
        response.put("updatedFields", newValues.keySet());

        return response;
    }

    @Override
    public long countPendingVerifications() {
        return profileRepository.countPendingVerifications();
    }

    @Override
    public Map<String, Object> getVerificationStats() {
        Map<String, Object> stats = new HashMap<>();

        long totalProfiles = profileRepository.count();
        long pending = profileRepository.countPendingVerifications();
        long verified = profileRepository.findByVerificationStatus(Profile.VerificationStatus.VERIFIED).size();
        long rejected = profileRepository.findByVerificationStatus(Profile.VerificationStatus.REJECTED).size();

        // Today's stats
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime todayEnd = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);
        long verifiedToday = profileRepository.findVerifiedBetweenDates(todayStart, todayEnd).size();

        // This week stats
        LocalDateTime weekStart = LocalDateTime.of(LocalDate.now().minusDays(7), LocalTime.MIN);
        long verifiedThisWeek = profileRepository.findVerifiedBetweenDates(weekStart, todayEnd).size();

        stats.put("totalProfiles", totalProfiles);
        stats.put("pendingVerification", pending);
        stats.put("verifiedProfiles", verified);
        stats.put("rejectedProfiles", rejected);
        stats.put("verifiedToday", verifiedToday);
        stats.put("verifiedThisWeek", verifiedThisWeek);
        stats.put("verificationRate", totalProfiles > 0 ? (verified * 100.0 / totalProfiles) : 0);

        return stats;
    }

    // Helper method to map User and Profile to DTO
    private AdminUserProfileDto mapToAdminUserProfileDto(User user, Profile profile) {
        return AdminUserProfileDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .mobileNumber(user.getMobileNumber())
                .role(user.getRole())
                .isActive(user.isActive())
                .isVerified(user.isVerified())
                .isPremium(user.getPremium())
                .lastLogin(user.getLastLogin())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .premiumSince(user.getPremiumSince())

                // Profile fields
                .profileId(profile.getId())
                .age(profile.getAge())
                .dateOfBirth(profile.getDateOfBirth())
                .profileComplete(profile.getProfileComplete())
                .weight(profile.getWeight())
                .fullName(profile.getFullName())
                .gender(profile.getGender())
                .maritalStatus(profile.getMaritalStatus())
                .motherTongue(profile.getMotherTongue())
                .address(profile.getAddress())
                .aboutMe(profile.getAboutMe())
                .annualIncome(profile.getAnnualIncome())
                .diet(profile.getDiet())
                .drinkingHabit(profile.getDrinkingHabit())
                .smokingHabit(profile.getSmokingHabit())
                .educationLevel(profile.getEducationLevel())
                .educationField(profile.getEducationField())
                .college(profile.getCollege())
                .occupation(profile.getOccupation())
                .company(profile.getCompany())
                .workingCity(profile.getWorkingCity())
                .religion(profile.getReligion())
                .caste(profile.getCaste())
                .subCaste(profile.getSubCaste())
                .gotra(profile.getGotra())
                .height(profile.getHeight())
                .hobbies(profile.getHobbies())

                // Astro
                .timeOfBirth(profile.getTimeOfBirth())
                .placeOfBirth(profile.getPlaceOfBirth())
                .manglikStatus(profile.getManglikStatus() != null ? profile.getManglikStatus().name() : null)
                .nakshatra(profile.getNakshatra())
                .rashi(profile.getRashi())
                .astroVisibility(
                        profile.getAstroVisibility() != null ? profile.getAstroVisibility().name() : "ALL_MEMBERS")

                .country(profile.getCountry())
                .state(profile.getState())
                .city(profile.getCity())
                .profileCreatedBy(profile.getProfileCreatedBy())
                .profileVisibility(profile.getProfileVisibility())
                .photoCount(profile.getPhotoCount())
                .profilePhotoUrl(profile.getProfilePhotoUrl())
                .photoUrl2(profile.getPhotoUrl2())
                .photoUrl3(profile.getPhotoUrl3())
                .photoUrl4(profile.getPhotoUrl4())
                .photoUrl5(profile.getPhotoUrl5())
                .photoUrl6(profile.getPhotoUrl6())
                .idProofType(profile.getIdProofType())
                .idProofNumber(profile.getIdProofNumber())
                .idProofUrl(profile.getIdProofUrl())
                .verificationStatus(
                        profile.getVerificationStatus() != null ? profile.getVerificationStatus().name() : null)
                .verificationNotes(profile.getVerificationNotes())
                .verifiedAt(profile.getVerifiedAt())
                .verifiedBy(profile.getVerifiedBy())
                .allPhotos(profile.getAllPhotos())
                .build();
    }

    @Override
    public void blastProfile(Long profileUserId) {
        Profile profile = profileRepository.findByUserId(profileUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found"));

        List<User> participants = userRepository.findByActiveTrue();

        for (User user : participants) {
            if (!user.getId().equals(profileUserId)) {
                emailService.sendProfileBlasterEmail(user, profile);
                notificationService.createNotification(
                        user.getId(),
                        "SYSTEM_ALERT",
                        "Featured Profile: " + profile.getFullName(),
                        "Check out this featured profile that matches your interests!",
                        profile.getId(),
                        "PROFILE");
            }
        }

        logAdminAction("PROFILE_BLAST", "USER_PROFILE", profileUserId, "Blasted profile to all active users");
    }
}