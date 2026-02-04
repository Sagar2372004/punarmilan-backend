package com.punarmilan.backend.config;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Data
@Slf4j
public class PaymentConfig {

    @Value("${razorpay.key.id:}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret:}")
    private String razorpayKeySecret;

    @PostConstruct
    public void init() {
        logConfiguration();
    }

    private void logConfiguration() {
        log.info("=== Payment Configuration ===");

        log.info("Razorpay Key ID configured: {}",
                razorpayKeyId != null && !razorpayKeyId.trim().isEmpty());
        if (razorpayKeyId != null && !razorpayKeyId.trim().isEmpty()) {
            log.info("Razorpay Key ID: {}", razorpayKeyId);
        }

        log.info("Razorpay Key Secret configured: {}",
                razorpayKeySecret != null && !razorpayKeySecret.trim().isEmpty());
    }

    @Bean
    public RazorpayClient razorpayClient() {
        try {
            if (razorpayKeyId == null || razorpayKeyId.trim().isEmpty() ||
                    razorpayKeySecret == null || razorpayKeySecret.trim().isEmpty()) {
                log.error("Razorpay credentials are not properly configured!");
                log.error("Key ID: {}", razorpayKeyId);
                log.error("Key Secret: {}",
                        razorpayKeySecret != null ? "***set***" : "null");
                throw new IllegalArgumentException(
                        "Razorpay credentials not configured. " +
                                "Please set razorpay.key.id and razorpay.key.secret in application.yml");
            }

            log.info("Initializing Razorpay client with Key ID: {}", razorpayKeyId);
            RazorpayClient client = new RazorpayClient(razorpayKeyId, razorpayKeySecret);
            log.info("Razorpay client initialized successfully");
            return client;

        } catch (RazorpayException e) {
            log.error("Failed to initialize Razorpay client: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to initialize Razorpay: " + e.getMessage(), e);
        }
    }
}