package com.punarmilan.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "connection_requests",
       indexes = {
           @Index(name = "idx_sender_receiver", columnList = "sender_id, receiver_id"),
           @Index(name = "idx_receiver_status", columnList = "receiver_id, status")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConnectionRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Status status = Status.PENDING;

    @Column(length = 500)
    private String message;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private boolean read = false;

    private LocalDateTime sentAt;
    private LocalDateTime respondedAt;

    @Column(length = 500)
    private String responseMessage;

    // Enum for request status
    public enum Status {
        PENDING,        // Request sent, waiting for response
        ACCEPTED,       // Receiver accepted the request
        REJECTED,       // Receiver rejected the request
        WITHDRAWN,      // Sender withdrew the request
        EXPIRED,        // Request expired (e.g., after 30 days)
        BLOCKED         // Receiver blocked the sender
    }

    @PrePersist
    protected void onCreate() {
        sentAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        if (status != Status.PENDING && respondedAt == null) {
            respondedAt = LocalDateTime.now();
        }
    }

    // Helper methods
    public boolean isActive() {
        return status == Status.PENDING;
    }
    
    public boolean isAccepted() {
        return status == Status.ACCEPTED;
    }
    
    public boolean canWithdraw() {
        return status == Status.PENDING;
    }
}