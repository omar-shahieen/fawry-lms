package com.fawry.lms.quiz.dto;

import java.time.Instant;

public record QuizResponse(
                Long id,
                Long courseId,
                String title,
                Integer durationMinutes,
                boolean published,
                Instant createdAt,
                Instant updatedAt) {
}
