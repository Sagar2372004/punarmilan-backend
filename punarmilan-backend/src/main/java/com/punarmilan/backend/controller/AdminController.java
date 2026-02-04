package com.punarmilan.backend.controller;

import com.punarmilan.backend.dto.*;
import com.punarmilan.backend.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin Management", description = "Admin control panel endpoints")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    // ==================== USER MANAGEMENT ====================

    @Operation(summary = "Get all users")
    @GetMapping("/users")
    public ResponseEntity<List<AdminUserDto>> getAllUsers() {
        List<AdminUserDto> users = adminService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @Operation(summary = "Get users with pagination")
    @GetMapping("/users/paginated")
    public ResponseEntity<Page<AdminUserDto>> getUsersPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Sort.Direction sortDirection = direction.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));

        Page<AdminUserDto> users = adminService.getUsersPaginated(pageable);
        return ResponseEntity.ok(users);
    }

    @Operation(summary = "Get user by ID")
    @GetMapping("/users/{userId}")
    public ResponseEntity<AdminUserDto> getUserById(@PathVariable Long userId) {
        AdminUserDto user = adminService.getUserById(userId);
        return ResponseEntity.ok(user);
    }

    @Operation(summary = "Perform user action (block/unblock/delete/make premium)")
    @PostMapping("/users/actions")
    public ResponseEntity<Map<String, Object>> performUserAction(
            @Valid @RequestBody UserActionRequest request) {
        Map<String, Object> response = adminService.performUserAction(request);
        return ResponseEntity.ok(response);
    }

    // ==================== SUBSCRIPTION MANAGEMENT ====================

    @Operation(summary = "Get all subscriptions")
    @GetMapping("/subscriptions")
    public ResponseEntity<List<SubscriptionDto>> getAllSubscriptions() {
        List<SubscriptionDto> subscriptions = adminService.getAllSubscriptions();
        return ResponseEntity.ok(subscriptions);
    }

    @Operation(summary = "Get subscription by ID")
    @GetMapping("/subscriptions/{subscriptionId}")
    public ResponseEntity<SubscriptionDto> getSubscriptionById(@PathVariable Long subscriptionId) {
        SubscriptionDto subscription = adminService.getSubscriptionById(subscriptionId);
        return ResponseEntity.ok(subscription);
    }

    @Operation(summary = "Cancel subscription")
    @PostMapping("/subscriptions/{subscriptionId}/cancel")
    public ResponseEntity<Boolean> cancelSubscription(@PathVariable Long subscriptionId) {
        boolean cancelled = adminService.cancelSubscription(subscriptionId);
        return ResponseEntity.ok(cancelled);
    }

    @Operation(summary = "Update subscription plan")
    @PutMapping("/subscriptions/{subscriptionId}/plan")
    public ResponseEntity<Boolean> updateSubscriptionPlan(
            @PathVariable Long subscriptionId,
            @RequestParam String plan) {
        boolean updated = adminService.updateSubscriptionPlan(subscriptionId, plan);
        return ResponseEntity.ok(updated);
    }

    // ==================== PAYMENT MANAGEMENT ====================

    @Operation(summary = "Get all transactions")
    @GetMapping("/transactions")
    public ResponseEntity<List<PaymentTransactionDto>> getAllTransactions() {
        List<PaymentTransactionDto> transactions = adminService.getAllTransactions();
        return ResponseEntity.ok(transactions);
    }

    @Operation(summary = "Get transactions with pagination")
    @GetMapping("/transactions/paginated")
    public ResponseEntity<Page<PaymentTransactionDto>> getTransactionsPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Sort.Direction sortDirection = direction.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));

        Page<PaymentTransactionDto> transactions = adminService.getTransactionsPaginated(pageable);
        return ResponseEntity.ok(transactions);
    }

    @Operation(summary = "Refund payment")
    @PostMapping("/transactions/{transactionId}/refund")
    public ResponseEntity<Boolean> refundPayment(@PathVariable Long transactionId) {
        boolean refunded = adminService.refundPayment(transactionId);
        return ResponseEntity.ok(refunded);
    }

    // ==================== DASHBOARD & STATISTICS ====================

    @Operation(summary = "Get dashboard statistics")
    @GetMapping("/stats/dashboard")
    public ResponseEntity<AdminStatsDto> getDashboardStats() {
        AdminStatsDto stats = adminService.getDashboardStats();
        return ResponseEntity.ok(stats);
    }

    @Operation(summary = "Get user growth statistics")
    @GetMapping("/stats/users/growth")
    public ResponseEntity<Map<String, Long>> getUserGrowthStats(
            @RequestParam(defaultValue = "30") int days) {
        Map<String, Long> stats = adminService.getUserGrowthStats(days);
        return ResponseEntity.ok(stats);
    }

    @Operation(summary = "Get revenue statistics")
    @GetMapping("/stats/revenue")
    public ResponseEntity<Map<String, Double>> getRevenueStats(
            @RequestParam(defaultValue = "30") int days) {
        Map<String, Double> stats = adminService.getRevenueStats(days);
        return ResponseEntity.ok(stats);
    }

    // ==================== AUDIT LOGS ====================

    @Operation(summary = "Get audit logs")
    @GetMapping("/audit-logs")
    public ResponseEntity<Page<AuditLogDto>> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<AuditLogDto> logs = adminService.getAuditLogs(pageable);
        return ResponseEntity.ok(logs);
    }

    @Operation(summary = "Get audit logs by entity")
    @GetMapping("/audit-logs/entity/{entityType}/{entityId}")
    public ResponseEntity<List<AuditLogDto>> getAuditLogsByEntity(
            @PathVariable String entityType,
            @PathVariable Long entityId) {

        List<AuditLogDto> logs = adminService.getAuditLogsByEntity(entityType, entityId);
        return ResponseEntity.ok(logs);
    }

    @Operation(summary = "Get audit logs by admin user")
    @GetMapping("/audit-logs/user/{email}")
    public ResponseEntity<List<AuditLogDto>> getAuditLogsByUser(@PathVariable String email) {
        List<AuditLogDto> logs = adminService.getAuditLogsByUser(email);
        return ResponseEntity.ok(logs);
    }

    @Operation(summary = "Get audit logs between dates")
    @GetMapping("/audit-logs/date-range")
    public ResponseEntity<List<AuditLogDto>> getAuditLogsBetweenDates(
            @RequestParam String startDate,
            @RequestParam String endDate) {

        LocalDateTime start = LocalDateTime.parse(startDate);
        LocalDateTime end = LocalDateTime.parse(endDate);

        List<AuditLogDto> logs = adminService.getAuditLogsBetweenDates(start, end);
        return ResponseEntity.ok(logs);
    }

    // ==================== SYSTEM OPERATIONS ====================

    @Operation(summary = "Cleanup old data")
    @PostMapping("/cleanup")
    public ResponseEntity<String> cleanupOldData(@RequestParam(defaultValue = "90") int days) {
        adminService.cleanupOldData(days);
        return ResponseEntity.ok("Cleanup completed successfully");
    }

    @Operation(summary = "Send bulk notification")
    @PostMapping("/notifications/bulk")
    public ResponseEntity<String> sendBulkNotification(
            @RequestParam List<Long> userIds,
            @RequestParam String title,
            @RequestParam String message) {

        adminService.sendBulkNotification(userIds, title, message);
        return ResponseEntity.ok("Bulk notification sent successfully");
    }

    @Operation(summary = "Export data")
    @GetMapping("/export/{dataType}")
    public ResponseEntity<String> exportData(
            @PathVariable String dataType,
            @RequestParam String startDate,
            @RequestParam String endDate) {

        LocalDateTime start = LocalDateTime.parse(startDate);
        LocalDateTime end = LocalDateTime.parse(endDate);

        adminService.exportData(dataType, start, end);
        return ResponseEntity.ok("Export initiated successfully");
    }

    @Operation(summary = "Get user profile by ID")
    @GetMapping("/profiles/{userId}")
    public ResponseEntity<AdminUserProfileDto> getUserProfileById(@PathVariable Long userId) {
        AdminUserProfileDto profile = adminService.getUserProfileById(userId);
        return ResponseEntity.ok(profile);
    }

    @Operation(summary = "Get all user profiles with pagination")
    @GetMapping("/profiles")
    public ResponseEntity<Page<AdminUserProfileDto>> getAllUserProfiles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Sort.Direction sortDirection = direction.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));

        Page<AdminUserProfileDto> profiles = adminService.getAllUserProfiles(pageable);
        return ResponseEntity.ok(profiles);
    }

    @Operation(summary = "Get profiles by verification status")
    @GetMapping("/profiles/verification/{status}")
    public ResponseEntity<Page<AdminUserProfileDto>> getProfilesByVerificationStatus(
            @PathVariable String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<AdminUserProfileDto> profiles = adminService.getProfilesByVerificationStatus(status, pageable);
        return ResponseEntity.ok(profiles);
    }

    @Operation(summary = "Search profiles by name")
    @GetMapping("/profiles/search")
    public ResponseEntity<List<AdminUserProfileDto>> searchProfiles(
            @RequestParam String keyword) {

        List<AdminUserProfileDto> profiles = adminService.searchProfiles(keyword);
        return ResponseEntity.ok(profiles);
    }

    @Operation(summary = "Verify user profile")
    @PostMapping("/profiles/verify")
    public ResponseEntity<Map<String, Object>> verifyUserProfile(
            @Valid @RequestBody ProfileVerificationRequest request) {

        Map<String, Object> response = adminService.verifyUserProfile(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update user profile by admin")
    @PutMapping("/profiles/update")
    public ResponseEntity<Map<String, Object>> updateProfileByAdmin(
            @Valid @RequestBody ProfileUpdateByAdminRequest request) {

        Map<String, Object> response = adminService.updateProfileByAdmin(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get verification statistics")
    @GetMapping("/stats/verifications")
    public ResponseEntity<Map<String, Object>> getVerificationStats() {
        Map<String, Object> stats = adminService.getVerificationStats();
        return ResponseEntity.ok(stats);
    }

    @Operation(summary = "Count pending verifications")
    @GetMapping("/profiles/pending/count")
    public ResponseEntity<Long> countPendingVerifications() {
        long count = adminService.countPendingVerifications();
        return ResponseEntity.ok(count);
    }

    @Operation(summary = "Blast profile to all active users")
    @PostMapping("/profiles/{userId}/blast")
    public ResponseEntity<Void> blastProfile(@PathVariable Long userId) {
        adminService.blastProfile(userId);
        return ResponseEntity.ok().build();
    }
    
}