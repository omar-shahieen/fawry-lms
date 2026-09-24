package com.fawry.lms.quiz.dtos;

import java.time.Instant;
import java.util.UUID;

public record QuizAttemptResponse(
        Long id,
        Long quizId,
        UUID studentId,
        String studentName,
        String studentEmail,
        Instant startedAt,
        Instant submittedAt,
        Integer score,
        Integer totalQuestions) {
}
