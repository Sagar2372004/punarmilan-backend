package com.punarmilan.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationStatsDto {
    private long totalUnread;
    private long totalUnseen;
    private long connectionRequests;
    private long profileViews;
    private long newMatches;
    private long verificationUpdates;
    private long systemAlerts;
}