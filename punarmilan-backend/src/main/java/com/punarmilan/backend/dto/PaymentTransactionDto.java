package com.punarmilan.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentTransactionDto {
    private Long id;
    private Long userId;
    private String userEmail;
    private String transactionId;
    private String orderId;
    private Double amount;
    private String currency;
    private String status;
    private String paymentMethod;
    private String paymentProvider;
    private String description;
    private String metadata;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}