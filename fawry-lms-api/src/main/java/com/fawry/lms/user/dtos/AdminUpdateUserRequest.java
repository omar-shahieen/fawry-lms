package com.fawry.lms.user.dtos;

import com.fawry.lms.user.entities.Role;

public record AdminUpdateUserRequest(
        String fullName,
        String email,
        String password,
        Role role,
        String profilePictureUrl,
        Boolean isActive) {
}
