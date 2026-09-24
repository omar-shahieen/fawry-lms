package com.fawry.lms.quiz.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record SubmitAnswerRequest(
        @NotNull @Positive Long questionId,
        @Positive Long selectedOptionId) {
}
