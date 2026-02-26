package com.proconnect.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Slf4j
@Service
public class JwtService {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiry-days:30}")
    private int expiryDays;

    private SecretKey signingKey;

    @PostConstruct
    public void init() {
        if (jwtSecret == null || jwtSecret.length() < 32) {
            throw new IllegalStateException(
                "app.jwt.secret must be at least 32 characters long. " +
                "Set the JWT_SECRET environment variable in production.");
        }
        signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    /** Generate a JWT with the professional's numeric ID as subject. */
    public String generateToken(Long professionalId) {
        long nowMs = System.currentTimeMillis();
        long expiryMs = (long) expiryDays * 24 * 60 * 60 * 1000;

        return Jwts.builder()
                .subject(String.valueOf(professionalId))
                .issuedAt(new Date(nowMs))
                .expiration(new Date(nowMs + expiryMs))
                .signWith(signingKey)
                .compact();
    }

    /** Returns the professionalId from a valid token, or null if invalid/expired. */
    public Long extractProfessionalId(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Long.parseLong(claims.getSubject());
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Invalid JWT token: {}", e.getMessage());
            return null;
        }
    }

    public boolean isValid(String token) {
        return extractProfessionalId(token) != null;
    }
}
