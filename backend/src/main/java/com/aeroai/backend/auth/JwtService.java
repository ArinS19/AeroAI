package com.aeroai.backend.auth;

import com.aeroai.backend.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/**
 * Stateless JWT issuance/validation (Section 8, 39). Access-token TTL default
 * 60 minutes (JWT_EXPIRY_MINUTES). No refresh-token flow - on expiry the
 * frontend simply prompts re-login, per spec's accepted simplification.
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final long expiryMillis;

    public JwtService(AppProperties props) {
        String secret = props.getJwt().getSecret();
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("aeroai.jwt.secret must be configured and at least 32 bytes long.");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiryMillis = Duration.ofMinutes(props.getJwt().getExpiryMinutes()).toMillis();
    }

    public String generateToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("email", user.getEmail())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expiryMillis)))
                .signWith(key)
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Long extractUserId(String token) {
        return Long.valueOf(parseClaims(token).getSubject());
    }
}
