package com.fawry.lms.auth.dtos;

import java.util.UUID;

import com.fawry.lms.user.entities.Role;

public record AuthenticatedUserResponse(
        UUID id,
        String fullName,
        String email,
        Role role,
        String profilePictureUrl) {
}
