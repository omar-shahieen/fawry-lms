package com.fawry.lms.dashboard.dtos;

import java.time.Instant;

public record InstructorAnnouncementResponse(
        Long id,
        Long courseId,
        String title,
        String body,
        Instant createdAt) {
}
