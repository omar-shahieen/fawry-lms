package com.fawry.lms.user.dto;

import com.fawry.lms.user.entities.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateUserRequest(
                @NotBlank String fullName,
                @NotBlank @Email String email,
                @NotBlank String password,
                @NotNull Role role,
                String profilePictureUrl) {
}
