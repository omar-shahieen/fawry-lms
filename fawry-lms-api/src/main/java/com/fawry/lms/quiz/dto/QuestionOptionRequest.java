package com.fawry.lms.quiz.dto;

import jakarta.validation.constraints.NotBlank;

public record QuestionOptionRequest(@NotBlank String text, boolean isCorrect) {
}
