package com.proconnect.controller;

import com.proconnect.dto.AuthRequestDTO;
import com.proconnect.dto.AuthResponseDTO;
import com.proconnect.entity.Professional;
import com.proconnect.repository.ProfessionalRepository;
import com.proconnect.security.JwtService;
import com.proconnect.service.EmailOtpService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final EmailOtpService emailOtpService;
    private final ProfessionalRepository professionalRepository;
    private final JwtService jwtService;

    @Value("${app.cookie.secure:false}")
    private boolean cookieSecure;

    @Value("${app.jwt.expiry-days:30}")
    private int jwtExpiryDays;

    /**
     * Step 1: Request OTP.
     * Only professionals with a registered email can log in.
     */
    @PostMapping("/request-otp")
    public ResponseEntity<?> requestOtp(@RequestBody AuthRequestDTO body) {
        String email = normalize(body.getEmail());

        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email is required"));
        }

        boolean exists = professionalRepository.existsByEmailIgnoreCase(email);
        if (!exists) {
            // Return 200 with neutral message — don't reveal whether email exists
            log.info("OTP requested for unknown email: {}", email);
            return ResponseEntity.ok(Map.of("message", "If this email is registered, you will receive an OTP"));
        }

        emailOtpService.sendOtp(email);
        log.info("OTP sent to professional email: {}", email);
        return ResponseEntity.ok(Map.of("message", "OTP sent to your email"));
    }

    /**
     * Step 2: Verify OTP → issue JWT in HttpOnly cookie.
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody AuthRequestDTO body,
                                       HttpServletResponse response) {
        String email = normalize(body.getEmail());
        String otp = body.getOtp() != null ? body.getOtp().trim() : null;

        if (email == null || email.isBlank() || otp == null || otp.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email and OTP are required"));
        }

        boolean valid = emailOtpService.verifyOtp(email, otp);
        if (!valid) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid or expired OTP"));
        }

        Professional professional = professionalRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new RuntimeException("Professional not found after OTP verification"));

        String token = jwtService.generateToken(professional.getId());
        addTokenCookie(response, token);

        log.info("Professional id={} logged in via OTP", professional.getId());
        return ResponseEntity.ok(toAuthResponse(professional));
    }

    /**
     * GET /api/auth/me — returns current professional's profile from JWT.
     */
    @GetMapping("/me")
    public ResponseEntity<AuthResponseDTO> me(@AuthenticationPrincipal Long professionalId) {
        Professional professional = professionalRepository.findById(professionalId)
                .orElse(null);
        if (professional == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(toAuthResponse(professional));
    }

    /**
     * POST /api/auth/logout — clears the JWT cookie.
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        Cookie cookie = new Cookie("proconnect_token", "");
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        cookie.setSecure(cookieSecure);
        response.addCookie(cookie);
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private void addTokenCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie("proconnect_token", token);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(jwtExpiryDays * 24 * 60 * 60);
        cookie.setSecure(cookieSecure);
        response.addCookie(cookie);
    }

    private AuthResponseDTO toAuthResponse(Professional p) {
        String displayName = p.getDisplayName() != null ? p.getDisplayName()
                : (p.getFirstName() + " " + p.getLastName()).trim();
        return new AuthResponseDTO(
                p.getId(),
                displayName,
                p.getEmail(),
                p.getSlug(),
                p.getIsAvailable(),
                p.getIsVerified(),
                p.getAvatarUrl(),
                p.getHeadline()
        );
    }

    private String normalize(String email) {
        return email != null ? email.toLowerCase().trim() : null;
    }
}
