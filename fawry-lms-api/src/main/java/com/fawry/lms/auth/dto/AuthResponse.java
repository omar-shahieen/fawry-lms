package com.fawry.lms.auth.dto;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        AuthenticatedUserResponse user
) {
    @Override
    public String toString() {
        return "AuthResponse[accessToken=[REDACTED], refreshToken=[REDACTED], user=" + user + "]";
    }
}
