package com.fawry.lms.section.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateSectionRequest(@NotBlank String title, @NotNull Integer orderIndex) {
}
