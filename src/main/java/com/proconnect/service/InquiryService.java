package com.proconnect.service;

import com.proconnect.dto.BookingDTO;
import com.proconnect.dto.InquiryRequestDTO;
import com.proconnect.dto.InquiryResponseDTO;
import com.proconnect.entity.BookingInquiry;
import com.proconnect.entity.Professional;
import com.proconnect.exception.ResourceNotFoundException;
import com.proconnect.repository.BookingInquiryRepository;
import com.proconnect.repository.ProfessionalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InquiryService {

    private final BookingInquiryRepository bookingInquiryRepository;
    private final ProfessionalRepository professionalRepository;
    private final EmailOtpService emailOtpService;
    private final BookingEventService bookingEventService;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @Transactional
    public InquiryResponseDTO createInquiry(Long professionalId, InquiryRequestDTO dto) {
        if ((dto.getEmail() == null || dto.getEmail().isBlank()) &&
            (dto.getPhone() == null || dto.getPhone().isBlank())) {
            throw new IllegalArgumentException("Either email or phone number is required.");
        }

        Professional professional = professionalRepository.findById(professionalId)
            .orElseThrow(() -> ResourceNotFoundException.professionalNotFound(professionalId));

        String token = UUID.randomUUID().toString().replace("-", "");

        BookingInquiry inquiry = new BookingInquiry();
        inquiry.setProfessional(professional);
        inquiry.setCustomerName(dto.getName());
        inquiry.setCustomerEmail(dto.getEmail());
        inquiry.setCustomerPhone(dto.getPhone());
        inquiry.setPreferredDate(dto.getPreferredDate());
        inquiry.setPreferredTime(dto.getPreferredTime());
        inquiry.setNote(dto.getNote());
        inquiry.setReviewToken(token);
        inquiry.setTokenUsed(false);
        inquiry.setTokenExpiresAt(LocalDateTime.now().plusDays(30));

        bookingInquiryRepository.save(inquiry);

        // ── Push real-time SSE event to professional's dashboard ───────────
        bookingEventService.pushNewBooking(professionalId, BookingDTO.from(inquiry));

        String professionalName = professional.getDisplayName() != null
            ? professional.getDisplayName()
            : professional.getFullName();

        String slotLabel = (dto.getPreferredDate() != null && dto.getPreferredTime() != null)
            ? dto.getPreferredDate() + " at " + dto.getPreferredTime()
            : "a time to be confirmed";

        // ── Notify the client ──────────────────────────────────────────────
        if (dto.getEmail() != null && !dto.getEmail().isBlank()) {
            emailOtpService.sendBookingConfirmationToClient(
                dto.getEmail(), dto.getName(), professionalName, slotLabel);
        }

        // ── Notify the professional ────────────────────────────────────────
        if (professional.getEmail() != null && !professional.getEmail().isBlank()) {
            emailOtpService.sendBookingNotificationToProfessional(
                professional.getEmail(), professionalName,
                dto.getName(), dto.getEmail(), dto.getPhone(), slotLabel, dto.getNote());
        }

        log.info("Booking inquiry created — professional={}, customer={}, slot={}",
            professionalId, dto.getEmail(), slotLabel);

        return new InquiryResponseDTO(
            inquiry.getId(),
            token,
            professionalName,
            "Your booking request has been sent. " + professionalName + " will confirm shortly."
        );
    }
}
