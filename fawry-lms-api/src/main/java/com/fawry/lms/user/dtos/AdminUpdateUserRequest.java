package com.fawry.lms.user.dtos;

import com.fawry.lms.user.entities.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;

public record AdminUpdateUserRequest(
        @Pattern(regexp = ".*\\S.*") String fullName,
        @Email @Pattern(regexp = ".*\\S.*") String email,
        @Pattern(regexp = ".*\\S.*") String password,
        Role role,
        String profilePictureUrl,
        Boolean isActive) {
}
