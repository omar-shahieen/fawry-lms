package com.fawry.lms.user.dto;

public record UpdateCurrentUserRequest(
                String fullName,
                String profilePictureUrl) {
}
