package com.punarmilan.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "transaction_id", nullable = false, unique = true)
    private String transactionId;

    @Column(name = "order_id")
    private String orderId;

    @Column(nullable = false)
    private Double amount;

    @Column(nullable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Enumerated(EnumType.STRING) // ✅ FIXED: Add enum type
    @Column(name = "payment_method")
    private PaymentMethod paymentMethod; // ✅ FIXED: Changed from String to enum

    @Column(name = "payment_provider")
    private String paymentProvider; // STRIPE, RAZORPAY

    @Column(name = "description")
    private String description;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata; // JSON string of additional data

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum PaymentStatus {
        CREATED,
        ATTEMPTED,
        PAID,
        FAILED,
        REFUNDED,
        PARTIALLY_REFUNDED
    }

    // ✅ ADD THIS ENUM for payment method
    public enum PaymentMethod {
        CARD,
        UPI,
        NETBANKING,
        WALLET,
        EMI,
        CASH,
        OTHER
    }
}