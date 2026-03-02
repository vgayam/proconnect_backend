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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReviewService — unit tests")
class ReviewServiceTest {

    @Mock ReviewRepository          reviewRepository;
    @Mock BookingInquiryRepository  bookingInquiryRepository;
    @Mock ProfessionalRepository    professionalRepository;

    @InjectMocks ReviewService reviewService;

    private Professional professional;
    private BookingInquiry validInquiry;

    @BeforeEach
    void setUp() {
        professional = new Professional();
        professional.setId(1L);
        professional.setFirstName("Jane");
        professional.setLastName("Doe");
        professional.setDisplayName("Jane Doe");
        professional.setHeadline("Electrician");
        professional.setCity("Mumbai");
        professional.setState("Maharashtra");
        professional.setCountry("India");

        validInquiry = new BookingInquiry();
        validInquiry.setId(10L);
        validInquiry.setProfessional(professional);
        validInquiry.setCustomerName("Alice B.");
        validInquiry.setCustomerEmail("alice@example.com");
        validInquiry.setReviewToken("valid-token-abc");
        validInquiry.setTokenUsed(false);
        validInquiry.setTokenExpiresAt(LocalDateTime.now().plusDays(28));
    }

    // =========================================================================
    // validateToken
    // =========================================================================

    @Test
    @DisplayName("validateToken — valid, unused, not-expired token → valid=true with professional name")
    void validateToken_valid_returnsValidTrue() {
        when(bookingInquiryRepository.findByReviewToken("valid-token-abc"))
                .thenReturn(Optional.of(validInquiry));

        TokenValidationDTO result = reviewService.validateToken("valid-token-abc");

        assertThat(result.isValid()).isTrue();
        assertThat(result.getProfessionalName()).isEqualTo("Jane Doe");
        assertThat(result.getProfessionalId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("validateToken — token not found in DB → valid=false with message")
    void validateToken_notFound_returnsValidFalse() {
        when(bookingInquiryRepository.findByReviewToken("bad-token"))
                .thenReturn(Optional.empty());

        TokenValidationDTO result = reviewService.validateToken("bad-token");

        assertThat(result.isValid()).isFalse();
        assertThat(result.getMessage()).isEqualTo("Invalid review link.");
    }

    @Test
    @DisplayName("validateToken — token already used → valid=false with used message")
    void validateToken_alreadyUsed_returnsValidFalse() {
        validInquiry.setTokenUsed(true);
        when(bookingInquiryRepository.findByReviewToken("used-token"))
                .thenReturn(Optional.of(validInquiry));

        TokenValidationDTO result = reviewService.validateToken("used-token");

        assertThat(result.isValid()).isFalse();
        assertThat(result.getMessage()).contains("already been used");
    }

    @Test
    @DisplayName("validateToken — token expired → valid=false with expired message")
    void validateToken_expired_returnsValidFalse() {
        validInquiry.setTokenExpiresAt(LocalDateTime.now().minusDays(1));
        when(bookingInquiryRepository.findByReviewToken("expired-token"))
                .thenReturn(Optional.of(validInquiry));

        TokenValidationDTO result = reviewService.validateToken("expired-token");

        assertThat(result.isValid()).isFalse();
        assertThat(result.getMessage()).contains("expired");
    }

    // =========================================================================
    // submitReview
    // =========================================================================

    @Test
    @DisplayName("submitReview — valid token → saves review and marks token as used")
    void submitReview_valid_savesReviewAndMarksTokenUsed() {
        when(bookingInquiryRepository.findByReviewToken("valid-token-abc"))
                .thenReturn(Optional.of(validInquiry));
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> {
            Review r = inv.getArgument(0);
            r.setId(1L);
            return r;
        });
        when(reviewRepository.findAverageRatingByProfessionalId(1L)).thenReturn(5.0);
        when(reviewRepository.countByProfessionalId(1L)).thenReturn(1L);
        when(professionalRepository.save(any(Professional.class))).thenReturn(professional);

        ReviewRequestDTO dto = new ReviewRequestDTO((short) 5, "Brilliant!");
        ReviewResponseDTO result = reviewService.submitReview("valid-token-abc", dto);

        assertThat(result.getRating()).isEqualTo((short) 5);
        assertThat(result.getComment()).isEqualTo("Brilliant!");
        assertThat(result.getCustomerName()).isEqualTo("Alice B.");

        // Token must be marked used
        assertThat(validInquiry.isTokenUsed()).isTrue();
        verify(bookingInquiryRepository).save(validInquiry);
    }

    @Test
    @DisplayName("submitReview — already used token → throws IllegalArgumentException")
    void submitReview_alreadyUsed_throwsException() {
        validInquiry.setTokenUsed(true);
        when(bookingInquiryRepository.findByReviewToken("used-token"))
                .thenReturn(Optional.of(validInquiry));

        assertThatThrownBy(() ->
                reviewService.submitReview("used-token", new ReviewRequestDTO((short) 4, "Late")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already been used");

        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    @DisplayName("submitReview — expired token → throws IllegalArgumentException")
    void submitReview_expired_throwsException() {
        validInquiry.setTokenExpiresAt(LocalDateTime.now().minusDays(1));
        when(bookingInquiryRepository.findByReviewToken("expired-token"))
                .thenReturn(Optional.of(validInquiry));

        assertThatThrownBy(() ->
                reviewService.submitReview("expired-token", new ReviewRequestDTO((short) 3, "Old")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expired");

        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    @DisplayName("submitReview — token not found → throws IllegalArgumentException")
    void submitReview_tokenNotFound_throwsException() {
        when(bookingInquiryRepository.findByReviewToken("ghost-token"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                reviewService.submitReview("ghost-token", new ReviewRequestDTO((short) 5, "?")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid review link");
    }

    // =========================================================================
    // getReviewsForProfessional
    // =========================================================================

    @Test
    @DisplayName("getReviewsForProfessional — returns reviews in descending created order")
    void getReviewsForProfessional_returnsReviews() {
        Review r1 = makeReview(1L, "Alice B.", (short) 5, "Great!");
        Review r2 = makeReview(2L, "Bob C.", (short) 4, "Good");
        when(reviewRepository.findByProfessionalIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(r1, r2));

        List<ReviewResponseDTO> results = reviewService.getReviewsForProfessional(1L);

        assertThat(results).hasSize(2);
        assertThat(results.get(0).getCustomerName()).isEqualTo("Alice B.");
        assertThat(results.get(1).getRating()).isEqualTo((short) 4);
    }

    @Test
    @DisplayName("getReviewsForProfessional — no reviews → returns empty list")
    void getReviewsForProfessional_noReviews_returnsEmptyList() {
        when(reviewRepository.findByProfessionalIdOrderByCreatedAtDesc(99L))
                .thenReturn(List.of());

        List<ReviewResponseDTO> results = reviewService.getReviewsForProfessional(99L);

        assertThat(results).isEmpty();
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private Review makeReview(Long id, String name, short rating, String comment) {
        Review r = new Review();
        r.setId(id);
        r.setCustomerName(name);
        r.setRating(rating);
        r.setComment(comment);
        r.setProfessional(professional);
        r.setCreatedAt(LocalDateTime.now());
        return r;
    }
}
