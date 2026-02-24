package com.proconnect.service;

import com.proconnect.dto.ReviewRequestDTO;
import com.proconnect.dto.ReviewResponseDTO;
import com.proconnect.dto.TokenValidationDTO;
import com.proconnect.entity.BookingInquiry;
import com.proconnect.entity.Professional;
import com.proconnect.entity.Review;
import com.proconnect.repository.BookingInquiryRepository;
import com.proconnect.repository.ProfessionalRepository;
import com.proconnect.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookingInquiryRepository bookingInquiryRepository;
    private final ProfessionalRepository professionalRepository;

    public TokenValidationDTO validateToken(String token) {
        return bookingInquiryRepository.findByReviewToken(token)
            .map(inquiry -> {
                if (inquiry.isTokenUsed()) {
                    return new TokenValidationDTO(false, null, null, "This review link has already been used.");
                }
                if (inquiry.getTokenExpiresAt().isBefore(LocalDateTime.now())) {
                    return new TokenValidationDTO(false, null, null, "This review link has expired.");
                }
                Professional pro = inquiry.getProfessional();
                String name = pro.getDisplayName() != null ? pro.getDisplayName() : pro.getFullName();
                return new TokenValidationDTO(true, name, pro.getId(), "Token is valid.");
            })
            .orElse(new TokenValidationDTO(false, null, null, "Invalid review link."));
    }

    @Transactional
    public ReviewResponseDTO submitReview(String token, ReviewRequestDTO dto) {
        BookingInquiry inquiry = bookingInquiryRepository.findByReviewToken(token)
            .orElseThrow(() -> new IllegalArgumentException("Invalid review link."));

        if (inquiry.isTokenUsed()) {
            throw new IllegalArgumentException("This review link has already been used.");
        }
        if (inquiry.getTokenExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("This review link has expired.");
        }

        Review review = new Review();
        review.setProfessional(inquiry.getProfessional());
        review.setInquiry(inquiry);
        review.setCustomerName(inquiry.getCustomerName());
        review.setRating(dto.getRating());
        review.setComment(dto.getComment());
        reviewRepository.save(review);

        inquiry.setTokenUsed(true);
        bookingInquiryRepository.save(inquiry);

        updateProfessionalRating(inquiry.getProfessional());

        return new ReviewResponseDTO(
            review.getId(),
            review.getCustomerName(),
            review.getRating(),
            review.getComment(),
            review.getCreatedAt()
        );
    }

    public List<ReviewResponseDTO> getReviewsForProfessional(Long professionalId) {
        return reviewRepository.findByProfessionalIdOrderByCreatedAtDesc(professionalId)
            .stream()
            .map(r -> new ReviewResponseDTO(
                r.getId(),
                r.getCustomerName(),
                r.getRating(),
                r.getComment(),
                r.getCreatedAt()
            ))
            .collect(Collectors.toList());
    }

    private void updateProfessionalRating(Professional professional) {
        Double avgRating = reviewRepository.findAverageRatingByProfessionalId(professional.getId());
        long count = reviewRepository.countByProfessionalId(professional.getId());
        if (avgRating != null) {
            professional.setRating(BigDecimal.valueOf(avgRating).setScale(2, RoundingMode.HALF_UP));
        }
        professional.setReviewCount((int) count);
        professionalRepository.save(professional);
    }
}
