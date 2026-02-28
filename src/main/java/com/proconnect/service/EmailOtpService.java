package com.proconnect.service;

import com.proconnect.entity.EmailOtp;
import com.proconnect.repository.EmailOtpRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailOtpService {

    private final EmailOtpRepository emailOtpRepository;
    private final JavaMailSender mailSender;

    @Value("${app.mail.dev-mode:true}")
    private boolean devMode;

    @Value("${app.mail.from:noreply@proconnect.in}")
    private String fromAddress;

    private static final SecureRandom RANDOM = new SecureRandom();
    /** OTP valid for 10 minutes */
    private static final int OTP_TTL_MINUTES = 10;

    @Transactional
    public void sendOtp(String email) {
        String normalizedEmail = email.toLowerCase().trim();

        // Generate code first
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));

        if (devMode) {
            log.info("===== [DEV MODE] OTP for {} => {} (valid {}min) =====", normalizedEmail, code, OTP_TTL_MINUTES);
        } else {
            // Send email BEFORE saving to DB — if email fails, nothing is saved
            sendEmail(normalizedEmail, code);
        }

        // Only reach here if email sent successfully (or dev mode)
        emailOtpRepository.invalidateAll(normalizedEmail);

        EmailOtp otp = new EmailOtp();
        otp.setEmail(normalizedEmail);
        otp.setOtpCode(code);
        otp.setVerified(false);
        otp.setExpiresAt(LocalDateTime.now().plusMinutes(OTP_TTL_MINUTES));
        emailOtpRepository.save(otp);
    }

    @Transactional
    public boolean verifyOtp(String email, String code) {
        return emailOtpRepository
            .findValidOtp(email.toLowerCase().trim(), code.trim())
            .map(otp -> {
                otp.setVerified(true);
                emailOtpRepository.save(otp);
                return true;
            })
            .orElse(false);
    }

    public void sendContactViewedNotification(String professionalEmail,
                                               String professionalName,
                                               String viewerEmail) {
        if (devMode) {
            log.info("DEV MODE — contact view notification skipped: professional={}, viewer={}",
                professionalEmail, viewerEmail);
            return;
        }
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(fromAddress);
            msg.setTo(professionalEmail);
            msg.setSubject("Someone viewed your contact details on ProConnect");
            msg.setText("""
                Hi %s,

                Good news! Someone is interested in your services.

                A user with email %s just viewed your contact details on ProConnect.

                They may reach out to you directly — or you can reach out to them first!

                — The ProConnect Team
                """.formatted(professionalName != null ? professionalName : "there", viewerEmail));
            mailSender.send(msg);
            log.info("Contact view notification sent to professional {}", professionalEmail);
        } catch (Exception e) {
            log.warn("Failed to send contact view notification to {}: {}", professionalEmail, e.getMessage());
            // intentionally not re-throwing — notification failure must not break the main flow
        }
    }

    public void sendReviewRequestEmail(String clientEmail, String professionalName,
                                        String reviewLink) {
        if (devMode) {
            log.info("===== [DEV MODE] Review request for {} => {} =====", clientEmail, reviewLink);
            return;
        }
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(fromAddress);
            msg.setTo(clientEmail);
            msg.setSubject("How was your experience with " + professionalName + "? Leave a review");
            msg.setText("""
                Hi,

                You recently connected with %s on ProConnect.

                If you had a chance to work with them, we'd love to hear about your experience.
                Your review helps others in the community make informed decisions.

                Leave your review here:
                %s

                This link is valid for 30 days and can only be used once.

                — The ProConnect Team
                """.formatted(professionalName, reviewLink));
            mailSender.send(msg);
            log.info("Review request email sent to {} for professional {}", clientEmail, professionalName);
        } catch (Exception e) {
            log.warn("Failed to send review request email to {}: {}", clientEmail, e.getMessage());
            // intentionally not re-throwing — review request failure must not break the main flow
        }
    }

    private void sendEmail(String toEmail, String code) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(fromAddress);
            msg.setTo(toEmail);
            msg.setSubject("Your ProConnect verification code");
            msg.setText("""
                Hi,

                Your verification code is: %s

                This code expires in %d minutes. Do not share it with anyone.

                — The ProConnect Team
                """.formatted(code, OTP_TTL_MINUTES));
            mailSender.send(msg);
            log.info("OTP email sent to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send OTP email to {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Failed to send verification email. Please try again.");
        }
    }
}
