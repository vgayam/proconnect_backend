package com.proconnect.controller;

import com.proconnect.dto.BookingDTO;
import com.proconnect.dto.InquiryRequestDTO;
import com.proconnect.dto.InquiryResponseDTO;
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
    private final EmailOtpService emailOtpService;
    private final BookingEventService bookingEventService;

    /** Step 0: Send OTP to the booker's email before submitting the booking. */
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

    /** Step 1: Verify OTP then create the booking inquiry. */
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
        if (!emailOtpService.verifyOtp(email, otp)) {
            return ResponseEntity.status(401).body(Map.of("message", "Invalid or expired verification code"));
        }

        InquiryRequestDTO dto = new InquiryRequestDTO();
        dto.setName((String) body.get("name"));
        dto.setEmail(email);
        dto.setPhone((String) body.get("phone"));
        dto.setPreferredDate((String) body.get("preferredDate"));
        dto.setPreferredTime((String) body.get("preferredTime"));
        dto.setNote((String) body.get("note"));
        Object rawLat = body.get("customerLat");
        Object rawLng = body.get("customerLng");
        if (rawLat instanceof Number n) dto.setCustomerLat(n.doubleValue());
        if (rawLng instanceof Number n) dto.setCustomerLng(n.doubleValue());

        log.info("POST /api/inquiries/professionals/{}/verify-and-book — email={}", professionalId, email);
        return ResponseEntity.ok(inquiryService.createInquiry(professionalId, dto));
    }

    /** Legacy direct booking (no OTP) — kept for backwards compatibility. */
    @PostMapping("/professionals/{professionalId}")
    public ResponseEntity<InquiryResponseDTO> createInquiry(
            @PathVariable Long professionalId,
            @Valid @RequestBody InquiryRequestDTO dto) {

        log.info("POST /api/inquiries/professionals/{} — from={}", professionalId, dto.getName());
        return ResponseEntity.ok(inquiryService.createInquiry(professionalId, dto));
    }

    /** Returns all bookings (inquiries + accepted job posts) for a professional. */
    @GetMapping("/professionals/{professionalId}")
    public ResponseEntity<List<BookingDTO>> getInquiries(@PathVariable Long professionalId) {
        log.info("GET /api/inquiries/professionals/{}", professionalId);
        return ResponseEntity.ok(inquiryService.getBookingsForProfessional(professionalId));
    }

    /** Accept / reject / complete a direct booking. */
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {

        String status = body.get("status");
        if (status == null || (!status.equals("ACCEPTED") && !status.equals("REJECTED") && !status.equals("COMPLETED"))) {
            return ResponseEntity.badRequest().body(Map.of("message", "status must be ACCEPTED, REJECTED, or COMPLETED"));
        }
        log.info("PATCH /api/inquiries/{}/status — status={}", id, status);
        return ResponseEntity.ok(inquiryService.updateBookingStatus(id, status));
    }

    /** Cancel a booking via guest cancellation token. */
    @PostMapping("/cancel/{token}")
    public ResponseEntity<?> cancelBooking(@PathVariable String token) {
        log.info("POST /api/inquiries/cancel/{}", token);
        try {
            inquiryService.cancelByToken(token);
            return ResponseEntity.ok(Map.of("message", "Booking cancelled successfully"));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * SSE stream — pro subscribes on dashboard load to receive real-time new-booking events.
     * GET /api/inquiries/professionals/{id}/stream
     */
    @GetMapping(value = "/professionals/{professionalId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamBookings(@PathVariable Long professionalId) {
        log.debug("SSE subscribe — professionalId={}", professionalId);
        return bookingEventService.subscribe(professionalId);
    }
}
