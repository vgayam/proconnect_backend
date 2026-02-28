package com.proconnect.service;

import com.proconnect.dto.ContactMessageDTO;
import com.proconnect.dto.ProfessionalContactDTO;
import com.proconnect.entity.ContactMessage;
import com.proconnect.entity.ContactView;
import com.proconnect.entity.Professional;
import com.proconnect.exception.RateLimitException;
import com.proconnect.exception.ResourceNotFoundException;
import com.proconnect.repository.ContactMessageRepository;
import com.proconnect.repository.ContactViewRepository;
import com.proconnect.repository.ProfessionalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContactService {

    private static final int MAX_VIEWS_PER_24H = 2;

    private final ContactMessageRepository contactMessageRepository;
    private final ProfessionalRepository professionalRepository;
    private final ContactViewRepository contactViewRepository;
    private final EmailOtpService emailOtpService;

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
     * Step 2 — verify OTP, record the view, and return contact details.
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
}
