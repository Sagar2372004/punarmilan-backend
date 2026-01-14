package com.punarmilan.backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConnectionResponseDto {

    private Long id;
    private Long requestId;
    
    // Sender info
    private SenderInfo sender;
    private String senderProfilePhoto;
    
    // Receiver info
    private ReceiverInfo receiver;
    private String receiverProfilePhoto;
    
    private String status; // PENDING, ACCEPTED, REJECTED, WITHDRAWN
    private String message;
    private boolean read;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime sentAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime respondedAt;
    
    private String responseMessage;
    
    // Helper flags for UI
    private boolean canWithdraw;
    private boolean canAccept;
    private boolean canReject;
    private boolean isBlocked;
    private Integer matchScore;
    private String matchPercentage;
    
    // ==================== INNER CLASSES ====================
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResponseRequest {
        private boolean accept;
        private String responseMessage;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SenderInfo {
        private Long id;
        private String email;
        private String fullName;
        private String gender;
        private Integer age;
        private String city;
        private String profilePhotoUrl;
        private boolean isVerified;
        private String occupation;
        private String education;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReceiverInfo {
        private Long id;
        private String email;
        private String fullName;
        private String gender;
        private Integer age;
        private String city;
        private String profilePhotoUrl;
        private boolean isVerified;
        private String occupation;
        private String education;
    }
    
    // Add UserBasicDto as separate class
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserBasicDto {
        private Long id;
        private String email;
        private String fullName;
        private String gender;
        private Integer age;
        private String city;
        private String profilePhotoUrl;
        private boolean isVerified;
    }
}