package com.fawry.lms.course.dtos;

import java.time.Instant;
import java.util.UUID;

public record EnrollmentResponse(
        Long id,
        Long courseId,
        UUID studentId,
        Instant enrolledAt) {
}
