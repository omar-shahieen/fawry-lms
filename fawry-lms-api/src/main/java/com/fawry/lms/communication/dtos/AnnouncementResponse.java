package com.fawry.lms.communication.dtos;

import java.time.Instant;
import java.util.UUID;

public record AnnouncementResponse(
        Long id,
        Long courseId,
        UUID authorId,
        String authorName,
        String title,
        String body,
        Instant createdAt,
        Instant updatedAt) {
}
