package com.fawry.lms.section.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateContentRequest(@NotBlank String title, @NotBlank String body) {
}
