package com.fawry.lms.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SignupRequest(
        @NotBlank String fullName,
        @NotBlank @Email String email,
        @NotBlank String password
) {
    @Override
    public String toString() {
        return "SignupRequest[fullName=" + fullName + ", email=" + email + ", password=[REDACTED]]";
    }
}
