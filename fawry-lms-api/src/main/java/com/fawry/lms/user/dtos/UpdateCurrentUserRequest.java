package com.fawry.lms.user.dtos;

import jakarta.validation.constraints.Pattern;

public record UpdateCurrentUserRequest(
        @Pattern(regexp = ".*\\S.*") String fullName,
        String profilePictureUrl) {
}
