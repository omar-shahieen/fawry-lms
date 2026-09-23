package com.fawry.lms.course.dto;

import java.time.Instant;
import java.util.UUID;

public record CourseResponse(
                Long id,
                String title,
                String description,
                String code,
                String term,
                UUID instructorId,
                String instructorName,
                boolean isActive,
                Instant createdAt,
                Instant updatedAt) {
}
