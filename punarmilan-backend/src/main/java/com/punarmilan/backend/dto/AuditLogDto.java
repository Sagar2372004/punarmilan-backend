package com.punarmilan.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogDto {
    private Long id;
    private String action;
    private String entityType;
    private Long entityId;
    private String performedBy;
    private String performedByEmail;
    private String ipAddress;
    private String userAgent;
    private Map<String, Object> oldValues;
    private Map<String, Object> newValues;
    private LocalDateTime createdAt;
}