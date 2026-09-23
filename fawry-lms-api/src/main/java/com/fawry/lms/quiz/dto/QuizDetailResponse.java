package com.fawry.lms.quiz.dto;

import java.time.Instant;
import java.util.List;

public record QuizDetailResponse(
                Long id,
                Long courseId,
                String title,
                Integer durationMinutes,
                boolean published,
                Instant startedAt,
                Instant expiresAt,
                Instant submittedAt,
                Integer score,
                Integer totalQuestions,
                List<QuizStudentQuestionResponse> questions,
                List<QuizAnswerResponse> answers) {
}
