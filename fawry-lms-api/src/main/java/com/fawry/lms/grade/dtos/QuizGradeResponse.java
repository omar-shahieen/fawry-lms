package com.fawry.lms.grade.dtos;

import java.time.Instant;

public record QuizGradeResponse(
        Long quizId,
        String quizTitle,
        Integer score,
        Integer totalQuestions,
        Instant submittedAt) {
}
