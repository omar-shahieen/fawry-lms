package com.fawry.lms.quiz.dto;

public record QuizAnswerResponse(Long questionId, Long selectedOptionId, boolean isCorrect) {
}
