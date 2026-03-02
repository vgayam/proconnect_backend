package com.proconnect.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proconnect.dto.ContactOtpRequestDTO;
import com.proconnect.dto.ContactOtpVerifyDTO;
import com.proconnect.dto.ProfessionalContactDTO;
import com.proconnect.exception.RateLimitException;
import com.proconnect.exception.ResourceNotFoundException;
import com.proconnect.service.ContactService;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
    value = ContactController.class,
    excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class}
)
@DisplayName("ContactController — HTTP layer tests")
class ContactControllerTest {

    @Autowired WebApplicationContext context;
    @Autowired ObjectMapper objectMapper;
    @MockBean  ContactService contactService;
    @MockBean  com.proconnect.security.JwtAuthFilter jwtAuthFilter;
    @MockBean  com.proconnect.security.JwtService    jwtService;

    private MockMvc mockMvc;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        // Build WITHOUT filters so the JwtAuthFilter mock doesn't swallow requests
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    // =========================================================================
    // POST /api/contact/professionals/{id}/request-otp
    // =========================================================================

    @Test
    @DisplayName("requestOtp — valid email → 200 with confirmation message")
    void requestOtp_valid_returns200WithMessage() throws Exception {
        doNothing().when(contactService)
                .requestContactOtp(eq(1L), eq("viewer@example.com"), anyString());

        mockMvc.perform(post("/api/contact/professionals/1/request-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(email("viewer@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Verification code sent to viewer@example.com"));
    }

    @Test
    @DisplayName("requestOtp — unknown professional → 404")
    void requestOtp_unknownProfessional_returns404() throws Exception {
        doThrow(ResourceNotFoundException.professionalNotFound(999L))
                .when(contactService).requestContactOtp(eq(999L), anyString(), anyString());

        mockMvc.perform(post("/api/contact/professionals/999/request-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(email("viewer@example.com"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("999")));
    }

    @Test
    @DisplayName("requestOtp — rate limit exceeded → 429")
    void requestOtp_rateLimitExceeded_returns429() throws Exception {
        doThrow(new RateLimitException("You have reached the limit of 2 contact views per 24 hours"))
                .when(contactService).requestContactOtp(anyLong(), anyString(), anyString());

        mockMvc.perform(post("/api/contact/professionals/1/request-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(email("viewer@example.com"))))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    @DisplayName("requestOtp — blank email → 400 validation error")
    void requestOtp_blankEmail_returns400() throws Exception {
        mockMvc.perform(post("/api/contact/professionals/1/request-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    @DisplayName("requestOtp — malformed email → 400 validation error")
    void requestOtp_malformedEmail_returns400() throws Exception {
        mockMvc.perform(post("/api/contact/professionals/1/request-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("requestOtp — missing email field → 400 validation error")
    void requestOtp_missingEmailField_returns400() throws Exception {
        mockMvc.perform(post("/api/contact/professionals/1/request-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // =========================================================================
    // POST /api/contact/professionals/{id}/verify-otp
    // =========================================================================

    @Test
    @DisplayName("verifyOtp — correct OTP → 200 with contact details")
    void verifyOtp_correctOtp_returns200WithContactDetails() throws Exception {
        ProfessionalContactDTO contact = new ProfessionalContactDTO(
                "pro@example.com", "+91 98765 43210", "+91 98765 43210");

        when(contactService.verifyContactOtp(eq(1L), eq("viewer@example.com"), eq("123456"), anyString()))
                .thenReturn(contact);

        mockMvc.perform(post("/api/contact/professionals/1/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(otp("viewer@example.com", "123456"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("pro@example.com"))
                .andExpect(jsonPath("$.phone").value("+91 98765 43210"))
                .andExpect(jsonPath("$.whatsapp").value("+91 98765 43210"));
    }

    @Test
    @DisplayName("verifyOtp — wrong OTP → 500 (IllegalArgumentException → generic handler)")
    void verifyOtp_wrongOtp_returnsError() throws Exception {
        when(contactService.verifyContactOtp(anyLong(), anyString(), eq("000000"), anyString()))
                .thenThrow(new IllegalArgumentException("Invalid or expired verification code"));

        mockMvc.perform(post("/api/contact/professionals/1/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(otp("viewer@example.com", "000000"))))
                .andExpect(status().is5xxServerError());
    }

    @Test
    @DisplayName("verifyOtp — rate limit exceeded → 429")
    void verifyOtp_rateLimitExceeded_returns429() throws Exception {
        when(contactService.verifyContactOtp(anyLong(), anyString(), anyString(), anyString()))
                .thenThrow(new RateLimitException("Rate limit exceeded"));

        mockMvc.perform(post("/api/contact/professionals/1/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(otp("viewer@example.com", "123456"))))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    @DisplayName("verifyOtp — blank OTP → 400 validation error")
    void verifyOtp_blankOtp_returns400() throws Exception {
        mockMvc.perform(post("/api/contact/professionals/1/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"viewer@example.com\",\"otp\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("verifyOtp — missing otp field → 400 validation error")
    void verifyOtp_missingOtp_returns400() throws Exception {
        mockMvc.perform(post("/api/contact/professionals/1/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"viewer@example.com\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("verifyOtp — non-numeric professional id → 400")
    void verifyOtp_nonNumericId_returns400() throws Exception {
        mockMvc.perform(post("/api/contact/professionals/abc/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(otp("viewer@example.com", "123456"))))
                .andExpect(status().isBadRequest());
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private ContactOtpRequestDTO email(String email) {
        ContactOtpRequestDTO d = new ContactOtpRequestDTO();
        d.setEmail(email);
        return d;
    }

    private ContactOtpVerifyDTO otp(String email, String otp) {
        ContactOtpVerifyDTO d = new ContactOtpVerifyDTO();
        d.setEmail(email);
        d.setOtp(otp);
        return d;
    }

    private String json(Object o) throws Exception {
        return objectMapper.writeValueAsString(o);
    }
}
