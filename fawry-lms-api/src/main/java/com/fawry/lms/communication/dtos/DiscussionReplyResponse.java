package com.fawry.lms.communication.dtos;

import java.time.Instant;
import java.util.UUID;

public record DiscussionReplyResponse(
        Long id,
        UUID authorId,
        String authorName,
        String body,
        Instant createdAt,
        Instant updatedAt) {
}
