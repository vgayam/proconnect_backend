package com.proconnect.controller;

import com.proconnect.dto.InquiryRequestDTO;
import com.proconnect.dto.InquiryResponseDTO;
import com.proconnect.entity.BookingInquiry;
import com.proconnect.repository.BookingInquiryRepository;
import com.proconnect.service.InquiryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inquiries")
@RequiredArgsConstructor
@Slf4j
public class InquiryController {

    private final InquiryService inquiryService;
    private final BookingInquiryRepository bookingInquiryRepository;

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
