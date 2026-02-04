package com.punarmilan.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminStatsDto {
    private long totalUsers;
    private long activeUsers;
    private long premiumUsers;
    private long newUsersToday;
    private long newUsersThisWeek;
    private long newUsersThisMonth;
    
    private long totalSubscriptions;
    private long activeSubscriptions;
    private long expiredSubscriptions;
    private double totalRevenue;
    
    private long totalTransactions;
    private long successfulTransactions;
    private long failedTransactions;
    private double totalTransactionAmount;
    
    private long totalNotifications;
    private long unreadNotifications;
    
    private long totalMessages;
    private long unreadMessages;
}