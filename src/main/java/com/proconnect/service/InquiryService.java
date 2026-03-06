package com.proconnect.service;

import com.proconnect.dto.BookingDTO;
import com.proconnect.dto.InquiryRequestDTO;
import com.proconnect.dto.InquiryResponseDTO;
import com.proconnect.entity.BookingInquiry;
import com.proconnect.entity.Professional;
import com.proconnect.exception.ResourceNotFoundException;
import com.proconnect.repository.BookingInquiryRepository;
import com.proconnect.repository.JobPostRepository;
import com.proconnect.repository.ProfessionalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InquiryService {

    private final BookingInquiryRepository bookingInquiryRepository;
    private final JobPostRepository jobPostRepository;
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
        String cancellationToken = UUID.randomUUID().toString().replace("-", "");

        BookingInquiry inquiry = new BookingInquiry();
        inquiry.setProfessional(professional);
        inquiry.setCustomerName(dto.getName());
        inquiry.setCustomerEmail(dto.getEmail());
        inquiry.setCustomerPhone(dto.getPhone());
        inquiry.setCustomerAddress(dto.getAddress());
        inquiry.setCustomerLat(dto.getCustomerLat());
        inquiry.setCustomerLng(dto.getCustomerLng());
        inquiry.setServiceId(dto.getServiceId());
        inquiry.setPreferredDate(dto.getPreferredDate());
        inquiry.setPreferredTime(dto.getPreferredTime());
        inquiry.setNote(dto.getNote());
        inquiry.setReviewToken(token);
        inquiry.setCancellationToken(cancellationToken);
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
                dto.getName(), dto.getEmail(), dto.getPhone(), dto.getAddress(), slotLabel, dto.getNote());
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

    /**
     * Returns all direct booking inquiries AND accepted broadcast jobs for a
     * professional, merged and sorted newest-first.
     */
    public List<BookingDTO> getBookingsForProfessional(Long professionalId) {
        List<BookingDTO> list = new ArrayList<>(
            bookingInquiryRepository.findByProfessionalId(professionalId)
                .stream()
                .map(BookingDTO::from)
                .toList()
        );
        jobPostRepository.findByAcceptedByIdOrderByCreatedAtDesc(professionalId)
                .stream()
                .map(BookingDTO::fromJobPost)
                .forEach(list::add);
        list.sort(Comparator.comparing(BookingDTO::getCreatedAt,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return list;
    }

    /**
     * Updates the status of a direct booking inquiry (ACCEPTED / REJECTED / COMPLETED)
     * and sends the appropriate email to the customer.
     */
    @Transactional
    public BookingDTO updateBookingStatus(Long bookingId, String status) {
        BookingInquiry b = bookingInquiryRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));

        b.setStatus(status);
        bookingInquiryRepository.save(b);
        log.info("Booking {} marked as {}", bookingId, status);

        if (b.getCustomerEmail() != null && !b.getCustomerEmail().isBlank()) {
            String proName = b.getProfessional().getDisplayName() != null
                    ? b.getProfessional().getDisplayName()
                    : b.getProfessional().getFullName();
            String slot = buildSlotLabel(b.getPreferredDate(), b.getPreferredTime());

            if ("COMPLETED".equals(status) && b.getReviewToken() != null) {
                String reviewLink = frontendUrl + "/review/" + b.getReviewToken();
                emailOtpService.sendReviewRequestEmail(b.getCustomerEmail(), proName, reviewLink);
            } else if ("ACCEPTED".equals(status)) {
                emailOtpService.sendBookingStatusEmail(
                        b.getCustomerEmail(), b.getCustomerName(), proName, slot, status, b.getCancellationToken());
            } else {
                emailOtpService.sendBookingStatusEmail(
                        b.getCustomerEmail(), b.getCustomerName(), proName, slot, status);
            }
        }
        return BookingDTO.from(b);
    }

    /**
     * Cancels a booking identified by its guest cancellation token.
     * Notifies the professional by email.
     */
    @Transactional
    public void cancelByToken(String token) {
        BookingInquiry b = bookingInquiryRepository.findByCancellationToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Cancellation token not found"));

        if (List.of("CANCELLED", "REJECTED", "COMPLETED").contains(b.getStatus())) {
            throw new IllegalStateException("Booking is already " + b.getStatus());
        }

        b.setStatus("CANCELLED");
        bookingInquiryRepository.save(b);
        log.info("Booking {} cancelled by customer via token", b.getId());

        String slot = buildSlotLabel(b.getPreferredDate(), b.getPreferredTime());
        emailOtpService.sendBookingCancelledEmail(
                b.getProfessional().getEmail(),
                b.getProfessional().getDisplayName(),
                b.getCustomerName(),
                slot);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private String buildSlotLabel(String date, String time) {
        String slot = date != null ? date : "";
        if (time != null && !time.isBlank()) slot += (slot.isBlank() ? "" : " at ") + time;
        return slot;
    }
}
