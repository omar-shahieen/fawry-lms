package com.fawry.lms.quiz.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CreateQuestionRequest(
        @NotBlank String text,
        @NotNull Integer orderIndex,
        @NotEmpty @Valid List<QuestionOptionRequest> options) {
}
