package com.proconnect.controller;

import com.proconnect.dto.InquiryRequestDTO;
import com.proconnect.dto.InquiryResponseDTO;
import com.proconnect.entity.BookingInquiry;
import com.proconnect.repository.BookingInquiryRepository;
import com.proconnect.service.EmailOtpService;
import com.proconnect.service.InquiryService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inquiries")
@RequiredArgsConstructor
@Slf4j
public class InquiryController {

    private final InquiryService inquiryService;
    private final BookingInquiryRepository bookingInquiryRepository;
    private final EmailOtpService emailOtpService;

    /**
     * Step 0: Send OTP to the booker's email before submitting the booking.
     * POST /api/inquiries/professionals/{id}/request-otp
     */
    @PostMapping("/professionals/{professionalId}/request-otp")
    public ResponseEntity<Map<String, String>> requestBookingOtp(
            @PathVariable Long professionalId,
            @RequestBody Map<String, String> body) {

        String email = body.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email is required"));
        }
        log.info("POST /api/inquiries/professionals/{}/request-otp — email={}", professionalId, email);
        emailOtpService.sendOtp(email);
        return ResponseEntity.ok(Map.of("message", "Verification code sent to " + email));
    }

    /**
     * Step 1: Verify OTP then create the booking inquiry.
     * POST /api/inquiries/professionals/{id}/verify-and-book
     */
    @PostMapping("/professionals/{professionalId}/verify-and-book")
    public ResponseEntity<?> verifyOtpAndBook(
            @PathVariable Long professionalId,
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {

        String email = (String) body.get("email");
        String otp   = (String) body.get("otp");

        if (email == null || otp == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email and OTP are required"));
        }

        boolean valid = emailOtpService.verifyOtp(email, otp);
        if (!valid) {
            return ResponseEntity.status(401).body(Map.of("message", "Invalid or expired verification code"));
        }

        // Build the DTO from the remaining fields
        InquiryRequestDTO dto = new InquiryRequestDTO();
        dto.setName((String) body.get("name"));
        dto.setEmail(email);
        dto.setPhone((String) body.get("phone"));
        dto.setPreferredDate((String) body.get("preferredDate"));
        dto.setPreferredTime((String) body.get("preferredTime"));
        dto.setNote((String) body.get("note"));

        log.info("POST /api/inquiries/professionals/{}/verify-and-book — email={}", professionalId, email);
        InquiryResponseDTO response = inquiryService.createInquiry(professionalId, dto);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/professionals/{professionalId}")
    public ResponseEntity<InquiryResponseDTO> createInquiry(
            @PathVariable Long professionalId,
            @Valid @RequestBody InquiryRequestDTO dto) {

        log.info("POST /api/inquiries/professionals/{} — from={}", professionalId, dto.getName());
        InquiryResponseDTO response = inquiryService.createInquiry(professionalId, dto);
        return ResponseEntity.ok(response);
    }

    /** Returns all booking inquiries for a professional (used by the dashboard). */
    @GetMapping("/professionals/{professionalId}")
    public ResponseEntity<List<BookingInquiry>> getInquiries(@PathVariable Long professionalId) {
        log.info("GET /api/inquiries/professionals/{}", professionalId);
        List<BookingInquiry> inquiries = bookingInquiryRepository.findByProfessionalId(professionalId);
        return ResponseEntity.ok(inquiries);
    }
}
