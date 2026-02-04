package com.punarmilan.backend.dto;

import lombok.Data;

@Data
public class PaymentVerificationDto {
    private String paymentId;
    private String orderId;
    private String signature; // For Razorpay
    private String paymentProvider;
}

