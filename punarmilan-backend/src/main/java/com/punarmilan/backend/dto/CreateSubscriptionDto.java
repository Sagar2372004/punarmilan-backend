package com.punarmilan.backend.dto;

import com.punarmilan.backend.entity.PremiumSubscription;

import lombok.Data;

@Data
public class CreateSubscriptionDto {
    private PremiumSubscription.SubscriptionPlan plan;
    private String paymentProvider;
    private Boolean autoRenew;
}
