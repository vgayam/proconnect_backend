package com.proconnect.service;

import com.proconnect.dto.InquiryRequestDTO;
import com.proconnect.dto.InquiryResponseDTO;
import com.proconnect.entity.BookingInquiry;
import com.proconnect.entity.Professional;
import com.proconnect.exception.ResourceNotFoundException;
import com.proconnect.repository.BookingInquiryRepository;
import com.proconnect.repository.ProfessionalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
        inquiry.setReviewToken(token);
        inquiry.setTokenUsed(false);
        inquiry.setTokenExpiresAt(LocalDateTime.now().plusHours(72));

        bookingInquiryRepository.save(inquiry);

        // TODO: Send review link email when email is provided
        String reviewLink = "/review/" + token;
        log.info("Booking inquiry created for professional {}. Review link: {}", professionalId, reviewLink);
        if (dto.getEmail() != null && !dto.getEmail().isBlank()) {
            log.info("Would send review email to {} with link {}", dto.getEmail(), reviewLink);
        }

        String professionalName = professional.getDisplayName() != null
            ? professional.getDisplayName()
            : professional.getFullName();

        return new InquiryResponseDTO(
            inquiry.getId(),
            token,
            professionalName,
            "Your inquiry has been recorded. You will receive the professional's contact details below."
        );
    }
}
