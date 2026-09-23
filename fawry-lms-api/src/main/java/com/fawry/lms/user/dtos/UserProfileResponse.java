package com.fawry.lms.user.dtos;

import java.util.List;
import java.util.UUID;

import com.fawry.lms.user.entities.Role;

public record UserProfileResponse(
        UUID id,
        String fullName,
        String email,
        Role role,
        String profilePictureUrl,
        List<EnrolledCourseResponse> enrolledCourses) {
}
