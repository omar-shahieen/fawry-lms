package com.fawry.lms.user.dtos;

public record UpdateCurrentUserRequest(
        String fullName,
        String profilePictureUrl) {
}
