package com.proconnect.controller;

import com.proconnect.dto.ContactOtpRequestDTO;
import com.proconnect.dto.ContactOtpVerifyDTO;
import com.proconnect.dto.ProfessionalContactDTO;
import com.proconnect.service.ContactService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/contact")
@RequiredArgsConstructor
@Slf4j
public class ContactController {

    private final ContactService contactService;

    /**
     * Step 1: Send OTP to the viewer's email.
     * POST /api/contact/professionals/{id}/request-otp
     */
    @PostMapping("/professionals/{professionalId}/request-otp")
    public ResponseEntity<Map<String, String>> requestOtp(
            @PathVariable Long professionalId,
            @Valid @RequestBody ContactOtpRequestDTO dto,
            HttpServletRequest request) {

        String ip = extractIp(request);
        log.info("POST /api/contact/professionals/{}/request-otp — email={}, ip={}", professionalId, dto.getEmail(), ip);
        contactService.requestContactOtp(professionalId, dto.getEmail(), ip);
        return ResponseEntity.ok(Map.of("message", "Verification code sent to " + dto.getEmail()));
    }

    /**
     * Step 2: Verify OTP and return professional contact details.
     * POST /api/contact/professionals/{id}/verify-otp
     */
    @PostMapping("/professionals/{professionalId}/verify-otp")
    public ResponseEntity<ProfessionalContactDTO> verifyOtp(
            @PathVariable Long professionalId,
            @Valid @RequestBody ContactOtpVerifyDTO dto,
            HttpServletRequest request) {

        String ip = extractIp(request);
        log.info("POST /api/contact/professionals/{}/verify-otp — email={}, ip={}", professionalId, dto.getEmail(), ip);
        ProfessionalContactDTO contact = contactService.verifyContactOtp(
                professionalId, dto.getEmail(), dto.getOtp(), ip);
        return ResponseEntity.ok(contact);
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private String extractIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
