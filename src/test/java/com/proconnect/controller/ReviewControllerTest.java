package com.proconnect.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proconnect.dto.ReviewRequestDTO;
import com.proconnect.dto.ReviewResponseDTO;
import com.proconnect.dto.TokenValidationDTO;
import com.proconnect.service.ReviewService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
    value = ReviewController.class,
    excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class}
)
@DisplayName("ReviewController — HTTP layer tests")
class ReviewControllerTest {

    @Autowired WebApplicationContext context;
    @Autowired ObjectMapper objectMapper;
    @MockBean  ReviewService  reviewService;
    @MockBean  com.proconnect.security.JwtAuthFilter jwtAuthFilter;
    @MockBean  com.proconnect.security.JwtService    jwtService;

    private MockMvc mockMvc;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        // Build WITHOUT filters so the JwtAuthFilter mock doesn't swallow requests
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    // =========================================================================
    // GET /api/reviews/token/{token}
    // =========================================================================

    @Test
    @DisplayName("validateToken — valid token → 200 with valid=true and professional name")
    void validateToken_valid_returnsValidTrue() throws Exception {
        when(reviewService.validateToken("good-token"))
                .thenReturn(new TokenValidationDTO(true, "John Smith", 1L, "Token is valid."));

        mockMvc.perform(get("/api/reviews/token/good-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.professionalName").value("John Smith"))
                .andExpect(jsonPath("$.professionalId").value(1))
                .andExpect(jsonPath("$.message").value("Token is valid."));
    }

    @Test
    @DisplayName("validateToken — token not found → 200 with valid=false")
    void validateToken_notFound_returnsValidFalse() throws Exception {
        when(reviewService.validateToken("bad-token"))
                .thenReturn(new TokenValidationDTO(false, null, null, "Invalid review link."));

        mockMvc.perform(get("/api/reviews/token/bad-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.professionalName").doesNotExist());
    }

    @Test
    @DisplayName("validateToken — already used token → 200 with valid=false and used message")
    void validateToken_alreadyUsed_returnsValidFalse() throws Exception {
        when(reviewService.validateToken("used-token"))
                .thenReturn(new TokenValidationDTO(false, null, null, "This review link has already been used."));

        mockMvc.perform(get("/api/reviews/token/used-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.message").value("This review link has already been used."));
    }

    @Test
    @DisplayName("validateToken — expired token → 200 with valid=false and expired message")
    void validateToken_expired_returnsValidFalse() throws Exception {
        when(reviewService.validateToken("expired-token"))
                .thenReturn(new TokenValidationDTO(false, null, null, "This review link has expired."));

        mockMvc.perform(get("/api/reviews/token/expired-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.message").value("This review link has expired."));
    }

    // =========================================================================
    // POST /api/reviews/token/{token}
    // =========================================================================

    @Test
    @DisplayName("submitReview — valid 5-star review → 200 with saved review")
    void submitReview_valid_returns200() throws Exception {
        ReviewResponseDTO saved = new ReviewResponseDTO(
                1L, "John D.", (short) 5, "Excellent work!", LocalDateTime.now());
        when(reviewService.submitReview(eq("good-token"), any(ReviewRequestDTO.class)))
                .thenReturn(saved);

        mockMvc.perform(post("/api/reviews/token/good-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":5,\"comment\":\"Excellent work!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(5))
                .andExpect(jsonPath("$.customerName").value("John D."))
                .andExpect(jsonPath("$.comment").value("Excellent work!"));
    }

    @Test
    @DisplayName("submitReview — rating below 1 → 400 validation error")
    void submitReview_ratingBelowMin_returns400() throws Exception {
        mockMvc.perform(post("/api/reviews/token/good-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":0,\"comment\":\"Too low\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("submitReview — rating above 5 → 400 validation error")
    void submitReview_ratingAboveMax_returns400() throws Exception {
        mockMvc.perform(post("/api/reviews/token/good-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":6,\"comment\":\"Too high\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("submitReview — missing rating field → 400 validation error")
    void submitReview_missingRating_returns400() throws Exception {
        mockMvc.perform(post("/api/reviews/token/good-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"No rating\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("submitReview — already used token → 500 (IllegalArgumentException → generic handler)")
    void submitReview_alreadyUsed_returnsError() throws Exception {
        when(reviewService.submitReview(eq("used-token"), any(ReviewRequestDTO.class)))
                .thenThrow(new IllegalArgumentException("This review link has already been used."));

        mockMvc.perform(post("/api/reviews/token/used-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":5}"))
                .andExpect(status().is5xxServerError());
    }

    // =========================================================================
    // GET /api/professionals/{id}/reviews
    // =========================================================================

    @Test
    @DisplayName("getReviews — returns list of reviews for a professional")
    void getReviews_returnsReviewList() throws Exception {
        when(reviewService.getReviewsForProfessional(1L)).thenReturn(java.util.List.of(
                new ReviewResponseDTO(1L, "Alice B.", (short) 5, "Fantastic!", LocalDateTime.now()),
                new ReviewResponseDTO(2L, "Bob C.", (short) 4, "Very good", LocalDateTime.now())
        ));

        mockMvc.perform(get("/api/professionals/1/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].customerName").value("Alice B."))
                .andExpect(jsonPath("$[1].rating").value(4));
    }

    @Test
    @DisplayName("getReviews — professional with no reviews → empty list")
    void getReviews_noReviews_returnsEmptyList() throws Exception {
        when(reviewService.getReviewsForProfessional(99L)).thenReturn(java.util.List.of());

        mockMvc.perform(get("/api/professionals/99/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
