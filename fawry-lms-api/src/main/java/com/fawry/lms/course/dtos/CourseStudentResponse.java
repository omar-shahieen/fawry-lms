package com.fawry.lms.course.dtos;

import java.util.UUID;

public record CourseStudentResponse(
        UUID id,
        String fullName,
        String email,
        String profilePictureUrl) {
}
