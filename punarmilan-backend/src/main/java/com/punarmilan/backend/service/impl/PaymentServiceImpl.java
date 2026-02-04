package com.punarmilan.backend.service.impl;

import com.punarmilan.backend.config.PaymentConfig;
import com.punarmilan.backend.dto.*;
import com.punarmilan.backend.entity.*;
import com.punarmilan.backend.exception.ResourceNotFoundException;
import com.punarmilan.backend.repository.PaymentTransactionRepository;
import com.punarmilan.backend.repository.PremiumSubscriptionRepository;
import com.punarmilan.backend.repository.UserRepository;
import com.punarmilan.backend.service.EmailService;
import com.punarmilan.backend.service.NotificationService;
import com.punarmilan.backend.service.PaymentService;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private final PaymentConfig paymentConfig;
    private final RazorpayClient razorpayClient;
    private final PremiumSubscriptionRepository subscriptionRepository;
    private final PaymentTransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final NotificationService notificationService;

    @Override
    public PaymentResponseDto createPayment(PaymentRequestDto request) {
        try {
            User user = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));

            if ("razorpay".equalsIgnoreCase(request.getPaymentProvider())) {
                return createRazorpayPayment(request, user);
            } else {
                throw new IllegalArgumentException("Unsupported payment provider. Only Razorpay is supported.");
            }
        } catch (Exception e) {
            log.error("Error creating payment: ", e);
            throw new RuntimeException("Payment creation failed: " + e.getMessage());
        }
    }

    private PaymentResponseDto createRazorpayPayment(PaymentRequestDto request, User user) throws RazorpayException {
        try {
            log.info("Creating Razorpay payment for user: {}, email: {}, amount: {} {}",
                    user.getId(), user.getEmail(), request.getAmount(), request.getCurrency());

            // Convert amount to paise (smallest currency unit for INR)
            long amountInPaise = (long) (request.getAmount() * 100);

            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountInPaise); // Amount in paise
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "receipt_" + System.currentTimeMillis());

            JSONObject notes = new JSONObject();
            notes.put("userId", user.getId().toString());
            notes.put("plan", request.getPlan().name());
            notes.put("userEmail", user.getEmail());
            orderRequest.put("notes", notes);

            // Add payment capture (auto-capture)
            orderRequest.put("payment_capture", 1); // 1 for auto-capture

            log.debug("Razorpay Order Request: {}", orderRequest.toString());

            Order order = razorpayClient.orders.create(orderRequest);

            log.info("Razorpay Order Created Successfully - Order ID: {}, Amount: {}, Currency: {}",
                    order.get("id"), order.get("amount"), order.get("currency"));

            // Save transaction record
            PaymentTransaction transaction = PaymentTransaction.builder()
                    .user(user)
                    .transactionId(order.get("id"))
                    .orderId(order.get("id"))
                    .amount(request.getAmount())
                    .currency(request.getCurrency())
                    .status(PaymentTransaction.PaymentStatus.CREATED)
                    .paymentProvider("RAZORPAY")
                    .description("Payment for " + getPlanName(request.getPlan()))
                    .build();

            transactionRepository.save(transaction);

            log.info("Transaction saved with ID: {}", transaction.getId());

            PaymentResponseDto response = new PaymentResponseDto();
            response.setOrderId(order.get("id"));
            response.setTransactionId(order.get("id"));
            response.setAmount(request.getAmount());
            response.setCurrency(request.getCurrency());
            response.setStatus("CREATED");
            response.setUserId(user.getId());

            // For Razorpay, return key ID for frontend SDK
            response.setPaymentUrl("https://checkout.razorpay.com/v1/checkout.js?key=" +
                    paymentConfig.getRazorpayKeyId());

            return response;

        } catch (RazorpayException e) {
            log.error("Razorpay API Error - Message: {}", e.getMessage(), e);
            throw new RuntimeException("Razorpay payment creation failed: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error creating Razorpay payment: ", e);
            throw new RuntimeException("Payment creation failed: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean verifyPayment(PaymentVerificationDto verification) {
        try {
            if ("razorpay".equalsIgnoreCase(verification.getPaymentProvider())) {
                return verifyRazorpayPayment(verification);
            }
            return false;
        } catch (Exception e) {
            log.error("Payment verification failed: ", e);
            return false;
        }
    }

    private boolean verifyRazorpayPayment(PaymentVerificationDto verification) throws RazorpayException {
        // Verify signature for Razorpay
        String generatedSignature = generateRazorpaySignature(
                verification.getOrderId() + "|" + verification.getPaymentId(),
                paymentConfig.getRazorpayKeySecret());

        if (generatedSignature.equals(verification.getSignature())) {
            // Get payment details
            com.razorpay.Payment payment = razorpayClient.payments.fetch(verification.getPaymentId());

            if ("captured".equals(payment.get("status"))) {
                // Get transaction
                PaymentTransaction transaction = transactionRepository.findByTransactionId(verification.getOrderId())
                        .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));

                // Update transaction
                transaction.setStatus(PaymentTransaction.PaymentStatus.PAID);

                // Set payment method
                String razorpayMethod = payment.get("method").toString().toUpperCase();
                PaymentTransaction.PaymentMethod method;
                switch (razorpayMethod) {
                    case "CARD":
                        method = PaymentTransaction.PaymentMethod.CARD;
                        break;
                    case "UPI":
                        method = PaymentTransaction.PaymentMethod.UPI;
                        break;
                    case "NETBANKING":
                        method = PaymentTransaction.PaymentMethod.NETBANKING;
                        break;
                    case "WALLET":
                        method = PaymentTransaction.PaymentMethod.WALLET;
                        break;
                    default:
                        method = PaymentTransaction.PaymentMethod.OTHER;
                }
                transaction.setPaymentMethod(method);

                transactionRepository.save(transaction);

                // Get user and plan from notes
                JSONObject notes = payment.get("notes");
                Long userId = Long.parseLong(notes.getString("userId"));
                PremiumSubscription.SubscriptionPlan plan = PremiumSubscription.SubscriptionPlan.valueOf(
                        notes.getString("plan"));

                User user = userRepository.findById(userId)
                        .orElseThrow(() -> new ResourceNotFoundException("User not found"));

                // Create subscription
                createUserSubscription(user, plan, "RAZORPAY", verification.getPaymentId(), verification.getOrderId());

                // Send notifications
                sendPaymentSuccessNotifications(user, transaction);

                return true;
            }
        }
        return false;
    }

    private void createUserSubscription(User user, PremiumSubscription.SubscriptionPlan plan,
            String provider, String paymentId, String orderId) {

        // Calculate end date based on plan
        LocalDateTime endDate = calculateEndDate(plan);

        // Deactivate any existing active subscription
        List<PremiumSubscription> existing = subscriptionRepository.findByUserAndStatus(
                user, PremiumSubscription.SubscriptionStatus.ACTIVE);

        for (PremiumSubscription old : existing) {
            old.setStatus(PremiumSubscription.SubscriptionStatus.CANCELLED);
            subscriptionRepository.save(old);
        }

        // Create new subscription
        PremiumSubscription subscription = PremiumSubscription.builder()
                .user(user)
                .plan(plan)
                .status(PremiumSubscription.SubscriptionStatus.ACTIVE)
                .paymentProvider(provider)
                .paymentId(paymentId)
                .amount(getPlanPrice(plan))
                .currency("INR")
                .startDate(LocalDateTime.now())
                .endDate(endDate)
                .autoRenew(true)
                .build();

        subscriptionRepository.save(subscription);

        // Update user's premium status
        user.setPremium(true);
        user.setPremiumSince(LocalDateTime.now());
        userRepository.save(user);

        log.info("Created premium subscription for user: {}, Plan: {}, End Date: {}",
                user.getEmail(), plan, endDate);
    }

    private LocalDateTime calculateEndDate(PremiumSubscription.SubscriptionPlan plan) {
        LocalDateTime now = LocalDateTime.now();
        switch (plan) {
            case PREMIUM:
                return now.plusMonths(1);
            case PRO:
                return now.plusYears(1);
            case ENTERPRISE:
                return now.plusYears(2);
            default:
                return now.plusMonths(1);
        }
    }

    private Double getPlanPrice(PremiumSubscription.SubscriptionPlan plan) {
        switch (plan) {
            case PREMIUM:
                return 299.0; // ₹299/month
            case PRO:
                return 2999.0; // ₹2999/year
            case ENTERPRISE:
                return 9999.0; // ₹9999/2 years
            default:
                return 0.0;
        }
    }

    private String getPlanName(PremiumSubscription.SubscriptionPlan plan) {
        switch (plan) {
            case PREMIUM:
                return "Premium Monthly";
            case PRO:
                return "Pro Yearly";
            case ENTERPRISE:
                return "Enterprise";
            default:
                return "Basic";
        }
    }

    private String generateRazorpaySignature(String data, String secret) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(new javax.crypto.spec.SecretKeySpec(secret.getBytes(), "HmacSHA256"));
            byte[] bytes = mac.doFinal(data.getBytes());
            StringBuilder hash = new StringBuilder();
            for (byte b : bytes) {
                hash.append(String.format("%02x", b));
            }
            return hash.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error generating signature", e);
        }
    }

    private void sendPaymentSuccessNotifications(User user, PaymentTransaction transaction) {
        // Send email
        emailService.sendPaymentSuccessEmail(user, transaction);

        // Create in-app notification
        notificationService.createNotification(
                user.getId(),
                "SYSTEM_ALERT",
                "Payment Successful",
                "Your payment of " + transaction.getAmount() + " " +
                        transaction.getCurrency() + " was successful",
                transaction.getId(),
                "PAYMENT");

        log.info("Payment success notifications sent for user: {}", user.getEmail());
    }

    @Override
    public PremiumSubscription createSubscription(CreateSubscriptionDto request, User user) {
        // Implementation for recurring subscriptions
        return null; // TODO: Implement
    }

    @Override
    public boolean cancelSubscription(Long subscriptionId) {
        Optional<PremiumSubscription> subscriptionOpt = subscriptionRepository.findById(subscriptionId);
        if (subscriptionOpt.isPresent()) {
            PremiumSubscription subscription = subscriptionOpt.get();
            subscription.setStatus(PremiumSubscription.SubscriptionStatus.CANCELLED);
            subscription.setAutoRenew(false);
            subscriptionRepository.save(subscription);

            // Update user premium status if no other active subscriptions
            User subscriptionUser = subscription.getUser();

            List<PremiumSubscription> userSubscriptions = subscriptionRepository.findByUser(subscriptionUser);
            long activeSubscriptions = userSubscriptions.stream()
                    .filter(sub -> sub.getStatus() == PremiumSubscription.SubscriptionStatus.ACTIVE)
                    .count();

            if (activeSubscriptions == 0) {
                subscriptionUser.setPremium(false);
                userRepository.save(subscriptionUser);
                log.info("User {} premium status set to false", subscriptionUser.getEmail());
            }

            log.info("Cancelled subscription: {} for user: {}", subscriptionId, subscriptionUser.getEmail());
            return true;
        }
        log.warn("Subscription not found with ID: {}", subscriptionId);
        return false;
    }

    @Override
    public boolean updateSubscription(Long subscriptionId, PremiumSubscription.SubscriptionPlan newPlan) {
        Optional<PremiumSubscription> subscriptionOpt = subscriptionRepository.findById(subscriptionId);
        if (subscriptionOpt.isPresent()) {
            PremiumSubscription subscription = subscriptionOpt.get();
            subscription.setPlan(newPlan);
            subscription.setAmount(getPlanPrice(newPlan));
            subscription.setEndDate(calculateEndDate(newPlan));
            subscriptionRepository.save(subscription);
            log.info("Updated subscription {} to plan {}", subscriptionId, newPlan);
            return true;
        }
        return false;
    }

    @Override
    public SubscriptionDto getCurrentSubscription(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<PremiumSubscription> subscriptions = subscriptionRepository.findByUserAndStatus(
                user, PremiumSubscription.SubscriptionStatus.ACTIVE);

        if (!subscriptions.isEmpty()) {
            return mapToSubscriptionDto(subscriptions.get(0));
        }

        return null;
    }

    @Override
    public List<SubscriptionDto> getSubscriptionHistory(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<PremiumSubscription> subscriptions = subscriptionRepository.findByUserOrderByCreatedAtDesc(user);

        return subscriptions.stream()
                .map(this::mapToSubscriptionDto)
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public boolean handleWebhook(String payload, String signature, String provider) {
        try {
            if ("razorpay".equalsIgnoreCase(provider)) {
                return handleRazorpayWebhook(payload, signature);
            }
            return false;
        } catch (Exception e) {
            log.error("Webhook handling failed: ", e);
            return false;
        }
    }

    private boolean handleRazorpayWebhook(String payload, String signature) {
        // Verify Razorpay webhook signature
        String expectedSignature = generateRazorpaySignature(payload, paymentConfig.getRazorpayKeySecret());

        if (expectedSignature.equals(signature)) {
            JSONObject json = new JSONObject(payload);
            String event = json.getString("event");

            switch (event) {
                case "payment.captured":
                    handleRazorpayPaymentCaptured(json);
                    break;
                case "payment.failed":
                    handleRazorpayPaymentFailed(json);
                    break;
                default:
                    log.info("Unhandled Razorpay event: {}", event);
            }
            return true;
        }

        log.warn("Razorpay webhook signature verification failed");
        return false;
    }

    @Override
    public String createCustomer(User user) {
        // Stripe customer creation removed. Razorpay uses user details directly.
        return null;
    }

    @Override
    public boolean refundPayment(String transactionId, Double amount) {
        Optional<PaymentTransaction> transactionOpt = transactionRepository.findByTransactionId(transactionId);
        if (transactionOpt.isPresent()) {
            PaymentTransaction transaction = transactionOpt.get();

            if ("RAZORPAY".equals(transaction.getPaymentProvider())) {
                // TODO: Implement Razorpay refund
                log.warn("Razorpay refund not implemented yet");
                return false;
            }
        }
        return false;
    }

    @Scheduled(cron = "0 0 0 * * *") // Run daily at midnight
    public void checkExpiringSubscriptions() {
        LocalDateTime threeDaysFromNow = LocalDateTime.now().plusDays(3);

        List<PremiumSubscription> expiringSubscriptions = subscriptionRepository
                .findByStatusAndEndDateBefore(
                        PremiumSubscription.SubscriptionStatus.ACTIVE,
                        threeDaysFromNow);

        for (PremiumSubscription subscription : expiringSubscriptions) {
            sendExpiryReminderNotification(subscription);
        }
    }

    @Scheduled(cron = "0 0 1 * * *") // Run daily at 1 AM
    public void expireSubscriptions() {
        List<PremiumSubscription> expiredSubscriptions = subscriptionRepository
                .findByStatusAndEndDateBefore(
                        PremiumSubscription.SubscriptionStatus.ACTIVE,
                        LocalDateTime.now());

        for (PremiumSubscription subscription : expiredSubscriptions) {
            subscription.setStatus(PremiumSubscription.SubscriptionStatus.EXPIRED);
            subscriptionRepository.save(subscription);

            User user = subscription.getUser();
            user.setPremium(false);
            userRepository.save(user);

            sendSubscriptionExpiredNotification(subscription);

            log.info("Expired subscription: {} for user: {}", subscription.getId(), user.getEmail());
        }
    }

    private void sendExpiryReminderNotification(PremiumSubscription subscription) {
        User user = subscription.getUser();

        notificationService.createNotification(
                user.getId(),
                "SYSTEM_ALERT",
                "Subscription Expiring Soon",
                "Your " + subscription.getPlan() + " subscription expires on " +
                        subscription.getEndDate().toLocalDate(),
                subscription.getId(),
                "SUBSCRIPTION");

        emailService.sendSubscriptionExpiryEmail(user, subscription);

        log.info("Sent expiry reminder for subscription: {}", subscription.getId());
    }

    private void sendSubscriptionExpiredNotification(PremiumSubscription subscription) {
        User user = subscription.getUser();

        notificationService.createNotification(
                user.getId(),
                "SYSTEM_ALERT",
                "Subscription Expired",
                "Your " + subscription.getPlan() + " subscription has expired",
                subscription.getId(),
                "SUBSCRIPTION");

        emailService.sendSubscriptionExpiredEmail(user, subscription);

        log.info("Sent expired notification for subscription: {}", subscription.getId());
    }

    private SubscriptionDto mapToSubscriptionDto(PremiumSubscription subscription) {
        SubscriptionDto dto = new SubscriptionDto();
        dto.setId(subscription.getId());
        dto.setUserId(subscription.getUser().getId());
        dto.setPlan(subscription.getPlan().name());
        dto.setStatus(subscription.getStatus().name());
        dto.setAmount(subscription.getAmount());
        dto.setCurrency(subscription.getCurrency());
        dto.setStartDate(subscription.getStartDate());
        dto.setEndDate(subscription.getEndDate());
        dto.setAutoRenew(subscription.isAutoRenew());
        dto.setCreatedAt(subscription.getCreatedAt());
        return dto;
    }

    private void handleRazorpayPaymentCaptured(JSONObject json) {
        log.info("Razorpay payment captured: {}", json);

        try {
            String paymentId = json.getJSONObject("payload").getJSONObject("payment").getJSONObject("entity")
                    .getString("id");
            String orderId = json.getJSONObject("payload").getJSONObject("payment").getJSONObject("entity")
                    .getString("order_id");

            PaymentVerificationDto verification = new PaymentVerificationDto();
            verification.setPaymentId(paymentId);
            verification.setOrderId(orderId);
            verification.setSignature(json.getString("signature"));
            verification.setPaymentProvider("razorpay");

            verifyRazorpayPayment(verification);
        } catch (Exception e) {
            log.error("Error processing Razorpay payment captured webhook", e);
        }
    }

    private void handleRazorpayPaymentFailed(JSONObject json) {
        log.info("Razorpay payment failed: {}", json);
    }
}