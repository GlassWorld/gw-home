package com.gw.api.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtProvider {

    private final SecretKey signingKey;
    private final long accessTokenExpiresInSeconds;
    private final long refreshTokenExpiresInSeconds;

    public JwtProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration-seconds:1800}") long accessTokenExpiresInSeconds,
            @Value("${jwt.refresh-token-expiration-seconds:604800}") long refreshTokenExpiresInSeconds
    ) {
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.accessTokenExpiresInSeconds = accessTokenExpiresInSeconds;
        this.refreshTokenExpiresInSeconds = refreshTokenExpiresInSeconds;
    }

    public String generateAccessToken(String loginId, String role) {
        Instant now = Instant.now();
        Instant expiration = now.plusSeconds(accessTokenExpiresInSeconds);

        return Jwts.builder()
                .subject(loginId)
                .claim("role", role)
                .claim("type", "access")
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(signingKey)
                .compact();
    }

    public String generateRefreshToken(String loginId) {
        Instant now = Instant.now();
        Instant expiration = now.plusSeconds(refreshTokenExpiresInSeconds);

        return Jwts.builder()
                .subject(loginId)
                .claim("type", "refresh")
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(signingKey)
                .compact();
    }

    public String extractLoginId(String token) {
        return extractClaims(token).getSubject();
    }

    public String extractRole(String token) {
        return extractClaims(token).get("role", String.class);
    }

    public String extractTokenType(String token) {
        return extractClaims(token).get("type", String.class);
    }

    public boolean validate(String token) {
        try {
            extractClaims(token);
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    public long getAccessTokenExpiresInSeconds() {
        return accessTokenExpiresInSeconds;
    }

    public Instant getRefreshTokenExpirationInstant() {
        return Instant.now().plus(Duration.ofSeconds(refreshTokenExpiresInSeconds));
    }

    private Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
