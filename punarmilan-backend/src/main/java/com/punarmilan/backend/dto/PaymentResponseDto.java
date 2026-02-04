package com.punarmilan.backend.dto;

import lombok.Data;

@Data
public class PaymentResponseDto {
    private String orderId;
    private String transactionId;
    private String paymentUrl;
    private Double amount;
    private String currency;
    private String status;
    private Long subscriptionId;
    private Long userId;
}