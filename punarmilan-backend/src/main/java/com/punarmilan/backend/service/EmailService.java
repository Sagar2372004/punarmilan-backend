package com.punarmilan.backend.service;

import com.punarmilan.backend.entity.PaymentTransaction;
import com.punarmilan.backend.entity.PremiumSubscription;
import com.punarmilan.backend.entity.User;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

        private final JavaMailSender mailSender;
        private final TemplateEngine templateEngine;
        private final com.punarmilan.backend.repository.ProfileRepository profileRepository;

        @org.springframework.beans.factory.annotation.Value("${app.frontend.url:http://localhost:3000}")
        private String frontendUrl;

        @org.springframework.beans.factory.annotation.Value("${app.base-url:http://localhost:8080}")
        private String baseUrl;

        @org.springframework.beans.factory.annotation.Value("${spring.mail.username:noreply@punarmilan.com}")
        private String fromEmail;

        @Async
        public void sendPaymentSuccessEmail(User user, PaymentTransaction transaction) {
                try {
                        MimeMessage message = mailSender.createMimeMessage();
                        MimeMessageHelper helper = new MimeMessageHelper(message,
                                        MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                                        StandardCharsets.UTF_8.name());

                        Context context = new Context();
                        context.setVariable("user", user);
                        context.setVariable("transaction", transaction);
                        context.setVariable("date", transaction.getCreatedAt()
                                        .format(DateTimeFormatter.ofPattern("dd MMM yyyy")));
                        context.setVariable("frontendUrl", frontendUrl);

                        // Get name from profile or fallback to email part
                        String name = profileRepository.findByUser(user)
                                        .map(com.punarmilan.backend.entity.Profile::getFullName)
                                        .orElse(user.getEmail().split("@")[0]);
                        context.setVariable("username", name);
                        context.setVariable("firstName", name.split(" ")[0]);

                        String html = templateEngine.process("email/payment-success", context);

                        helper.setTo(user.getEmail());
                        helper.setSubject("Payment Successful - Premium Subscription Activated");
                        helper.setText(html, true);
                        helper.setFrom(fromEmail);

                        mailSender.send(message);
                        log.info("Payment success email sent to: {}", user.getEmail());
                } catch (MessagingException e) {
                        log.error("Failed to send payment success email: ", e);
                }
        }

        @Async
        public void sendSubscriptionExpiryEmail(User user, PremiumSubscription subscription) {
                try {
                        MimeMessage message = mailSender.createMimeMessage();
                        MimeMessageHelper helper = new MimeMessageHelper(message,
                                        MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                                        StandardCharsets.UTF_8.name());

                        Context context = new Context();
                        context.setVariable("user", user);
                        context.setVariable("subscription", subscription);
                        context.setVariable("formattedEndDate", subscription.getEndDate()
                                        .format(DateTimeFormatter.ofPattern("dd MMMM yyyy")));
                        context.setVariable("frontendUrl", frontendUrl);

                        // Calculate days remaining
                        long daysRemaining = java.time.Duration.between(java.time.LocalDateTime.now(),
                                        subscription.getEndDate()).toDays();
                        context.setVariable("daysRemaining", daysRemaining < 0 ? 0 : daysRemaining);

                        String name = profileRepository.findByUser(user)
                                        .map(com.punarmilan.backend.entity.Profile::getFullName)
                                        .orElse(user.getEmail().split("@")[0]);
                        context.setVariable("username", name);
                        context.setVariable("firstName", name.split(" ")[0]);

                        String html = templateEngine.process("email/subscription-expiry", context);

                        helper.setTo(user.getEmail());
                        helper.setSubject("Your Premium Subscription is Expiring Soon");
                        helper.setText(html, true);
                        helper.setFrom(fromEmail);

                        mailSender.send(message);
                        log.info("Subscription expiry email sent to: {}", user.getEmail());
                } catch (MessagingException e) {
                        log.error("Failed to send expiry email: ", e);
                }
        }

        @Async
        public void sendSubscriptionExpiredEmail(User user, PremiumSubscription subscription) {
                try {
                        MimeMessage message = mailSender.createMimeMessage();
                        MimeMessageHelper helper = new MimeMessageHelper(message,
                                        MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                                        StandardCharsets.UTF_8.name());

                        Context context = new Context();
                        context.setVariable("user", user);
                        context.setVariable("subscription", subscription);
                        context.setVariable("formattedEndDate", subscription.getEndDate()
                                        .format(DateTimeFormatter.ofPattern("dd MMMM yyyy")));
                        context.setVariable("frontendUrl", frontendUrl);

                        String name = profileRepository.findByUser(user)
                                        .map(com.punarmilan.backend.entity.Profile::getFullName)
                                        .orElse(user.getEmail().split("@")[0]);
                        context.setVariable("username", name);
                        context.setVariable("firstName", name.split(" ")[0]);

                        String html = templateEngine.process("email/subscription-expired", context);

                        helper.setTo(user.getEmail());
                        helper.setSubject("Your Premium Subscription Has Expired");
                        helper.setText(html, true);
                        helper.setFrom(fromEmail);

                        mailSender.send(message);
                        log.info("Subscription expired email sent to: {}", user.getEmail());
                } catch (MessagingException e) {
                        log.error("Failed to send expired email: ", e);
                }
        }

        @Async
        public void sendWelcomeEmail(User user) {
                try {
                        MimeMessage message = mailSender.createMimeMessage();
                        MimeMessageHelper helper = new MimeMessageHelper(message,
                                        MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                                        StandardCharsets.UTF_8.name());

                        Context context = new Context();
                        context.setVariable("user", user);
                        context.setVariable("frontendUrl", frontendUrl);

                        // Use email as fallback if profile not yet committed to DB
                        String name = user.getEmail().split("@")[0];
                        try {
                                name = profileRepository.findByUser(user)
                                                .map(com.punarmilan.backend.entity.Profile::getFullName)
                                                .orElse(user.getEmail().split("@")[0]);
                        } catch (Exception e) {
                                log.warn("Could not fetch profile for welcome email, using email fallback");
                        }

                        context.setVariable("username", name);
                        context.setVariable("firstName", name.split(" ")[0]);

                        String html = templateEngine.process("email/welcome-email", context);

                        helper.setTo(user.getEmail());
                        helper.setSubject("Welcome to Punar Milan!");
                        helper.setText(html, true);
                        helper.setFrom(fromEmail);

                        mailSender.send(message);
                        log.info("Welcome email sent to: {}", user.getEmail());
                } catch (MessagingException e) {
                        log.error("Failed to send welcome email: ", e);
                }
        }

        @Async
        public void sendVerificationEmail(String email, String token) {
                try {
                        MimeMessage message = mailSender.createMimeMessage();
                        MimeMessageHelper helper = new MimeMessageHelper(message,
                                        MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                                        StandardCharsets.UTF_8.name());

                        Context context = new Context();
                        context.setVariable("email", email);
                        context.setVariable("token", token);
                        context.setVariable("verificationUrl",
                                        baseUrl + "/api/auth/verify-email?token=" + token);

                        context.setVariable("frontendUrl", frontendUrl);

                        // Use email as fallback for username
                        String name = email.split("@")[0];
                        context.setVariable("username", name);
                        context.setVariable("firstName", name);

                        String html = templateEngine.process("email/email-verification", context);

                        helper.setTo(email);
                        helper.setSubject("Verify Your Email Address");
                        helper.setText(html, true);
                        helper.setFrom(fromEmail);

                        mailSender.send(message);
                        log.info("Verification email sent to: {}", email);
                } catch (MessagingException e) {
                        log.error("Failed to send verification email: ", e);
                }
        }

        @Async
        public void sendEmailUpdateVerificationEmail(String email, String token) {
                try {
                        MimeMessage message = mailSender.createMimeMessage();
                        MimeMessageHelper helper = new MimeMessageHelper(message,
                                        MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                                        StandardCharsets.UTF_8.name());

                        Context context = new Context();
                        context.setVariable("email", email);
                        context.setVariable("token", token);
                        context.setVariable("verificationUrl",
                                        baseUrl + "/api/auth/verify-email-update?token=" + token);

                        context.setVariable("frontendUrl", frontendUrl);

                        String name = email.split("@")[0];
                        context.setVariable("username", name);
                        context.setVariable("firstName", name);

                        String html = templateEngine.process("email/email-verification", context); // We can reuse the
                                                                                                   // same template or
                                                                                                   // create a new one

                        helper.setTo(email);
                        helper.setSubject("Verify Your New Email Address");
                        helper.setText(html, true);
                        helper.setFrom(fromEmail);

                        mailSender.send(message);
                        log.info("Email update verification email sent to: {}", email);
                } catch (MessagingException e) {
                        log.error("Failed to send email update verification email: ", e);
                }
        }

        @Async
        public void sendPasswordResetEmail(User user, String token) {
                try {
                        MimeMessage message = mailSender.createMimeMessage();
                        MimeMessageHelper helper = new MimeMessageHelper(message,
                                        MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                                        StandardCharsets.UTF_8.name());

                        Context context = new Context();
                        context.setVariable("user", user);
                        context.setVariable("token", token);
                        context.setVariable("resetUrl",
                                        frontendUrl + "/reset-password?token=" + token);
                        context.setVariable("frontendUrl", frontendUrl);

                        String name = profileRepository.findByUser(user)
                                        .map(com.punarmilan.backend.entity.Profile::getFullName)
                                        .orElse(user.getEmail().split("@")[0]);
                        context.setVariable("username", name);
                        context.setVariable("firstName", name.split(" ")[0]);

                        String html = templateEngine.process("email/password-reset", context);

                        helper.setTo(user.getEmail());
                        helper.setSubject("Reset Your Password");
                        helper.setText(html, true);
                        helper.setFrom(fromEmail);

                        mailSender.send(message);
                        log.info("Password reset email sent to: {}", user.getEmail());
                } catch (MessagingException e) {
                        log.error("Failed to send password reset email: ", e);
                }
        }

        @Async
        public void sendConnectionRequestEmail(User sender, User receiver) {
                try {
                        MimeMessage message = mailSender.createMimeMessage();
                        MimeMessageHelper helper = new MimeMessageHelper(message,
                                        MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                                        StandardCharsets.UTF_8.name());

                        Context context = new Context();
                        context.setVariable("frontendUrl", frontendUrl);

                        // Fetch Profiles
                        com.punarmilan.backend.entity.Profile senderProfile = profileRepository.findByUser(sender)
                                        .orElse(null);
                        com.punarmilan.backend.entity.Profile receiverProfile = profileRepository.findByUser(receiver)
                                        .orElse(null);

                        // Map variables for template
                        Map<String, Object> senderMap = new HashMap<>();
                        String senderFullName = (senderProfile != null && senderProfile.getFullName() != null)
                                        ? senderProfile.getFullName()
                                        : sender.getEmail().split("@")[0];
                        String[] senderNames = senderFullName.split(" ");
                        senderMap.put("id", sender.getId());
                        senderMap.put("firstName", senderNames[0]);
                        senderMap.put("lastName", senderNames.length > 1 ? senderNames[1] : "");
                        senderMap.put("gender", senderProfile != null ? senderProfile.getGender() : "UNKNOWN");
                        senderMap.put("age", senderProfile != null ? senderProfile.getAge() : "N/A");
                        senderMap.put("profession",
                                        senderProfile != null ? senderProfile.getOccupation() : "Profession");
                        senderMap.put("education",
                                        senderProfile != null ? senderProfile.getEducationLevel() : "Education");
                        senderMap.put("city", senderProfile != null ? senderProfile.getCity() : "City");
                        senderMap.put("verified", sender.isVerified());
                        senderMap.put("height", senderProfile != null ? senderProfile.getHeight() : "N/A");
                        senderMap.put("connectionCount", 10); // Placeholder

                        Map<String, Object> receiverMap = new HashMap<>();
                        String receiverFullName = (receiverProfile != null && receiverProfile.getFullName() != null)
                                        ? receiverProfile.getFullName()
                                        : receiver.getEmail().split("@")[0];
                        receiverMap.put("firstName", receiverFullName.split(" ")[0]);

                        context.setVariable("sender", senderMap);
                        context.setVariable("receiver", receiverMap);
                        context.setVariable("compatibilityPercentage", 85); // Placeholder
                        context.setVariable("commonInterests", "Interests"); // Placeholder
                        context.setVariable("expiryDays", 7);

                        context.setVariable("profileUrl", frontendUrl + "/profile/" + sender.getId());
                        context.setVariable("senderUsername", senderFullName);
                        context.setVariable("receiverUsername", receiverFullName);

                        String html = templateEngine.process("email/connection-request", context);

                        helper.setTo(receiver.getEmail());
                        helper.setSubject("New Connection Request from " + senderFullName);
                        helper.setText(html, true);
                        helper.setFrom(fromEmail);

                        mailSender.send(message);
                        log.info("Connection request email sent to: {}", receiver.getEmail());
                } catch (MessagingException e) {
                        log.error("Failed to send connection request email: ", e);
                }
        }

        @Async
        public void sendMessageNotificationEmail(User sender, User receiver,
                        com.punarmilan.backend.entity.Message messageEntity) {
                try {
                        MimeMessage message = mailSender.createMimeMessage();
                        MimeMessageHelper helper = new MimeMessageHelper(message,
                                        MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                                        StandardCharsets.UTF_8.name());

                        Context context = new Context();
                        context.setVariable("frontendUrl", frontendUrl);
                        context.setVariable("message", messageEntity);

                        // Format date and time for template
                        context.setVariable("messageDate",
                                        messageEntity.getCreatedAt()
                                                        .format(DateTimeFormatter.ofPattern("dd MMMM yyyy")));
                        context.setVariable("messageTime",
                                        messageEntity.getCreatedAt().format(DateTimeFormatter.ofPattern("hh:mm a")));

                        // Limit preview if needed for template text parts
                        String content = messageEntity.getContent();
                        context.setVariable("messagePreview",
                                        content.length() > 100 ? content.substring(0, 100) + "..." : content);

                        // Fetch Profiles
                        com.punarmilan.backend.entity.Profile senderProfile = profileRepository.findByUser(sender)
                                        .orElse(null);
                        com.punarmilan.backend.entity.Profile receiverProfile = profileRepository.findByUser(receiver)
                                        .orElse(null);

                        // Map variables for template
                        Map<String, Object> senderMap = new HashMap<>();
                        String senderFullName = (senderProfile != null && senderProfile.getFullName() != null)
                                        ? senderProfile.getFullName()
                                        : sender.getEmail().split("@")[0];
                        String[] senderNames = senderFullName.split(" ");
                        senderMap.put("id", sender.getId());
                        senderMap.put("firstName", senderNames[0]);
                        senderMap.put("lastName", senderNames.length > 1 ? senderNames[1] : "");
                        senderMap.put("gender", senderProfile != null ? senderProfile.getGender() : "UNKNOWN");
                        senderMap.put("age", senderProfile != null ? senderProfile.getAge() : "N/A");
                        senderMap.put("profession",
                                        senderProfile != null ? senderProfile.getOccupation() : "Profession");
                        senderMap.put("city", senderProfile != null ? senderProfile.getCity() : "City");
                        senderMap.put("verified", sender.isVerified());

                        Map<String, Object> receiverMap = new HashMap<>();
                        String receiverFullName = (receiverProfile != null && receiverProfile.getFullName() != null)
                                        ? receiverProfile.getFullName()
                                        : receiver.getEmail().split("@")[0];
                        receiverMap.put("firstName", receiverFullName.split(" ")[0]);

                        context.setVariable("sender", senderMap);
                        context.setVariable("receiver", receiverMap);
                        context.setVariable("messageCount", 5); // Placeholder
                        context.setVariable("unreadCount", 1); // Placeholder
                        context.setVariable("compatibilityPercentage", 85); // Placeholder
                        context.setVariable("chatUrl", frontendUrl + "/messages/" + sender.getId());
                        context.setVariable("senderUsername", senderFullName);
                        context.setVariable("receiverUsername", receiverFullName);

                        String html = templateEngine.process("email/message-notification", context);

                        helper.setTo(receiver.getEmail());
                        helper.setSubject("New Message from " + senderFullName);
                        helper.setText(html, true);
                        helper.setFrom(fromEmail);

                        mailSender.send(message);
                        log.info("Message notification email sent to: {}", receiver.getEmail());
                } catch (MessagingException e) {
                        log.error("Failed to send message notification email: ", e);
                }
        }

        @Async
        public void sendVerificationStatusEmail(User user, boolean approved, String reason) {
                try {
                        MimeMessage message = mailSender.createMimeMessage();
                        MimeMessageHelper helper = new MimeMessageHelper(message,
                                        MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                                        StandardCharsets.UTF_8.name());

                        Context context = new Context();
                        context.setVariable("user", user);
                        context.setVariable("approved", approved);
                        context.setVariable("reason", reason);
                        context.setVariable("frontendUrl", frontendUrl);

                        String name = profileRepository.findByUser(user)
                                        .map(com.punarmilan.backend.entity.Profile::getFullName)
                                        .orElse(user.getEmail().split("@")[0]);
                        context.setVariable("username", name);
                        context.setVariable("firstName", name.split(" ")[0]);

                        String html = templateEngine.process("email/verification-status", context);

                        String statusStr = approved ? "Verified" : "Update Required";
                        helper.setTo(user.getEmail());
                        helper.setSubject("Profile Verification Status: " + statusStr);
                        helper.setText(html, true);
                        helper.setFrom(fromEmail);

                        mailSender.send(message);
                        log.info("Verification status email sent to: {}", user.getEmail());
                } catch (MessagingException e) {
                        log.error("Failed to send verification status email: ", e);
                }
        }

        @Async
        public void sendShortlistEmail(User sender, User receiver) {
                try {
                        MimeMessage message = mailSender.createMimeMessage();
                        MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());

                        Context context = new Context();
                        context.setVariable("frontendUrl", frontendUrl);

                        com.punarmilan.backend.entity.Profile senderProfile = profileRepository.findByUser(sender)
                                        .orElse(null);
                        String senderName = senderProfile != null ? senderProfile.getFullName()
                                        : sender.getEmail().split("@")[0];

                        context.setVariable("senderName", senderName);
                        context.setVariable("profileUrl", frontendUrl + "/profile/" + sender.getId());

                        String html = templateEngine.process("email/shortlist-alert", context);

                        helper.setTo(receiver.getEmail());
                        helper.setSubject(senderName + " shortlisted your profile!");
                        helper.setText(html, true);
                        helper.setFrom(fromEmail);

                        mailSender.send(message);
                } catch (MessagingException e) {
                        log.error("Failed to send shortlist email", e);
                }
        }

        @Async
        public void sendMatchDigestEmail(User user, java.util.List<com.punarmilan.backend.entity.Profile> matches) {
                try {
                        MimeMessage message = mailSender.createMimeMessage();
                        MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());

                        Context context = new Context();
                        context.setVariable("frontendUrl", frontendUrl);
                        context.setVariable("matches", matches);
                        context.setVariable("user", user);

                        String html = templateEngine.process("email/match-mail", context);

                        helper.setTo(user.getEmail());
                        helper.setSubject("New Matches Found for You - Punar Milan");
                        helper.setText(html, true);
                        helper.setFrom(fromEmail);

                        mailSender.send(message);
                } catch (MessagingException e) {
                        log.error("Failed to send match digest email", e);
                }
        }

        @Async
        public void sendVisitorsDigestEmail(User user, java.util.List<com.punarmilan.backend.entity.Profile> visitors) {
                try {
                        MimeMessage message = mailSender.createMimeMessage();
                        MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());

                        Context context = new Context();
                        context.setVariable("frontendUrl", frontendUrl);
                        context.setVariable("visitors", visitors);
                        context.setVariable("user", user);

                        String html = templateEngine.process("email/recent-visitors", context);

                        helper.setTo(user.getEmail());
                        helper.setSubject("People viewed your profile recently");
                        helper.setText(html, true);
                        helper.setFrom(fromEmail);

                        mailSender.send(message);
                } catch (MessagingException e) {
                        log.error("Failed to send visitors email", e);
                }
        }

        @Async
        public void sendProfileBlasterEmail(User targetUser, com.punarmilan.backend.entity.Profile profile) {
                try {
                        MimeMessage message = mailSender.createMimeMessage();
                        MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());

                        Context context = new Context();
                        context.setVariable("frontendUrl", frontendUrl);
                        context.setVariable("profile", profile);

                        String html = templateEngine.process("email/profile-blaster", context);

                        helper.setTo(targetUser.getEmail());
                        helper.setSubject("Featured Profile of the Day: " + profile.getFullName());
                        helper.setText(html, true);
                        helper.setFrom(fromEmail);

                        mailSender.send(message);
                } catch (MessagingException e) {
                        log.error("Failed to send blaster email", e);
                }
        }
}