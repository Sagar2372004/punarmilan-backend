package com.punarmilan.backend.dto;

import com.punarmilan.backend.entity.PremiumSubscription;
import lombok.Data;

@Data
public class PaymentRequestDto {
    private Long userId;
    private PremiumSubscription.SubscriptionPlan plan;
    private Double amount;
    private String currency;
    private String paymentProvider; // "razorpay"
}
