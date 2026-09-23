package com.fawry.lms.quiz.dtos;

public record QuizAnswerResponse(Long questionId, Long selectedOptionId, boolean isCorrect) {
}
