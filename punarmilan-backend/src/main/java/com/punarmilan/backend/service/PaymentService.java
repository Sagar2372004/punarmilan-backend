package com.punarmilan.backend.service;

import java.util.List;

import com.punarmilan.backend.dto.*;
import com.punarmilan.backend.entity.PremiumSubscription;
import com.punarmilan.backend.entity.User;

public interface PaymentService {
    
    PaymentResponseDto createPayment(PaymentRequestDto request);
    
    boolean verifyPayment(PaymentVerificationDto verification);
    
    PremiumSubscription createSubscription(CreateSubscriptionDto request, User user);
    
    boolean cancelSubscription(Long subscriptionId);
    
    boolean updateSubscription(Long subscriptionId, PremiumSubscription.SubscriptionPlan newPlan);
    
    SubscriptionDto getCurrentSubscription(Long userId);
    
    List<SubscriptionDto> getSubscriptionHistory(Long userId);
    
    boolean handleWebhook(String payload, String signature, String provider);
    
    String createCustomer(User user);
    
    boolean refundPayment(String transactionId, Double amount);
}