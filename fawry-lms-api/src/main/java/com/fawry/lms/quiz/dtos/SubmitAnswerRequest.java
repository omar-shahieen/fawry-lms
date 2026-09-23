package com.fawry.lms.quiz.dtos;

import jakarta.validation.constraints.NotNull;

public record SubmitAnswerRequest(@NotNull Long questionId, Long selectedOptionId) {
}
