package com.fawry.lms.section.dtos;

import jakarta.validation.constraints.Pattern;

public record UpdateContentRequest(
        @Pattern(regexp = ".*\\S.*") String title,
        @Pattern(regexp = ".*\\S.*") String body) {
}
