package com.fawry.lms.quiz.dtos;

import java.time.Instant;
import java.util.List;

public record SubmitQuizResponse(
        Long attemptId,
        Integer score,
        Integer totalQuestions,
        Instant submittedAt,
        List<QuizAnswerResponse> answers) {
}
