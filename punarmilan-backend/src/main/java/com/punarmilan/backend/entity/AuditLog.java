package com.punarmilan.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String action;

    @Column(name = "entity_type", nullable = false)
    private String entityType;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(name = "performed_by")
    private String performedBy;

    @Column(name = "performed_by_email")
    private String performedByEmail;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "old_values", columnDefinition = "TEXT")
    private String oldValuesJson;

    @Column(name = "new_values", columnDefinition = "TEXT")
    private String newValuesJson;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Helper methods to handle JSON conversion
    public void setOldValues(Map<String, Object> oldValues) {
        this.oldValuesJson = convertMapToJson(oldValues);
    }

    public Map<String, Object> getOldValues() {
        return convertJsonToMap(this.oldValuesJson);
    }

    public void setNewValues(Map<String, Object> newValues) {
        this.newValuesJson = convertMapToJson(newValues);
    }

    public Map<String, Object> getNewValues() {
        return convertJsonToMap(this.newValuesJson);
    }

    private String convertMapToJson(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return "{}";
        }
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(map);
        } catch (Exception e) {
            return "{}";
        }
    }

    private Map<String, Object> convertJsonToMap(String json) {
        if (json == null || json.isEmpty() || json.equals("{}")) {
            return new HashMap<>();
        }
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readValue(json, Map.class);
        } catch (Exception e) {
            return new HashMap<>();
        }
    }
}