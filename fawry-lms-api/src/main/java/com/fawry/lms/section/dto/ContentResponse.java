package com.fawry.lms.section.dto;

import java.time.Instant;

public record ContentResponse(
                Long id,
                Long sectionId,
                String title,
                String body,
                Instant createdAt,
                Instant updatedAt) {
}
