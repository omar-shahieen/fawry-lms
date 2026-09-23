package com.fawry.lms.user.dto;

import java.time.Instant;
import java.util.UUID;

import com.fawry.lms.user.entities.Role;

public record AdminUserResponse(
                UUID id,
                String fullName,
                String email,
                Role role,
                String profilePictureUrl,
                boolean isActive,
                Instant createdAt,
                Instant updatedAt) {
}
