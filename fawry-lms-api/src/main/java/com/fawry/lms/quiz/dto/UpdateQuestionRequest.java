package com.fawry.lms.quiz.dto;

import jakarta.validation.Valid;
import java.util.List;

public record UpdateQuestionRequest(
                String text,
                Integer orderIndex,
                @Valid List<QuestionOptionRequest> options) {
}
