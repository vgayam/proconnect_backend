package com.proconnect.service;

import com.proconnect.dto.ContactMessageDTO;
import com.proconnect.dto.ProfessionalContactDTO;
import com.proconnect.entity.BookingInquiry;
import com.proconnect.entity.ContactMessage;
import com.proconnect.entity.ContactView;
import com.proconnect.entity.Professional;
import com.proconnect.exception.RateLimitException;
import com.proconnect.exception.ResourceNotFoundException;
import com.proconnect.repository.BookingInquiryRepository;
import com.proconnect.repository.ContactMessageRepository;
import com.proconnect.repository.ContactViewRepository;
import com.proconnect.repository.ProfessionalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContactService {

    private static final int MAX_VIEWS_PER_24H = 2;
    private static final int REVIEW_TOKEN_EXPIRY_DAYS = 30;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ContactMessageRepository contactMessageRepository;
    private final ProfessionalRepository professionalRepository;
    private final ContactViewRepository contactViewRepository;
    private final BookingInquiryRepository bookingInquiryRepository;
    private final EmailOtpService emailOtpService;

    /** Set NOTIFY_PROFESSIONAL_ON_CONTACT=true in env to enable lead notifications. */
    @Value("${app.contact.notify-professional:false}")
    private boolean notifyProfessional;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @Transactional
    public void sendContactMessage(Long professionalId, ContactMessageDTO dto) {
        Professional professional = professionalRepository.findById(professionalId)
            .orElseThrow(() -> ResourceNotFoundException.professionalNotFound(professionalId));
        
        ContactMessage message = new ContactMessage();
        message.setProfessional(professional);
        message.setSenderName(dto.getName());
        message.setSenderEmail(dto.getEmail());
        message.setSubject(dto.getSubject());
        message.setMessage(dto.getMessage());
        message.setServiceId(dto.getServiceId());
        message.setStatus("NEW");
        
        contactMessageRepository.save(message);
        
        // TODO: Send email notification to professional
    }

    /**
     * Step 1 — rate-check then send OTP to the requester's email.
     */
    public void requestContactOtp(Long professionalId, String viewerEmail, String viewerIp) {
        if (!professionalRepository.existsById(professionalId)) {
            throw new ResourceNotFoundException("Professional not found: " + professionalId);
        }
        checkRateLimit(viewerEmail, viewerIp);
        emailOtpService.sendOtp(viewerEmail);
        log.info("Contact OTP sent to {} for professional {}", viewerEmail, professionalId);
    }

    /**
     * Step 2 — verify OTP, record the view, issue a review token, and return contact details.
     */
    @Transactional
    public ProfessionalContactDTO verifyContactOtp(Long professionalId, String viewerEmail,
                                                    String otp, String viewerIp) {
        checkRateLimit(viewerEmail, viewerIp);

        boolean valid = emailOtpService.verifyOtp(viewerEmail, otp);
        if (!valid) {
            throw new IllegalArgumentException("Invalid or expired verification code");
        }

        Professional professional = professionalRepository.findById(professionalId)
                .orElseThrow(() -> new ResourceNotFoundException("Professional not found: " + professionalId));

        contactViewRepository.save(ContactView.builder()
                .professionalId(professionalId)
                .viewerEmail(viewerEmail)
                .viewerIp(viewerIp)
                .viewedAt(Instant.now())
                .build());

        log.info("Contact details revealed to {} for professional {}", viewerEmail, professionalId);

        // ── Issue a review token so the client can leave a review later ──────
        String reviewToken = generateReviewToken();
        BookingInquiry inquiry = new BookingInquiry();
        inquiry.setProfessional(professional);
        inquiry.setCustomerName(anonymizeName(viewerEmail));
        inquiry.setCustomerEmail(viewerEmail);
        inquiry.setReviewToken(reviewToken);
        inquiry.setTokenUsed(false);
        inquiry.setTokenExpiresAt(LocalDateTime.now().plusDays(REVIEW_TOKEN_EXPIRY_DAYS));
        bookingInquiryRepository.save(inquiry);

        String proName = professional.getDisplayName() != null
                ? professional.getDisplayName() : professional.getFullName();
        String reviewLink = frontendUrl.stripTrailing().replaceAll("/+$", "") + "/review/" + reviewToken;

        emailOtpService.sendReviewRequestEmail(viewerEmail, proName, reviewLink);
        log.info("Review token issued and email sent to {} for professional {}", viewerEmail, professionalId);

        // ── Notify the professional about the lead (feature-flagged, default off) ──
        if (notifyProfessional && professional.getEmail() != null) {
            emailOtpService.sendContactViewedNotification(
                professional.getEmail(),
                proName,
                viewerEmail
            );
        }

        return new ProfessionalContactDTO(
                professional.getEmail(),
                professional.getPhone(),
                professional.getWhatsapp()
        );
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private void checkRateLimit(String viewerEmail, String viewerIp) {
        Instant since = Instant.now().minus(24, ChronoUnit.HOURS);

        if (viewerEmail != null && !viewerEmail.isBlank()) {
            long byEmail = contactViewRepository.countByEmailSince(viewerEmail, since);
            if (byEmail >= MAX_VIEWS_PER_24H) {
                throw new RateLimitException(
                        "You have reached the limit of " + MAX_VIEWS_PER_24H +
                        " contact views per 24 hours for this email address.");
            }
        }

        if (viewerIp != null && !viewerIp.isBlank()) {
            long byIp = contactViewRepository.countByIpSince(viewerIp, since);
            if (byIp >= MAX_VIEWS_PER_24H) {
                throw new RateLimitException(
                        "You have reached the limit of " + MAX_VIEWS_PER_24H +
                        " contact views per 24 hours from this device.");
            }
        }
    }

    private String generateReviewToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes); // 64-char hex string
    }

    /**
     * Derives an anonymized display name from an email address.
     * e.g. "gayamvenkata123@gmail.com" → "Gayamvenkata V."
     *      "john.doe@example.com"     → "John D."
     */
    private String anonymizeName(String email) {
        if (email == null || !email.contains("@")) return "Verified Client";
        String localPart = email.split("@")[0]; // e.g. "gayamvenkata123" or "john.doe"
        // Split on dots, dashes, underscores, digits
        String[] parts = localPart.split("[.\\-_0-9]+");
        if (parts.length == 0 || parts[0].isBlank()) return "Verified Client";

        String firstName = capitalize(parts[0]);
        if (parts.length >= 2 && !parts[1].isBlank()) {
            return firstName + " " + Character.toUpperCase(parts[1].charAt(0)) + ".";
        }
        // Single word — just show first name
        return firstName;
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }
}
