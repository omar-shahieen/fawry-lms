package com.fawry.lms.quiz.dtos;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateQuizRequest(
        @NotBlank String title,
        @NotNull @Min(1) Integer durationMinutes,
        boolean published) {
}
