package com.fawry.lms.course.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateCourseRequest(
                @NotBlank String title,
                String description,
                @NotBlank String code,
                @NotBlank String term,
                @NotNull UUID instructorId) {
}
