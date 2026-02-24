package com.proconnect.controller;

import com.proconnect.dto.ReviewRequestDTO;
import com.proconnect.dto.ReviewResponseDTO;
import com.proconnect.dto.TokenValidationDTO;
import com.proconnect.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
public class ReviewController {

    private final ReviewService reviewService;

    /** Validate token and get professional name for the review form */
    @GetMapping("/api/reviews/token/{token}")
    public ResponseEntity<TokenValidationDTO> validateToken(@PathVariable String token) {
        log.info("GET /api/reviews/token/{}", token);
        TokenValidationDTO result = reviewService.validateToken(token);
        return ResponseEntity.ok(result);
    }

    /** Submit a review using a valid token */
    @PostMapping("/api/reviews/token/{token}")
    public ResponseEntity<ReviewResponseDTO> submitReview(
            @PathVariable String token,
            @Valid @RequestBody ReviewRequestDTO dto) {

        log.info("POST /api/reviews/token/{}", token);
        ReviewResponseDTO review = reviewService.submitReview(token, dto);
        return ResponseEntity.ok(review);
    }

    /** Get all reviews for a professional */
    @GetMapping("/api/professionals/{professionalId}/reviews")
    public ResponseEntity<List<ReviewResponseDTO>> getReviews(@PathVariable Long professionalId) {
        List<ReviewResponseDTO> reviews = reviewService.getReviewsForProfessional(professionalId);
        return ResponseEntity.ok(reviews);
    }
}
