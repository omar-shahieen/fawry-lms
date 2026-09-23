package com.fawry.lms.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;

import com.fawry.lms.user.entities.Role;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;

/** Creates and verifies signed access and refresh JWTs. */
public final class JwtTokenProvider {

    private static final String USER_ID_CLAIM = "userId";
    private static final String ROLE_CLAIM = "role";
    private static final String TOKEN_TYPE_CLAIM = "tokenType";

    private final SecretKey signingKey;
    private final Duration accessTokenLifetime;
    private final Duration refreshTokenLifetime;

    public JwtTokenProvider(String secret, Duration accessTokenLifetime, Duration refreshTokenLifetime) {
        Objects.requireNonNull(secret, "secret must not be null");
        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalArgumentException("JWT secret must be at least 32 bytes for HMAC-SHA signing");
        }
        this.signingKey = Keys.hmacShaKeyFor(secretBytes);
        this.accessTokenLifetime = Objects.requireNonNull(accessTokenLifetime, "accessTokenLifetime must not be null");
        this.refreshTokenLifetime = Objects.requireNonNull(refreshTokenLifetime,
                "refreshTokenLifetime must not be null");
    }

    public String generateAccessToken(UUID userId, Role role) {
        return generateToken(userId, role, TokenType.ACCESS, accessTokenLifetime);
    }

    public String generateRefreshToken(UUID userId, Role role) {
        return generateToken(userId, role, TokenType.REFRESH, refreshTokenLifetime);
    }

    public TokenClaims parseToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        UUID userId = UUID.fromString(claims.get(USER_ID_CLAIM, String.class));
        Role role = Role.valueOf(claims.get(ROLE_CLAIM, String.class));
        TokenType tokenType = TokenType.valueOf(claims.get(TOKEN_TYPE_CLAIM, String.class));
        return new TokenClaims(userId, role, tokenType, claims.getExpiration().toInstant());
    }

    public boolean isTokenValid(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException exception) {
            return false;
        }
    }

    private String generateToken(UUID userId, Role role, TokenType tokenType, Duration lifetime) {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(role, "role must not be null");
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .claim(USER_ID_CLAIM, userId.toString())
                .claim(ROLE_CLAIM, role.name())
                .claim(TOKEN_TYPE_CLAIM, tokenType.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(lifetime)))
                .signWith(signingKey)
                .compact();
    }

    public enum TokenType {
        ACCESS,
        REFRESH
    }

    public record TokenClaims(UUID userId, Role role, TokenType tokenType, Instant expiresAt) {
    }
}