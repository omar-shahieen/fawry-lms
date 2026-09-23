package com.fawry.lms.auth;

public record RefreshResponse(String accessToken) {
    @Override
    public String toString() {
        return "RefreshResponse[accessToken=[REDACTED]]";
    }
}
