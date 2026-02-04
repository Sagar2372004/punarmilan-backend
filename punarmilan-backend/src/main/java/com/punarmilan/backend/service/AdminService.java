package com.punarmilan.backend.service;

import com.punarmilan.backend.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface AdminService {

    // User Management
    List<AdminUserDto> getAllUsers();

    Page<AdminUserDto> getUsersPaginated(Pageable pageable);

    AdminUserDto getUserById(Long userId);

    Map<String, Object> performUserAction(UserActionRequest request);

    // User Profile Management
    AdminUserProfileDto getUserProfileById(Long userId);

    Page<AdminUserProfileDto> getAllUserProfiles(Pageable pageable);

    Page<AdminUserProfileDto> getProfilesByVerificationStatus(String status, Pageable pageable);

    List<AdminUserProfileDto> searchProfiles(String keyword);

    // Profile Verification
    Map<String, Object> verifyUserProfile(ProfileVerificationRequest request);

    Map<String, Object> updateProfileByAdmin(ProfileUpdateByAdminRequest request);

    long countPendingVerifications();

    // Subscription Management
    List<SubscriptionDto> getAllSubscriptions();

    SubscriptionDto getSubscriptionById(Long subscriptionId);

    boolean cancelSubscription(Long subscriptionId);

    boolean updateSubscriptionPlan(Long subscriptionId, String plan);

    // Payment Management
    List<PaymentTransactionDto> getAllTransactions();

    Page<PaymentTransactionDto> getTransactionsPaginated(Pageable pageable);

    boolean refundPayment(Long transactionId);

    // Statistics
    AdminStatsDto getDashboardStats();

    Map<String, Long> getUserGrowthStats(int days);

    Map<String, Double> getRevenueStats(int days);

    Map<String, Object> getVerificationStats();

    // Audit Logs
    Page<AuditLogDto> getAuditLogs(Pageable pageable);

    List<AuditLogDto> getAuditLogsByEntity(String entityType, Long entityId);

    List<AuditLogDto> getAuditLogsByUser(String email);

    List<AuditLogDto> getAuditLogsBetweenDates(LocalDateTime start, LocalDateTime end);

    // System Operations
    void cleanupOldData();

    void cleanupOldData(int days);

    void sendBulkNotification(List<Long> userIds, String title, String message);

    void exportData(String dataType, LocalDateTime start, LocalDateTime end);

    void blastProfile(Long profileUserId);
}