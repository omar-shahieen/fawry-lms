package com.fawry.lms.quiz.dtos;

import jakarta.validation.constraints.NotBlank;

public record QuestionOptionRequest(@NotBlank String text, boolean isCorrect) {
}
