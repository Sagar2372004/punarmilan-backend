package com.punarmilan.backend.controller;

import com.punarmilan.backend.dto.*;
import com.punarmilan.backend.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(name = "Payment Management", description = "Handle payments and subscriptions")
public class PaymentController {

    private final PaymentService paymentService;

    @Operation(summary = "Create payment")
    @PostMapping("/create")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<PaymentResponseDto> createPayment(@RequestBody PaymentRequestDto request) {
        PaymentResponseDto response = paymentService.createPayment(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Verify payment")
    @PostMapping("/verify")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Boolean> verifyPayment(@RequestBody PaymentVerificationDto verification) {
        boolean verified = paymentService.verifyPayment(verification);
        return ResponseEntity.ok(verified);
    }

    @Operation(summary = "Create subscription")
    @PostMapping("/subscriptions")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<SubscriptionDto> createSubscription(@RequestBody CreateSubscriptionDto request) {
        // Get current user and create subscription
        return ResponseEntity.ok(null); // Implementation needed
    }

    @Operation(summary = "Cancel subscription")
    @PostMapping("/subscriptions/{subscriptionId}/cancel")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Boolean> cancelSubscription(@PathVariable Long subscriptionId) {
        boolean cancelled = paymentService.cancelSubscription(subscriptionId);
        return ResponseEntity.ok(cancelled);
    }

    @Operation(summary = "Update subscription")
    @PutMapping("/subscriptions/{subscriptionId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Boolean> updateSubscription(
            @PathVariable Long subscriptionId,
            @RequestParam String plan) {
        // Implementation needed
        return ResponseEntity.ok(true);
    }

    @Operation(summary = "Get current subscription")
    @GetMapping("/subscriptions/current")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<SubscriptionDto> getCurrentSubscription() {
        // Get current user ID and fetch subscription
        Long userId = 1L; // Get from security context
        SubscriptionDto subscription = paymentService.getCurrentSubscription(userId);
        return ResponseEntity.ok(subscription);
    }

    @Operation(summary = "Get subscription history")
    @GetMapping("/subscriptions/history")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<SubscriptionDto>> getSubscriptionHistory() {
        Long userId = 1L; // Get from security context
        List<SubscriptionDto> history = paymentService.getSubscriptionHistory(userId);
        return ResponseEntity.ok(history);
    }

    @Operation(summary = "Handle payment webhook")
    @PostMapping("/webhook/{provider}")
    public ResponseEntity<Void> handleWebhook(
            @PathVariable String provider,
            @RequestBody String payload,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {

        boolean handled = paymentService.handleWebhook(payload, signature, provider);
        if (handled) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(summary = "Refund payment")
    @PostMapping("/refund")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Boolean> refundPayment(
            @RequestParam String transactionId,
            @RequestParam(required = false) Double amount) {

        boolean refunded = paymentService.refundPayment(transactionId, amount);
        return ResponseEntity.ok(refunded);
    }
}