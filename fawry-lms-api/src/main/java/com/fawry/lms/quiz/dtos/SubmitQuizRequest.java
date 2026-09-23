package com.fawry.lms.quiz.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record SubmitQuizRequest(@NotNull @Valid List<SubmitAnswerRequest> answers) {
}
