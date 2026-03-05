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

    @Value("${app.frontend.url:https://proconnect.in}")
    private String frontendUrl;

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

    public void sendBookingConfirmationToClient(String clientEmail, String clientName,
                                                 String professionalName, String slotLabel) {
        if (devMode) {
            log.info("DEV MODE — booking confirmation to client={}, professional={}, slot={}",
                clientEmail, professionalName, slotLabel);
            return;
        }
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(fromAddress);
            msg.setTo(clientEmail);
            msg.setSubject("Booking request sent to " + professionalName + " — ProConnect");
            msg.setText("""
                Hi %s,

                Your booking request has been sent successfully!

                Professional : %s
                Requested slot: %s

                %s will review your request and reach out to confirm.
                You'll hear from them shortly — keep an eye on your email and phone.

                — The ProConnect Team
                """.formatted(clientName, professionalName, slotLabel, professionalName));
            mailSender.send(msg);
            log.info("Booking confirmation sent to client {}", clientEmail);
        } catch (Exception e) {
            log.warn("Failed to send booking confirmation to {}: {}", clientEmail, e.getMessage());
        }
    }

    public void sendBookingNotificationToProfessional(String professionalEmail, String professionalName,
                                                       String clientName, String clientEmail,
                                                       String clientPhone, String clientAddress,
                                                       String slotLabel, String note) {
        if (devMode) {
            log.info("DEV MODE — booking notification to professional={}, from={}, slot={}",
                professionalEmail, clientEmail, slotLabel);
            return;
        }
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(fromAddress);
            msg.setTo(professionalEmail);
            msg.setSubject("New booking request from " + clientName + " — ProConnect");
            msg.setText("""
                Hi %s,

                You have a new booking request on ProConnect!

                Client   : %s
                Email    : %s
                Phone    : %s
                Address  : %s
                Slot     : %s
                Note     : %s

                Log in to your ProConnect dashboard to accept or reject this booking.

                — The ProConnect Team
                """.formatted(
                    professionalName,
                    clientName,
                    clientEmail  != null ? clientEmail  : "—",
                    clientPhone  != null ? clientPhone  : "—",
                    clientAddress != null && !clientAddress.isBlank() ? clientAddress : "—",
                    slotLabel,
                    note != null && !note.isBlank() ? note : "—"
                ));
            mailSender.send(msg);
            log.info("Booking notification sent to professional {}", professionalEmail);
        } catch (Exception e) {
            log.warn("Failed to send booking notification to {}: {}", professionalEmail, e.getMessage());
        }
    }

    public void sendBookingStatusEmail(String clientEmail, String clientName,
                                        String professionalName, String slotLabel, String status) {
        sendBookingStatusEmail(clientEmail, clientName, professionalName, slotLabel, status, null);
    }

    public void sendBookingStatusEmail(String clientEmail, String clientName,
                                        String professionalName, String slotLabel, String status, String cancellationToken) {
        if (devMode) {
            log.info("DEV MODE — booking status email to client={}, status={}", clientEmail, status);
            return;
        }
        boolean accepted = "ACCEPTED".equals(status);
        String cancelLink = (cancellationToken != null && accepted)
            ? frontendUrl.stripTrailing().replaceAll("/+$", "") + "/booking/cancel/" + cancellationToken
            : null;
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(fromAddress);
            msg.setTo(clientEmail);
            msg.setSubject((accepted ? "✅ Booking confirmed" : "❌ Booking declined") + " — ProConnect");
            msg.setText(accepted ? """
                Hi %s,

                Great news! %s has accepted your booking request.

                Slot: %s

                They will contact you shortly to confirm the final details.

                %s

                — The ProConnect Team
                """.formatted(clientName, professionalName, slotLabel,
                    cancelLink != null ? "Need to cancel? Click here:\n" + cancelLink : "")
                : """
                Hi %s,

                Unfortunately, %s is unable to accept your booking for %s.

                You can search for other professionals on ProConnect and book again.

                — The ProConnect Team
                """.formatted(clientName, professionalName, slotLabel));
            mailSender.send(msg);
            log.info("Booking status ({}) email sent to client {}", status, clientEmail);
        } catch (Exception e) {
            log.warn("Failed to send booking status email to {}: {}", clientEmail, e.getMessage());
        }
    }

    public void sendBookingCancelledEmail(String professionalEmail, String professionalName,
                                           String customerName, String slotLabel) {
        if (devMode) {
            log.info("DEV MODE — booking cancelled email to professional={}", professionalEmail);
            return;
        }
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(fromAddress);
            msg.setTo(professionalEmail);
            msg.setSubject("Booking cancelled by customer — ProConnect");
            msg.setText("""
                Hi %s,

                The customer %s has cancelled their booking for %s.

                — The ProConnect Team
                """.formatted(professionalName, customerName, slotLabel));
            mailSender.send(msg);
            log.info("Booking cancelled email sent to professional {}", professionalEmail);
        } catch (Exception e) {
            log.warn("Failed to send cancellation email to {}: {}", professionalEmail, e.getMessage());
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

    // ── Job Post emails ───────────────────────────────────────────────────────

    /** Tell the customer their broadcast job was accepted by a professional. */
    public void sendJobAcceptedToCustomer(String customerEmail, String customerName,
                                          String proName, String proPhone, String proEmail) {
        if (devMode) {
            log.info("DEV MODE — job accepted email to customer={}, pro={}", customerEmail, proName);
            return;
        }
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(fromAddress);
            msg.setTo(customerEmail);
            msg.setSubject("Your job request was accepted — ProConnect");
            msg.setText("""
                Hi %s,

                Great news! A professional has accepted your job request.

                Professional : %s
                Phone        : %s
                Email        : %s

                They will contact you shortly to confirm the details.

                — The ProConnect Team
                """.formatted(
                    customerName, proName,
                    proPhone != null ? proPhone : "—",
                    proEmail != null ? proEmail : "—"
                ));
            mailSender.send(msg);
        } catch (Exception e) {
            log.warn("Failed to send job-accepted email to {}: {}", customerEmail, e.getMessage());
        }
    }

    /** Tell the professional they have been assigned a new broadcast job. */
    public void sendJobAssignedToProfessional(String proEmail, String proName,
                                              String customerName, String customerPhone,
                                              String address, String description) {
        if (devMode) {
            log.info("DEV MODE — job assigned email to pro={}, customer={}", proEmail, customerName);
            return;
        }
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(fromAddress);
            msg.setTo(proEmail);
            msg.setSubject("New job assigned to you — ProConnect");
            msg.setText("""
                Hi %s,

                You have accepted a broadcast job on ProConnect!

                Customer    : %s
                Phone       : %s
                Address     : %s
                Description : %s

                Please contact the customer to arrange the visit.

                — The ProConnect Team
                """.formatted(
                    proName, customerName,
                    customerPhone   != null ? customerPhone   : "—",
                    address         != null ? address         : "—",
                    description
                ));
            mailSender.send(msg);
        } catch (Exception e) {
            log.warn("Failed to send job-assigned email to {}: {}", proEmail, e.getMessage());
        }
    }
}
