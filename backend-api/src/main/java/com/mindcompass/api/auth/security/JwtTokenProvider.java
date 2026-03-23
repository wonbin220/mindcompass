package com.mindcompass.api.auth.security;

import com.mindcompass.api.auth.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

@Component
// JWT 생성, 검증, 사용자 정보 추출을 담당하는 보안 유틸 클래스입니다.
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;
    private final SecretKey secretKey;

    public JwtTokenProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.secretKey = createSecretKey(jwtProperties.getSecret());
    }

    public GeneratedToken generateAccessToken(User user) {
        return generateToken(user, jwtProperties.getAccessTokenMinutes() * 60);
    }

    public GeneratedToken generateRefreshToken(User user) {
        return generateToken(user, jwtProperties.getRefreshTokenDays() * 24 * 60 * 60);
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    public Long extractUserId(String token) {
        return Long.parseLong(extractClaims(token).getSubject());
    }

    public String extractEmail(String token) {
        return extractClaims(token).get("email", String.class);
    }

    private GeneratedToken generateToken(User user, long validSeconds) {
        LocalDateTime issuedAt = LocalDateTime.now();
        LocalDateTime expiresAt = issuedAt.plusSeconds(validSeconds);

        String token = Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("email", user.getEmail())
                .issuedAt(toDate(issuedAt))
                .expiration(toDate(expiresAt))
                .signWith(secretKey)
                .compact();

        return new GeneratedToken(token, expiresAt);
    }

    private Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey createSecretKey(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT_SECRET environment value is required.");
        }

        byte[] rawBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (rawBytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET must be at least 32 characters long.");
        }
        return Keys.hmacShaKeyFor(rawBytes);
    }

    private Date toDate(LocalDateTime dateTime) {
        return Date.from(dateTime.toInstant(ZoneOffset.UTC));
    }

    public record GeneratedToken(
            String token,
            LocalDateTime expiresAt
    ) {
    }
}
