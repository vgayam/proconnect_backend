package com.proconnect.service;

import com.proconnect.dto.ProfessionalContactDTO;
import com.proconnect.entity.Professional;
import com.proconnect.exception.RateLimitException;
import com.proconnect.exception.ResourceNotFoundException;
import com.proconnect.repository.BookingInquiryRepository;
import com.proconnect.repository.ContactMessageRepository;
import com.proconnect.repository.ContactViewRepository;
import com.proconnect.repository.ProfessionalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ContactService — unit tests")
class ContactServiceTest {

    @Mock ProfessionalRepository    professionalRepository;
    @Mock ContactViewRepository     contactViewRepository;
    @Mock ContactMessageRepository  contactMessageRepository;
    @Mock BookingInquiryRepository  bookingInquiryRepository;
    @Mock EmailOtpService           emailOtpService;

    @InjectMocks ContactService contactService;

    private Professional professional;

    @BeforeEach
    void setUp() {
        // Inject @Value field that Mockito/InjectMocks cannot set
        ReflectionTestUtils.setField(contactService, "frontendUrl", "https://proconnect-frontend-rouge.vercel.app");

        professional = new Professional();
        professional.setId(1L);
        professional.setFirstName("John");
        professional.setLastName("Smith");
        professional.setEmail("pro@example.com");
        professional.setPhone("+91 98765 43210");
        professional.setWhatsapp("+91 98765 43210");
        professional.setHeadline("Expert Plumber");
        professional.setCity("Hyderabad");
        professional.setState("Telangana");
        professional.setCountry("India");
    }

    // =========================================================================
    // requestContactOtp
    // =========================================================================

    @Test
    @DisplayName("requestContactOtp — professional exists → OTP sent via email service")
    void requestContactOtp_professionalExists_sendsOtp() {
        when(professionalRepository.existsById(1L)).thenReturn(true);
        when(contactViewRepository.countByEmailSince(anyString(), any(Instant.class))).thenReturn(0L);
        when(contactViewRepository.countByIpSince(anyString(), any(Instant.class))).thenReturn(0L);

        assertThatNoException().isThrownBy(() ->
                contactService.requestContactOtp(1L, "viewer@example.com", "10.0.0.1"));

        verify(emailOtpService).sendOtp("viewer@example.com");
    }

    @Test
    @DisplayName("requestContactOtp — unknown professional → throws ResourceNotFoundException before OTP")
    void requestContactOtp_unknownProfessional_throwsResourceNotFoundException() {
        when(professionalRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() ->
                contactService.requestContactOtp(999L, "viewer@example.com", "10.0.0.1"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");

        verify(emailOtpService, never()).sendOtp(anyString());
    }

    @Test
    @DisplayName("requestContactOtp — email exceeds rate limit → throws RateLimitException")
    void requestContactOtp_emailRateLimitExceeded_throwsRateLimitException() {
        when(professionalRepository.existsById(1L)).thenReturn(true);
        // 2 is the MAX_VIEWS_PER_24H constant — email check runs first
        when(contactViewRepository.countByEmailSince(eq("viewer@example.com"), any(Instant.class)))
                .thenReturn(2L);
        // IP check is never reached because email check throws first — no stub needed

        assertThatThrownBy(() ->
                contactService.requestContactOtp(1L, "viewer@example.com", "10.0.0.1"))
                .isInstanceOf(RateLimitException.class)
                .hasMessageContaining("2");

        verify(emailOtpService, never()).sendOtp(anyString());
    }

    @Test
    @DisplayName("requestContactOtp — IP exceeds rate limit → throws RateLimitException")
    void requestContactOtp_ipRateLimitExceeded_throwsRateLimitException() {
        when(professionalRepository.existsById(1L)).thenReturn(true);
        when(contactViewRepository.countByEmailSince(anyString(), any(Instant.class))).thenReturn(0L);
        when(contactViewRepository.countByIpSince(eq("10.0.0.1"), any(Instant.class))).thenReturn(2L);

        assertThatThrownBy(() ->
                contactService.requestContactOtp(1L, "viewer@example.com", "10.0.0.1"))
                .isInstanceOf(RateLimitException.class)
                .hasMessageContaining("device");

        verify(emailOtpService, never()).sendOtp(anyString());
    }

    // =========================================================================
    // verifyContactOtp
    // =========================================================================

    @Test
    @DisplayName("verifyContactOtp — correct OTP → returns contact details and issues review token")
    void verifyContactOtp_correctOtp_returnsContactDetails() {
        when(contactViewRepository.countByEmailSince(anyString(), any(Instant.class))).thenReturn(0L);
        when(contactViewRepository.countByIpSince(anyString(), any(Instant.class))).thenReturn(0L);
        when(emailOtpService.verifyOtp("viewer@example.com", "123456")).thenReturn(true);
        when(professionalRepository.findById(1L)).thenReturn(Optional.of(professional));

        ProfessionalContactDTO result = contactService.verifyContactOtp(
                1L, "viewer@example.com", "123456", "10.0.0.1");

        assertThat(result.getEmail()).isEqualTo("pro@example.com");
        assertThat(result.getPhone()).isEqualTo("+91 98765 43210");
        assertThat(result.getWhatsapp()).isEqualTo("+91 98765 43210");

        // Review token and contact view must be persisted
        verify(contactViewRepository).save(any(com.proconnect.entity.ContactView.class));
        verify(bookingInquiryRepository).save(any(com.proconnect.entity.BookingInquiry.class));
    }

    @Test
    @DisplayName("verifyContactOtp — correct OTP → BookingInquiry has anonymized name (not raw email)")
    void verifyContactOtp_correctOtp_storesAnonymizedName() {
        when(contactViewRepository.countByEmailSince(anyString(), any(Instant.class))).thenReturn(0L);
        when(contactViewRepository.countByIpSince(anyString(), any(Instant.class))).thenReturn(0L);
        when(emailOtpService.verifyOtp("john.doe@example.com", "123456")).thenReturn(true);
        when(professionalRepository.findById(1L)).thenReturn(Optional.of(professional));

        contactService.verifyContactOtp(1L, "john.doe@example.com", "123456", "10.0.0.1");

        var captor = ArgumentCaptor.forClass(com.proconnect.entity.BookingInquiry.class);
        verify(bookingInquiryRepository).save(captor.capture());

        String storedName = captor.getValue().getCustomerName();
        // "john.doe@example.com" → "John D." — must NOT be raw email
        assertThat(storedName).doesNotContain("@");
        assertThat(storedName).isEqualTo("John D.");
    }

    @Test
    @DisplayName("verifyContactOtp — review token in BookingInquiry expires in ~30 days")
    void verifyContactOtp_correctOtp_tokenExpiresIn30Days() {
        when(contactViewRepository.countByEmailSince(anyString(), any(Instant.class))).thenReturn(0L);
        when(contactViewRepository.countByIpSince(anyString(), any(Instant.class))).thenReturn(0L);
        when(emailOtpService.verifyOtp(anyString(), anyString())).thenReturn(true);
        when(professionalRepository.findById(1L)).thenReturn(Optional.of(professional));

        contactService.verifyContactOtp(1L, "viewer@example.com", "123456", "10.0.0.1");

        var captor = ArgumentCaptor.forClass(com.proconnect.entity.BookingInquiry.class);
        verify(bookingInquiryRepository).save(captor.capture());

        var expiresAt = captor.getValue().getTokenExpiresAt();
        assertThat(expiresAt).isAfter(java.time.LocalDateTime.now().plusDays(29));
        assertThat(expiresAt).isBefore(java.time.LocalDateTime.now().plusDays(31));
    }

    @Test
    @DisplayName("verifyContactOtp — wrong OTP → throws IllegalArgumentException, no view saved")
    void verifyContactOtp_wrongOtp_throwsException() {
        when(contactViewRepository.countByEmailSince(anyString(), any(Instant.class))).thenReturn(0L);
        when(contactViewRepository.countByIpSince(anyString(), any(Instant.class))).thenReturn(0L);
        when(emailOtpService.verifyOtp("viewer@example.com", "000000")).thenReturn(false);

        assertThatThrownBy(() ->
                contactService.verifyContactOtp(1L, "viewer@example.com", "000000", "10.0.0.1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid or expired");

        verify(contactViewRepository, never()).save(any(com.proconnect.entity.ContactView.class));
        verify(bookingInquiryRepository, never()).save(any(com.proconnect.entity.BookingInquiry.class));
    }

    @Test
    @DisplayName("verifyContactOtp — unknown professional after OTP passes → throws ResourceNotFoundException")
    void verifyContactOtp_unknownProfessionalAfterOtp_throwsResourceNotFoundException() {
        when(contactViewRepository.countByEmailSince(anyString(), any(Instant.class))).thenReturn(0L);
        when(contactViewRepository.countByIpSince(anyString(), any(Instant.class))).thenReturn(0L);
        when(emailOtpService.verifyOtp(anyString(), anyString())).thenReturn(true);
        when(professionalRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                contactService.verifyContactOtp(999L, "viewer@example.com", "123456", "10.0.0.1"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }
}
