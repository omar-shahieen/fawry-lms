package com.fawry.lms.quiz.dtos;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

public record UpdateQuizRequest(
        @Pattern(regexp = ".*\\S.*") String title,
        @Min(1) Integer durationMinutes,
        Boolean published) {
}
