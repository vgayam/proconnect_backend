package com.proconnect.controller;

import com.proconnect.dto.BookingDTO;
import com.proconnect.dto.InquiryRequestDTO;
import com.proconnect.dto.InquiryResponseDTO;
import com.proconnect.repository.BookingInquiryRepository;
import com.proconnect.service.BookingEventService;
import com.proconnect.service.EmailOtpService;
import com.proconnect.service.InquiryService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

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
    private final BookingEventService bookingEventService;

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
    public ResponseEntity<List<BookingDTO>> getInquiries(@PathVariable Long professionalId) {
        log.info("GET /api/inquiries/professionals/{}", professionalId);
        List<BookingDTO> inquiries = bookingInquiryRepository
                .findByProfessionalId(professionalId)
                .stream()
                .map(BookingDTO::from)
                .toList();
        return ResponseEntity.ok(inquiries);
    }

    /** Accept or reject a booking — PATCH /api/inquiries/{id}/status  body: {"status":"ACCEPTED"|"REJECTED"} */
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {

        String status = body.get("status");
        if (status == null || (!status.equals("ACCEPTED") && !status.equals("REJECTED"))) {
            return ResponseEntity.badRequest().body(Map.of("message", "status must be ACCEPTED or REJECTED"));
        }
        return bookingInquiryRepository.findById(id)
                .map(b -> {
                    b.setStatus(status);
                    bookingInquiryRepository.save(b);
                    log.info("Booking {} marked as {}", id, status);
                    return ResponseEntity.ok(BookingDTO.from(b));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * SSE stream — GET /api/inquiries/professionals/{id}/stream
     * The professional dashboard subscribes here to receive real-time booking push events.
     * JWT is passed as a query param (?token=...) because EventSource cannot set headers.
     */
    @GetMapping(value = "/professionals/{professionalId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamBookings(@PathVariable Long professionalId) {
        log.info("SSE subscribe — professional {}", professionalId);
        return bookingEventService.subscribe(professionalId);
    }
}
