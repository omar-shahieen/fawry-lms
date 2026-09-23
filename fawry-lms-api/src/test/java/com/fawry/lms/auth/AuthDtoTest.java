package com.fawry.lms.auth;

import com.fawry.lms.user.Role;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AuthDtoTest {

    @Test
    void requestAndResponseDiagnosticStringsDoNotExposeCredentials() {
        String password = "plain-password-marker";
        String accessToken = "access-token-marker";
        String refreshToken = "refresh-token-marker";
        SignupRequest request = new SignupRequest("Student", "student@example.com", password);
        AuthResponse response = new AuthResponse(accessToken, refreshToken,
                new AuthenticatedUserResponse(UUID.randomUUID(), "Student", "student@example.com", Role.STUDENT,
                        "https://api.dicebear.com/example.svg"));

        assertThat(request.toString()).doesNotContain(password);
        assertThat(response.toString()).doesNotContain(accessToken, refreshToken);
        assertThat(request.toString()).contains("[REDACTED]");
        assertThat(response.toString()).contains("[REDACTED]");
    }
}
