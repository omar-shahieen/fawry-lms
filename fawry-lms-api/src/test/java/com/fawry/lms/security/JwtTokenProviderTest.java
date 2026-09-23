package com.fawry.lms.security;

import com.fawry.lms.user.Role;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenProviderTest {

    private static final String SECRET = "test-only-secret-with-at-least-32-bytes";

    @Test
    void generatedAccessAndRefreshTokensParseTheirUserAndRole() {
        JwtTokenProvider provider = new JwtTokenProvider(
                SECRET, Duration.ofMinutes(15), Duration.ofDays(7));
        UUID userId = UUID.randomUUID();

        JwtTokenProvider.TokenClaims accessClaims = provider.parseToken(
                provider.generateAccessToken(userId, Role.STUDENT));
        JwtTokenProvider.TokenClaims refreshClaims = provider.parseToken(
                provider.generateRefreshToken(userId, Role.STUDENT));

        assertThat(accessClaims.userId()).isEqualTo(userId);
        assertThat(accessClaims.role()).isEqualTo(Role.STUDENT);
        assertThat(accessClaims.tokenType()).isEqualTo(JwtTokenProvider.TokenType.ACCESS);
        assertThat(refreshClaims.userId()).isEqualTo(userId);
        assertThat(refreshClaims.role()).isEqualTo(Role.STUDENT);
        assertThat(refreshClaims.tokenType()).isEqualTo(JwtTokenProvider.TokenType.REFRESH);
    }

    @Test
    void rejectsExpiredToken() {
        JwtTokenProvider provider = new JwtTokenProvider(
                SECRET, Duration.ofSeconds(-1), Duration.ofDays(7));
        String token = provider.generateAccessToken(UUID.randomUUID(), Role.INSTRUCTOR);

        assertThat(provider.isTokenValid(token)).isFalse();
        assertThatThrownBy(() -> provider.parseToken(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsTamperedToken() {
        JwtTokenProvider provider = new JwtTokenProvider(
                SECRET, Duration.ofMinutes(15), Duration.ofDays(7));
        String token = provider.generateAccessToken(UUID.randomUUID(), Role.ADMIN);
        String[] parts = token.split("\\.");
        String changedPayload = (parts[1].startsWith("a") ? "b" : "a") + parts[1].substring(1);
        String tampered = parts[0] + "." + changedPayload + "." + parts[2];

        assertThat(provider.isTokenValid(tampered)).isFalse();
        assertThatThrownBy(() -> provider.parseToken(tampered)).isInstanceOf(JwtException.class);
    }
}