package com.fawry.lms.auth;

import com.fawry.lms.user.Role;

import java.util.UUID;

public record AuthenticatedUserResponse(
        UUID id,
        String fullName,
        String email,
        Role role,
        String profilePictureUrl
) {
}
