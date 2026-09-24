package com.fawry.lms.quiz.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import java.util.List;

public record UpdateQuestionRequest(
        @Pattern(regexp = ".*\\S.*") String text,
        Integer orderIndex,
        @Valid List<QuestionOptionRequest> options) {
}
